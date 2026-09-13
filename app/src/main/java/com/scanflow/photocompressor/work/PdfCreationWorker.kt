package com.scanflow.photocompressor.work

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * WorkManager worker for background PDF document creation from image URIs.
 *
 * Guarantees:
 * - Safe memory usage: images are loaded with safe downsampling and recycled per page.
 * - Progress reporting: reports page-by-page progress.
 * - Cooperative cancellation: checks isStopped per page.
 * - Temp file cleanup and resource closure.
 * - Output validation: verifies file exists and length > 0.
 */
class PdfCreationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "PdfCreationWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkManagerEntryPoint::class.java
        )
        val imageRepository = entryPoint.imageRepository()
        val fileManager = entryPoint.fileManager()

        val sourceUriStrings = inputData.getStringArray(WorkKeys.KEY_SOURCE_URIS) ?: emptyArray()
        val title = inputData.getString(WorkKeys.KEY_PDF_TITLE)?.ifBlank { "document" } ?: "document"
        val totalPages = sourceUriStrings.size

        if (totalPages == 0) {
            return@withContext Result.failure(
                workDataOf(WorkKeys.KEY_ERROR_MESSAGE to "No images provided for PDF creation")
            )
        }

        val pdfDocument = PdfDocument()
        var pagesAdded = 0
        var tempPdfFile: File? = null

        try {
            for (index in sourceUriStrings.indices) {
                if (isStopped) {
                    Log.i(TAG, "PDF creation worker cancelled at page $index of $totalPages")
                    pdfDocument.close()
                    return@withContext Result.failure(
                        workDataOf(WorkKeys.KEY_ERROR_MESSAGE to "PDF creation cancelled by user")
                    )
                }

                val uri = Uri.parse(sourceUriStrings[index])
                val progress = ((index.toFloat() / totalPages.toFloat()) * 90).toInt()
                setProgress(
                    workDataOf(
                        WorkKeys.KEY_PROGRESS_PERCENT to progress,
                        WorkKeys.KEY_CURRENT_URI to sourceUriStrings[index],
                        WorkKeys.KEY_PDF_PAGE_COUNT to pagesAdded,
                        WorkKeys.KEY_TOTAL_COUNT to totalPages
                    )
                )

                // Load downsampled bitmap (max 1920) to prevent OOM
                val bitmap = try {
                    imageRepository.loadBitmap(uri, maxWidth = 1920, maxHeight = 1920)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load bitmap for PDF page $index: ${e.message}")
                    null
                }

                if (bitmap != null) {
                    try {
                        val pageInfo = PdfDocument.PageInfo.Builder(
                            bitmap.width,
                            bitmap.height,
                            pagesAdded + 1
                        ).create()
                        val page = pdfDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(page)
                        pagesAdded++
                    } finally {
                        bitmap.recycle() // Immediate memory release per page
                    }
                }
            }

            if (pagesAdded == 0) {
                pdfDocument.close()
                return@withContext Result.failure(
                    workDataOf(WorkKeys.KEY_ERROR_MESSAGE to "Failed to render any pages into PDF")
                )
            }

            // Write to temp file
            tempPdfFile = fileManager.createTempFile("pdf_doc_", "pdf")
            FileOutputStream(tempPdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            // Validate output
            if (!tempPdfFile.exists() || tempPdfFile.length() <= 0L) {
                return@withContext Result.failure(
                    workDataOf(WorkKeys.KEY_ERROR_MESSAGE to "PDF output file is empty or missing")
                )
            }

            // Promote to final destination
            val sanitizedTitle = title.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val finalFile = File(
                fileManager.getOutputDirectory(),
                "${sanitizedTitle}_${System.currentTimeMillis()}.pdf"
            )
            tempPdfFile.copyTo(finalFile, overwrite = true)

            val outputUri = try {
                androidx.core.content.FileProvider.getUriForFile(
                    applicationContext,
                    "${applicationContext.packageName}.fileprovider",
                    finalFile
                )
            } catch (e: Exception) {
                Log.w(TAG, "FileProvider failed, falling back to Uri.fromFile: ${e.message}")
                Uri.fromFile(finalFile)
            }

            setProgress(
                workDataOf(
                    WorkKeys.KEY_PROGRESS_PERCENT to 100,
                    WorkKeys.KEY_PDF_PAGE_COUNT to pagesAdded,
                    WorkKeys.KEY_TOTAL_COUNT to totalPages
                )
            )

            return@withContext Result.success(
                workDataOf(
                    WorkKeys.KEY_PDF_OUTPUT_URI to outputUri.toString(),
                    WorkKeys.KEY_PDF_PAGE_COUNT to pagesAdded,
                    WorkKeys.KEY_TOTAL_COUNT to totalPages
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create PDF document: ${e.message}", e)
            try {
                pdfDocument.close()
            } catch (closeEx: Exception) {
                Log.w(TAG, "Error closing PdfDocument: ${closeEx.message}")
            }
            return@withContext Result.failure(
                workDataOf(WorkKeys.KEY_ERROR_MESSAGE to (e.message ?: "PDF generation error"))
            )
        } finally {
            try {
                tempPdfFile?.delete()
                fileManager.cleanTempFiles()
            } catch (cleanEx: Exception) {
                Log.w(TAG, "Error cleaning temp PDF files: ${cleanEx.message}")
            }
        }
    }
}

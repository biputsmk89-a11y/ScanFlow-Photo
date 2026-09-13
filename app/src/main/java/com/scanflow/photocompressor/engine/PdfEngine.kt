package com.scanflow.photocompressor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine responsible for assembling multiple photos into a standardized PDF document.
 * Follows the pipeline:
 * Select images -> Reorder -> Page size -> Margins -> Orientation -> Quality -> Generate
 *
 * Employs ImagePipelineEngine to normalize, scale, and compress each image.
 */
@Singleton
class PdfEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imagePipelineEngine: ImagePipelineEngine,
    private val fileManager: FileManager,
    private val bitmapUtils: BitmapUtils
) {

    suspend fun generatePdf(
        orderedUris: List<Uri>,
        config: PdfConfig
    ): Result<File> = withContext(Dispatchers.IO) {
        if (orderedUris.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No images provided for PDF generation"))
        }

        val pdfDocument = PdfDocument()
        var tempFile: File? = null

        try {
            var pageNumber = 1

            for (uri in orderedUris) {
                val marginPt = config.margin.marginPt.toFloat()

                // 1. Calculate Page Dimensions & Printable Area based on Presets
                // A4, A5, Letter, Original, Custom
                val (baseWidthPt, baseHeightPt) = when (config.pageSize) {
                    PdfPageSize.A4 -> Pair(595, 842)
                    PdfPageSize.A5 -> Pair(420, 595)
                    PdfPageSize.LETTER -> Pair(612, 792)
                    PdfPageSize.CUSTOM -> Pair(config.customWidthPt.coerceAtLeast(100), config.customHeightPt.coerceAtLeast(100))
                    PdfPageSize.ORIGINAL -> Pair(0, 0) // Computed dynamically from image
                }

                // Never insert full-resolution bitmap into PDF if scaling is sufficient.
                // Scale target dimension proportionally to printable area at 2x PDF resolution (144 DPI)
                val maxTargetDim = if (config.pageSize == PdfPageSize.ORIGINAL) {
                    config.quality.maxDimension
                } else {
                    val maxPrintablePt = maxOf(baseWidthPt, baseHeightPt) - (marginPt * 2)
                    (maxPrintablePt * 2f).toInt().coerceIn(600, config.quality.maxDimension)
                }

                // 2. Process image through ImagePipeline with downsampled scale
                val pipeline = ImagePipeline(
                    operations = listOf(
                        ImageOperation.Resize(
                            maxDimension = maxTargetDim,
                            maintainAspectRatio = true
                        ),
                        ImageOperation.Compress(
                            quality = config.quality.compressionQuality
                        )
                    )
                )

                // Execute pipeline or load scaled bitmap
                val processedResult = imagePipelineEngine.execute(
                    inputUri = uri,
                    pipeline = pipeline,
                    operationType = OperationType.CONVERT
                )

                val bitmap: Bitmap = if (processedResult.isSuccess) {
                    val resultUri = processedResult.getOrNull()?.outputUri
                    if (resultUri != null) {
                        bitmapUtils.decodeBitmap(resultUri, maxTargetDim, maxTargetDim)
                    } else {
                        bitmapUtils.decodeBitmap(uri, maxTargetDim, maxTargetDim)
                    }
                } else {
                    bitmapUtils.decodeBitmap(uri, maxTargetDim, maxTargetDim)
                }

                // 3. Determine Final Page Dimensions & Orientation (Portrait, Landscape, Auto)
                val isImageLandscape = bitmap.width > bitmap.height

                val isLandscapePage = when (config.orientation) {
                    PdfOrientation.LANDSCAPE -> true
                    PdfOrientation.PORTRAIT -> false
                    PdfOrientation.AUTO -> isImageLandscape
                }

                val (pageWidth, pageHeight) = when (config.pageSize) {
                    PdfPageSize.ORIGINAL -> {
                        Pair(bitmap.width + (marginPt * 2).toInt(), bitmap.height + (marginPt * 2).toInt())
                    }
                    PdfPageSize.A4, PdfPageSize.A5, PdfPageSize.LETTER, PdfPageSize.CUSTOM -> {
                        if (isLandscapePage) {
                            Pair(maxOf(baseWidthPt, baseHeightPt), minOf(baseWidthPt, baseHeightPt))
                        } else {
                            Pair(minOf(baseWidthPt, baseHeightPt), maxOf(baseWidthPt, baseHeightPt))
                        }
                    }
                }

                // 3. Render Page
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Fill background white
                canvas.drawColor(Color.WHITE)

                // Calculate printable area
                val printableWidth = (pageWidth - (marginPt * 2)).coerceAtLeast(1f)
                val printableHeight = (pageHeight - (marginPt * 2)).coerceAtLeast(1f)

                val scale = minOf(
                    printableWidth / bitmap.width.toFloat(),
                    printableHeight / bitmap.height.toFloat()
                )

                val drawWidth = bitmap.width * scale
                val drawHeight = bitmap.height * scale
                val drawLeft = marginPt + (printableWidth - drawWidth) / 2f
                val drawTop = marginPt + (printableHeight - drawHeight) / 2f

                val destRect = RectF(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

                canvas.drawBitmap(bitmap, null, destRect, paint)
                pdfDocument.finishPage(page)

                bitmap.recycle()
                pageNumber++
            }

            // 4. Save to temporary PDF file and validate
            val sanitizedTitle = config.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "document" }
            tempFile = fileManager.createTempFile("${sanitizedTitle}_", "pdf")
            FileOutputStream(tempFile).use { outStream ->
                pdfDocument.writeTo(outStream)
            }
            pdfDocument.close()

            if (!tempFile.exists() || tempFile.length() <= 0L) {
                return@withContext Result.failure(IllegalStateException("Generated PDF file is empty"))
            }

            Result.success(tempFile)
        } catch (e: Exception) {
            pdfDocument.close()
            tempFile?.delete()
            Result.failure(e)
        }
    }
}

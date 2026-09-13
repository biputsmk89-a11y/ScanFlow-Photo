package com.scanflow.photocompressor.work

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ImageOperation
import com.scanflow.photocompressor.domain.model.ImagePipeline
import com.scanflow.photocompressor.domain.model.ProcessingHistory
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for long-running single image processing tasks
 * (e.g. adaptive target size search on large images, heavy filter/transform pipelines).
 */
class ImageProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ImageProcessingWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkManagerEntryPoint::class.java
        )
        val imageEngine = entryPoint.imageEngine()
        val fileManager = entryPoint.fileManager()
        val historyRepository = entryPoint.historyRepository()

        val sourceUriStr = inputData.getString(WorkKeys.KEY_SOURCE_URI)
            ?: return@withContext Result.failure(
                workDataOf(WorkKeys.KEY_ERROR_MESSAGE to "Missing source URI")
            )
        val sourceUri = Uri.parse(sourceUriStr)

        val quality = inputData.getInt(WorkKeys.KEY_QUALITY, 80)
        val maxWidth = inputData.getInt(WorkKeys.KEY_MAX_WIDTH, 0)
        val maxHeight = inputData.getInt(WorkKeys.KEY_MAX_HEIGHT, 0)
        val formatStr = inputData.getString(WorkKeys.KEY_FORMAT) ?: ImageFormat.JPEG.name
        val format = try {
            ImageFormat.valueOf(formatStr)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Unknown format '$formatStr', defaulting to JPEG: ${e.message}")
            ImageFormat.JPEG
        }
        val removeMetadata = inputData.getBoolean(WorkKeys.KEY_REMOVE_METADATA, false)

        val ops = mutableListOf<ImageOperation>()
        if (maxWidth > 0 || maxHeight > 0) {
            ops.add(
                ImageOperation.Resize(
                    width = if (maxWidth > 0) maxWidth else null,
                    height = if (maxHeight > 0) maxHeight else null,
                    maintainAspectRatio = true
                )
            )
        }
        ops.add(ImageOperation.Compress(quality = quality))
        ops.add(ImageOperation.Convert(format = format))
        if (removeMetadata) {
            ops.add(ImageOperation.RemoveMetadata(removeExif = true))
        }
        val pipeline = ImagePipeline(ops)

        try {
            if (isStopped) {
                return@withContext Result.failure(
                    workDataOf(WorkKeys.KEY_ERROR_MESSAGE to "Operation cancelled before start")
                )
            }

            setProgress(
                workDataOf(
                    WorkKeys.KEY_STAGE to "DECODING",
                    WorkKeys.KEY_PROGRESS_PERCENT to 15
                )
            )

            setProgress(
                workDataOf(
                    WorkKeys.KEY_STAGE to "PROCESSING",
                    WorkKeys.KEY_PROGRESS_PERCENT to 50
                )
            )

            val result = imageEngine.process(sourceUri, pipeline)

            setProgress(
                workDataOf(
                    WorkKeys.KEY_STAGE to "VALIDATING_AND_SAVING",
                    WorkKeys.KEY_PROGRESS_PERCENT to 85
                )
            )

            try {
                val inputName = sourceUri.lastPathSegment ?: "image"
                val outputName = result.outputUri.lastPathSegment ?: "processed_image"
                historyRepository.addEntry(
                    ProcessingHistory(
                        inputUri = sourceUri.toString(),
                        inputFileName = inputName,
                        outputUri = result.outputUri.toString(),
                        outputFileName = outputName,
                        operation = com.scanflow.photocompressor.domain.model.OperationType.COMPRESS,
                        originalSize = result.originalBytes,
                        resultSize = result.outputBytes,
                        width = result.width,
                        height = result.height
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record history: ${e.message}")
            }

            setProgress(
                workDataOf(
                    WorkKeys.KEY_STAGE to "COMPLETE",
                    WorkKeys.KEY_PROGRESS_PERCENT to 100
                )
            )

            return@withContext Result.success(
                workDataOf(
                    WorkKeys.KEY_OUTPUT_URI to result.outputUri.toString(),
                    WorkKeys.KEY_PROGRESS_PERCENT to 100
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Image processing failed: ${e.message}", e)
            return@withContext Result.failure(
                workDataOf(WorkKeys.KEY_ERROR_MESSAGE to (e.message ?: "Unknown processing error"))
            )
        } finally {
            try {
                fileManager.cleanTempFiles()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean temp files: ${e.message}")
            }
            try {
                com.scanflow.photocompressor.util.UriHelper.cleanupStagedUris(applicationContext)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean staged URIs: ${e.message}")
            }
        }
    }
}

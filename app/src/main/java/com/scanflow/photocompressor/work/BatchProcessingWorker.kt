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
 * Background WorkManager worker for executing batch image processing.
 *
 * Guarantees:
 * - Strict sequential execution (concurrency = 1)
 * - Safe memory release per item
 * - Progress reporting via setProgress
 * - Cooperative cancellation (isStopped)
 * - Temp file cleanup in finally
 * - Rule 29 (Batch Failure Policy): A single corrupt image never fails the entire batch.
 *   Continues to subsequent items after any failure.
 */
class BatchProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "BatchProcessingWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkManagerEntryPoint::class.java
        )
        val imageEngine = entryPoint.imageEngine()
        val fileManager = entryPoint.fileManager()
        val historyRepository = entryPoint.historyRepository()

        val sourceUriStrings = inputData.getStringArray(WorkKeys.KEY_SOURCE_URIS) ?: emptyArray()
        val totalCount = sourceUriStrings.size

        if (totalCount == 0) {
            return@withContext Result.success(
                workDataOf(
                    WorkKeys.KEY_TOTAL_COUNT to 0,
                    WorkKeys.KEY_SUCCESS_COUNT to 0,
                    WorkKeys.KEY_FAILED_COUNT to 0,
                    WorkKeys.KEY_CANCELLED_COUNT to 0
                )
            )
        }

        // Reconstruct pipeline
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

        val outputUris = mutableListOf<String>()
        var successCount = 0
        var failedCount = 0
        var cancelledCount = 0

        try {
            for (index in sourceUriStrings.indices) {
                // Cooperative cancellation check
                if (isStopped) {
                    val remaining = totalCount - (successCount + failedCount)
                    cancelledCount += remaining
                    Log.i(TAG, "Batch worker stopped. $remaining items marked CANCELLED.")
                    break
                }

                val uriString = sourceUriStrings[index]
                val sourceUri = Uri.parse(uriString)

                // Report progress
                val currentProgress = ((index.toFloat() / totalCount.toFloat()) * 100).toInt()
                setProgress(
                    workDataOf(
                        WorkKeys.KEY_PROGRESS_PERCENT to currentProgress,
                        WorkKeys.KEY_CURRENT_URI to uriString,
                        WorkKeys.KEY_TOTAL_COUNT to totalCount,
                        WorkKeys.KEY_SUCCESS_COUNT to successCount,
                        WorkKeys.KEY_FAILED_COUNT to failedCount,
                        WorkKeys.KEY_CANCELLED_COUNT to cancelledCount
                    )
                )

                // Process item with individual isolation (Rule 29: Batch Failure Policy)
                try {
                    val result = imageEngine.process(sourceUri, pipeline)
                    outputUris.add(result.outputUri.toString())
                    successCount++

                    // Record in history
                    try {
                        val inputName = sourceUri.lastPathSegment ?: "image_${index + 1}"
                        val outputName = result.outputUri.lastPathSegment ?: "compressed_${index + 1}"
                        historyRepository.addEntry(
                            ProcessingHistory(
                                inputUri = sourceUri.toString(),
                                inputFileName = inputName,
                                outputUri = result.outputUri.toString(),
                                outputFileName = outputName,
                                operation = com.scanflow.photocompressor.domain.model.OperationType.BATCH,
                                originalSize = result.originalBytes,
                                resultSize = result.outputBytes,
                                width = result.width,
                                height = result.height
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to write history for $sourceUri: ${e.message}")
                    }
                } catch (e: Exception) {
                    // One image corrupt/failed does not fail the batch
                    failedCount++
                    Log.e(TAG, "Batch item failed for $sourceUri: ${e.message}", e)
                }
            }
        } finally {
            // Temp file cleanup & memory release
            try {
                fileManager.cleanTempFiles()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean temp files in worker: ${e.message}")
            }
            try {
                com.scanflow.photocompressor.util.UriHelper.cleanupStagedUris(applicationContext)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean staged URIs in worker: ${e.message}")
            }
        }

        // Final completion progress
        setProgress(
            workDataOf(
                WorkKeys.KEY_PROGRESS_PERCENT to 100,
                WorkKeys.KEY_TOTAL_COUNT to totalCount,
                WorkKeys.KEY_SUCCESS_COUNT to successCount,
                WorkKeys.KEY_FAILED_COUNT to failedCount,
                WorkKeys.KEY_CANCELLED_COUNT to cancelledCount
            )
        )

        // 29. BATCH FAILURE POLICY:
        // Result is valid even if individual items failed or were cancelled.
        val outputData = workDataOf(
            WorkKeys.KEY_TOTAL_COUNT to totalCount,
            WorkKeys.KEY_SUCCESS_COUNT to successCount,
            WorkKeys.KEY_FAILED_COUNT to failedCount,
            WorkKeys.KEY_CANCELLED_COUNT to cancelledCount,
            WorkKeys.KEY_OUTPUT_URIS to outputUris.toTypedArray()
        )

        return@withContext Result.success(outputData)
    }
}

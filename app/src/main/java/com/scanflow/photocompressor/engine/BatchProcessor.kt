package com.scanflow.photocompressor.engine

import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine responsible for processing multi-image batch jobs.
 *
 * Rules:
 * - Supports batch sizes: 1, 10, 50, 100+ items.
 * - Sequential processing flow: Multi-select -> Create Job -> Queue -> Process one item -> Save -> Release memory -> Next item.
 * - Initial concurrency: 1 (never processes 100 bitmaps in parallel; peak memory is strictly 1 image in flight).
 * - Retry: Skips already completed (SUCCESS) items to re-process only failed/cancelled items without duplicating work.
 * - Cancellation (Rule 31): Stops pending items, marks active/remaining items CANCELLED, cleans up temp files, preserves completed outputs.
 */
@Singleton
class BatchProcessor @Inject constructor(
    private val imagePipelineEngine: ImagePipelineEngine,
    private val fileManager: FileManager? = null
) {
    companion object {
        /**
         * 62. BATCH MEMORY POLICY
         * Default Concurrency = 1.
         * Strict Rule: Never decode the entire batch to memory.
         * Peak memory in flight is strictly 1 image at any time.
         */
        const val DEFAULT_CONCURRENCY = 1
    }

    /**
     * Executes the given [BatchJob] sequentially under Policy 62:
     * Pattern:
     * read one -> decode -> process -> encode -> save -> release -> next
     */
    fun process(job: BatchJob, concurrency: Int = DEFAULT_CONCURRENCY): Flow<BatchJob> = flow {
        require(concurrency >= 1) { "Concurrency must be >= 1" }
        var currentJob = job
        val itemsList = currentJob.items.toMutableList()

        // 1. Emit Initial State (Queue)
        emit(currentJob)

        try {
            for (index in itemsList.indices) {
                val context = currentCoroutineContext()
                if (!context.isActive) {
                    // If job was cancelled, mark remaining queued/processing items as CANCELLED
                    for (rem in index until itemsList.size) {
                        if (itemsList[rem].status == BatchItemStatus.QUEUED || itemsList[rem].status == BatchItemStatus.PROCESSING) {
                            itemsList[rem] = itemsList[rem].copy(status = BatchItemStatus.CANCELLED)
                        }
                    }
                    currentJob = currentJob.copy(items = itemsList.toList())
                    emit(currentJob)
                    break
                }

                val currentItem = itemsList[index]

                // Skip items that have already completed successfully (preserves completed outputs on retry)
                if (currentItem.status == BatchItemStatus.SUCCESS) {
                    continue
                }

                // 2. Mark Current Item as PROCESSING
                itemsList[index] = currentItem.copy(status = BatchItemStatus.PROCESSING)
                currentJob = currentJob.copy(items = itemsList.toList())
                emit(currentJob)

                // 3. Sequential Lifecycle:
                // read one -> decode -> process -> encode -> save -> release -> next
                try {
                    val pipelineResult = imagePipelineEngine.execute(
                        inputUri = currentItem.sourceUri,
                        pipeline = currentJob.operation,
                        operationType = OperationType.BATCH
                    )

                    if (pipelineResult.isSuccess) {
                        val result = pipelineResult.getOrThrow()
                        itemsList[index] = itemsList[index].copy(
                            status = BatchItemStatus.SUCCESS,
                            outputUri = result.outputUri,
                            originalBytes = result.originalSize,
                            outputBytes = result.compressedSize,
                            error = null
                        )
                    } else {
                        val err = pipelineResult.exceptionOrNull()
                        itemsList[index] = itemsList[index].copy(
                            status = BatchItemStatus.FAILED,
                            error = ProcessingError.fromThrowable(err)
                        )
                    }
                } catch (ce: CancellationException) {
                    // Handle coroutine cancellation cleanly
                    itemsList[index] = itemsList[index].copy(status = BatchItemStatus.CANCELLED)
                    for (rem in (index + 1) until itemsList.size) {
                        if (itemsList[rem].status == BatchItemStatus.QUEUED || itemsList[rem].status == BatchItemStatus.PROCESSING) {
                            itemsList[rem] = itemsList[rem].copy(status = BatchItemStatus.CANCELLED)
                        }
                    }
                    try {
                        fileManager?.cleanTempFiles()
                    } catch (_: Exception) {}
                    throw ce
                } catch (t: Throwable) {
                    // Error isolation: item failure does not abort subsequent items
                    itemsList[index] = itemsList[index].copy(
                        status = BatchItemStatus.FAILED,
                        error = ProcessingError.fromThrowable(t)
                    )
                } finally {
                    // Step: [release] - Clean up per-item temp files immediately before moving to next item
                    try {
                        fileManager?.cleanTempFiles()
                    } catch (e: Exception) {
                        android.util.Log.w("BatchProcessor", "Failed to clean per-item temp files: ${e.message}")
                    }
                }

                // 4. Memory is released immediately, emit progress
                currentJob = currentJob.copy(items = itemsList.toList())
                emit(currentJob)
            }
        } finally {
            try {
                fileManager?.cleanTempFiles()
            } catch (e: Exception) {
                android.util.Log.w("BatchProcessor", "Failed to clean final temp files: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.Default)
}

package com.scanflow.photocompressor.domain.model

import android.net.Uri
import java.util.UUID

/**
 * 27. BATCH MODEL
 * Represents a batch processing job containing multiple items under a single [ImagePipeline] operation.
 */
data class BatchJob(
    val id: String = UUID.randomUUID().toString(),
    val items: List<BatchItem>,
    val operation: ImagePipeline,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalCount: Int get() = items.size
    val completedCount: Int get() = items.count { it.status == BatchItemStatus.SUCCESS }
    val failedCount: Int get() = items.count { it.status == BatchItemStatus.FAILED }
    val processingCount: Int get() = items.count { it.status == BatchItemStatus.PROCESSING }
    val queuedCount: Int get() = items.count { it.status == BatchItemStatus.QUEUED }
    val cancelledCount: Int get() = items.count { it.status == BatchItemStatus.CANCELLED }

    val isComplete: Boolean get() = items.isNotEmpty() && items.all {
        it.status == BatchItemStatus.SUCCESS || it.status == BatchItemStatus.FAILED || it.status == BatchItemStatus.CANCELLED
    }

    val progressPercentage: Float get() = if (totalCount > 0) {
        val finished = completedCount + failedCount + cancelledCount
        finished.toFloat() / totalCount
    } else 0f

    val errorCount: Int get() = failedCount
    val totalSavedBytes: Long get() = items.filter { it.status == BatchItemStatus.SUCCESS }
        .sumOf { (it.originalBytes - it.outputBytes).coerceAtLeast(0L) }
}

/**
 * An individual item within a [BatchJob].
 */
data class BatchItem(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: Uri,
    val status: BatchItemStatus = BatchItemStatus.QUEUED,
    val outputUri: Uri? = null,
    val error: ProcessingError? = null,
    val originalBytes: Long = 0L,
    val outputBytes: Long = 0L
)

/**
 * Status of an individual [BatchItem] in the queue.
 */
enum class BatchItemStatus {
    QUEUED,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED
}


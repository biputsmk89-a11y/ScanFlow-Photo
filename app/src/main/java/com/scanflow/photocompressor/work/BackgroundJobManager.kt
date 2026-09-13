package com.scanflow.photocompressor.work

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.scanflow.photocompressor.domain.model.BatchJob
import com.scanflow.photocompressor.domain.model.ImageOperation
import com.scanflow.photocompressor.domain.model.ImagePipeline
import com.scanflow.photocompressor.util.UriHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized manager for enqueuing, observing, and controlling background WorkManager jobs
 * for Batch Processing, Long-Running Image Operations, and PDF Creation.
 * Ensures URI persistence and accessibility across process lifecycles.
 */
@Singleton
class BackgroundJobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {

    /**
     * Enqueue a batch processing job in the background with persistent URIs.
     */
    fun enqueueBatch(job: BatchJob): UUID {
        val uriStrings = job.items.map {
            UriHelper.prepareUriForBackgroundProcessing(context, it.sourceUri).toString()
        }.toTypedArray()

        var quality = 80
        var maxWidth = 0
        var maxHeight = 0
        var format = "JPEG"
        var removeMetadata = false

        for (op in job.operation.operations) {
            when (op) {
                is ImageOperation.Compress -> quality = op.quality
                is ImageOperation.Resize -> {
                    maxWidth = op.width ?: 0
                    maxHeight = op.height ?: 0
                }
                is ImageOperation.Convert -> format = op.format.name
                is ImageOperation.RemoveMetadata -> removeMetadata = op.removeExif
                else -> {}
            }
        }

        val inputData = workDataOf(
            WorkKeys.KEY_JOB_ID to job.id,
            WorkKeys.KEY_SOURCE_URIS to uriStrings,
            WorkKeys.KEY_QUALITY to quality,
            WorkKeys.KEY_MAX_WIDTH to maxWidth,
            WorkKeys.KEY_MAX_HEIGHT to maxHeight,
            WorkKeys.KEY_FORMAT to format,
            WorkKeys.KEY_REMOVE_METADATA to removeMetadata
        )

        val workRequest = OneTimeWorkRequestBuilder<BatchProcessingWorker>()
            .setInputData(inputData)
            .addTag("batch_processing")
            .addTag(job.id)
            .build()

        workManager.enqueue(workRequest)
        return workRequest.id
    }

    /**
     * Enqueue a long-running single image processing operation in the background.
     */
    fun enqueueImageProcessing(sourceUri: Uri, pipeline: ImagePipeline): UUID {
        var quality = 80
        var maxWidth = 0
        var maxHeight = 0
        var format = "JPEG"
        var removeMetadata = false

        for (op in pipeline.operations) {
            when (op) {
                is ImageOperation.Compress -> quality = op.quality
                is ImageOperation.Resize -> {
                    maxWidth = op.width ?: 0
                    maxHeight = op.height ?: 0
                }
                is ImageOperation.Convert -> format = op.format.name
                is ImageOperation.RemoveMetadata -> removeMetadata = op.removeExif
                else -> {}
            }
        }

        val preparedUri = UriHelper.prepareUriForBackgroundProcessing(context, sourceUri)

        val inputData = workDataOf(
            WorkKeys.KEY_SOURCE_URI to preparedUri.toString(),
            WorkKeys.KEY_QUALITY to quality,
            WorkKeys.KEY_MAX_WIDTH to maxWidth,
            WorkKeys.KEY_MAX_HEIGHT to maxHeight,
            WorkKeys.KEY_FORMAT to format,
            WorkKeys.KEY_REMOVE_METADATA to removeMetadata
        )

        val workRequest = OneTimeWorkRequestBuilder<ImageProcessingWorker>()
            .setInputData(inputData)
            .addTag("image_processing")
            .build()

        workManager.enqueue(workRequest)
        return workRequest.id
    }

    /**
     * Enqueue a background job to create a PDF from a list of image URIs.
     */
    fun enqueuePdfCreation(imageUris: List<Uri>, title: String = "document"): UUID {
        val uriStrings = imageUris.map { it.toString() }.toTypedArray()

        val inputData = workDataOf(
            WorkKeys.KEY_SOURCE_URIS to uriStrings,
            WorkKeys.KEY_PDF_TITLE to title
        )

        val workRequest = OneTimeWorkRequestBuilder<PdfCreationWorker>()
            .setInputData(inputData)
            .addTag("pdf_creation")
            .build()

        workManager.enqueue(workRequest)
        return workRequest.id
    }

    /**
     * Observe the status, progress, and output of a background work request.
     */
    fun observeWork(id: UUID): Flow<WorkInfo?> {
        return workManager.getWorkInfoByIdFlow(id)
    }

    /**
     * Cancel a running or enqueued background work request.
     */
    fun cancelWork(id: UUID) {
        workManager.cancelWorkById(id)
    }

    /**
     * Cancel all work tagged with a specific tag (e.g. "batch_processing").
     */
    fun cancelAllWorkByTag(tag: String) {
        workManager.cancelAllWorkByTag(tag)
    }
}

package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.engine.BatchProcessor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for batch processing multiple images.
 * Delegates to [BatchProcessor] to guarantee strict sequential execution (concurrency = 1)
 * and aggressive memory cleanup between items.
 */
class BatchCompressUseCase @Inject constructor(
    private val batchProcessor: BatchProcessor,
    private val entitlementRepository: com.scanflow.photocompressor.domain.repository.EntitlementRepository? = null
) {
    companion object {
        const val FREE_BATCH_LIMIT = 5
    }

    /**
     * Process a batch of images with the given preset.
     */
    operator fun invoke(
        images: List<ImageInfo>,
        preset: CompressionPreset
    ): Flow<BatchJob> {
        val effectiveImages = if (entitlementRepository != null &&
            !entitlementRepository.isFeatureUnlocked(ProFeature.UNLIMITED_BATCH) &&
            images.size > FREE_BATCH_LIMIT
        ) {
            images.take(FREE_BATCH_LIMIT)
        } else {
            images
        }
        val ops = mutableListOf<ImageOperation>()
        val maxDim = preset.maxDimension ?: maxOf(preset.maxWidth, preset.maxHeight)
        if (maxDim > 0) {
            ops.add(
                ImageOperation.Resize(
                    width = maxDim,
                    height = maxDim,
                    maintainAspectRatio = true
                )
            )
        }
        ops.add(ImageOperation.Compress(quality = preset.quality ?: 80))
        preset.format?.let { ops.add(ImageOperation.Convert(format = it)) }
        if (preset.removeGps || !preset.preserveExif) {
            ops.add(ImageOperation.RemoveMetadata(removeExif = true))
        }

        val job = BatchJob(
            items = effectiveImages.map { BatchItem(sourceUri = it.uri) },
            operation = ImagePipeline(ops)
        )
        return batchProcessor.process(job)
    }

    /**
     * Process a batch of URIs with a customizable [ImagePipeline].
     */
    operator fun invoke(
        uris: List<Uri>,
        operation: ImagePipeline
    ): Flow<BatchJob> {
        val effectiveUris = if (entitlementRepository != null &&
            !entitlementRepository.isFeatureUnlocked(ProFeature.UNLIMITED_BATCH) &&
            uris.size > FREE_BATCH_LIMIT
        ) {
            uris.take(FREE_BATCH_LIMIT)
        } else {
            uris
        }
        val job = BatchJob(
            items = effectiveUris.map { BatchItem(sourceUri = it) },
            operation = operation
        )
        return batchProcessor.process(job)
    }

    /**
     * Process an existing [BatchJob].
     */
    operator fun invoke(job: BatchJob): Flow<BatchJob> = batchProcessor.process(job)
}

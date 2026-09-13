package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.engine.BatchProcessor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 56. USE CASE: ProcessBatchUseCase
 * Orchestrates batch operations across multiple image URIs with strictly isolated sequential execution.
 */
class ProcessBatchUseCase @Inject constructor(
    private val batchProcessor: BatchProcessor
) {
    operator fun invoke(
        uris: List<Uri>,
        operation: ImagePipeline
    ): Flow<BatchJob> {
        val job = BatchJob(
            items = uris.map { BatchItem(sourceUri = it) },
            operation = operation
        )
        return batchProcessor.process(job)
    }

    operator fun invoke(
        images: List<ImageInfo>,
        preset: CompressionPreset
    ): Flow<BatchJob> {
        val ops = mutableListOf<ImageOperation>()
        val maxDim = preset.maxDimension ?: maxOf(preset.maxWidth, preset.maxHeight)
        if (maxDim > 0) {
            ops.add(ImageOperation.Resize(width = maxDim, height = maxDim, maintainAspectRatio = true))
        }
        ops.add(ImageOperation.Compress(quality = preset.quality ?: 80))
        preset.format?.let { ops.add(ImageOperation.Convert(format = it)) }
        if (preset.removeGps || !preset.preserveExif) {
            ops.add(ImageOperation.RemoveMetadata(removeExif = true))
        }

        val job = BatchJob(
            items = images.map { BatchItem(sourceUri = it.uri) },
            operation = ImagePipeline(ops)
        )
        return batchProcessor.process(job)
    }
}

package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 56. USE CASE: RemoveMetadataUseCase
 * Strips EXIF or GPS metadata while preserving original image pixels.
 */
class RemoveMetadataUseCase @Inject constructor(
    private val imagePipelineEngine: com.scanflow.photocompressor.engine.ImagePipelineEngine
) {
    suspend operator fun invoke(
        inputUri: Uri,
        option: MetadataOption = MetadataOption.REMOVE_ALL,
        quality: Int = 95
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        val ops = listOf(
            ImageOperation.Metadata(option),
            ImageOperation.Compress(quality)
        )
        imagePipelineEngine.execute(
            inputUri = inputUri,
            pipeline = ImagePipeline(ops),
            operationType = OperationType.COMPRESS
        )
    }
}

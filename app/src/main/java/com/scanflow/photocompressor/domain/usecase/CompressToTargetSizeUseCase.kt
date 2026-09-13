package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.CompressionResult
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.MetadataOption
import javax.inject.Inject

/**
 * 56. USE CASE: CompressToTargetSizeUseCase
 * Compresses an image to fit within a specific target file size (in bytes) using binary search quality tuning.
 */
class CompressToTargetSizeUseCase @Inject constructor(
    private val compressImageUseCase: CompressImageUseCase
) {
    suspend operator fun invoke(
        inputUri: Uri,
        targetSizeBytes: Long,
        format: ImageFormat = ImageFormat.JPEG,
        maxWidth: Int = 0,
        maxHeight: Int = 0,
        metadataOption: MetadataOption = MetadataOption.REMOVE_GPS
    ): Result<CompressionResult> {
        return compressImageUseCase(
            inputUri = inputUri,
            quality = 80,
            format = format,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            targetSizeBytes = targetSizeBytes,
            metadataOption = metadataOption
        )
    }
}

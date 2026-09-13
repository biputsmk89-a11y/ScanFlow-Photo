package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.CompressionResult
import com.scanflow.photocompressor.domain.model.ImageFormat
import javax.inject.Inject

/**
 * 56. USE CASE: ConvertImageUseCase
 * Converts image file format (JPEG, PNG, WEBP, etc.) with alpha channel handling.
 * Conforms directly to the naming required by specification 56.
 */
class ConvertImageUseCase @Inject constructor(
    private val convertFormatUseCase: ConvertFormatUseCase
) {
    suspend operator fun invoke(
        inputUri: Uri,
        targetFormat: ImageFormat,
        quality: Int = 90,
        backgroundColor: Int = android.graphics.Color.WHITE
    ): Result<CompressionResult> {
        return convertFormatUseCase(
            inputUri = inputUri,
            targetFormat = targetFormat,
            quality = quality,
            backgroundColor = backgroundColor
        )
    }
}

package com.scanflow.photocompressor.engine

import android.graphics.BitmapFactory
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ValidationResult
import com.scanflow.photocompressor.domain.model.ValidationStep
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates encoded image files before promotion to final storage.
 *
 * Strict 5-Step Sequential Validation:
 * 1. file exists
 * 2. file > 0 bytes
 * 3. MIME valid
 * 4. decode output successfully
 * 5. dimensions valid
 *
 * Only after all 5 validation checks pass: SUCCESS.
 */
@Singleton
class OutputValidator @Inject constructor() {

    companion object {
        val SUPPORTED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
        )
    }

    /**
     * Validates an encoded temporary file according to the strict 5-step rule.
     *
     * @param file The file to validate.
     * @param expectedFormat Optional expected [ImageFormat] to verify against.
     * @param minWidth Minimum acceptable width (default 1).
     * @param minHeight Minimum acceptable height (default 1).
     * @return [ValidationResult.Success] if all 5 steps pass, or [ValidationResult.Failure] describing the first failing step.
     */
    fun validate(
        file: File,
        expectedFormat: ImageFormat? = null,
        minWidth: Int = 1,
        minHeight: Int = 1
    ): ValidationResult {
        // Step 1: file exists
        if (!file.exists()) {
            return ValidationResult.Failure(
                step = ValidationStep.FILE_EXISTS,
                reason = "Output file does not exist: ${file.absolutePath}"
            )
        }

        // Step 2: file > 0 bytes
        val fileLength = file.length()
        if (fileLength <= 0L) {
            return ValidationResult.Failure(
                step = ValidationStep.FILE_NOT_EMPTY,
                reason = "Output file is empty (0 bytes): ${file.name}"
            )
        }

        // Read image bounds and MIME type without allocating full bitmap memory
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        // Step 3: MIME valid
        val detectedMime = options.outMimeType
        if (detectedMime.isNullOrBlank()) {
            return ValidationResult.Failure(
                step = ValidationStep.MIME_VALID,
                reason = "Unable to detect a valid image MIME type from encoded file"
            )
        }

        val normalizedMime = detectedMime.lowercase()
        if (normalizedMime !in SUPPORTED_MIME_TYPES) {
            return ValidationResult.Failure(
                step = ValidationStep.MIME_VALID,
                reason = "Unsupported MIME type: '$detectedMime'"
            )
        }

        if (expectedFormat != null) {
            val matchesExpected = when (expectedFormat) {
                ImageFormat.JPEG -> normalizedMime == "image/jpeg" || normalizedMime == "image/jpg"
                ImageFormat.PNG -> normalizedMime == "image/png"
                ImageFormat.WEBP, ImageFormat.WEBP_LOSSLESS -> normalizedMime == "image/webp"
            }
            if (!matchesExpected) {
                return ValidationResult.Failure(
                    step = ValidationStep.MIME_VALID,
                    reason = "MIME type mismatch: detected '$detectedMime' but expected '${expectedFormat.mimeType}'"
                )
            }
        }

        // Step 4: decode output successfully
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) {
            return ValidationResult.Failure(
                step = ValidationStep.DECODE_SUCCESS,
                reason = "Failed to decode output file header or corrupted image stream"
            )
        }

        // Step 5: dimensions valid
        if (width < minWidth || height < minHeight) {
            return ValidationResult.Failure(
                step = ValidationStep.DIMENSIONS_VALID,
                reason = "Invalid output dimensions (${width}x${height}), required >= ${minWidth}x${minHeight}"
            )
        }

        // All 5 steps passed: SUCCESS
        return ValidationResult.Success(
            file = file,
            mimeType = detectedMime,
            width = width,
            height = height,
            fileSizeBytes = fileLength
        )
    }
}

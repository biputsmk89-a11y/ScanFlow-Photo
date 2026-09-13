package com.scanflow.photocompressor.domain.model

import java.io.File

/**
 * Sequential steps executed during output image validation.
 */
enum class ValidationStep {
    /**
     * Step 1: Output file must exist in the file system.
     */
    FILE_EXISTS,

    /**
     * Step 2: Output file size must be strictly greater than 0 bytes.
     */
    FILE_NOT_EMPTY,

    /**
     * Step 3: MIME type must be detected, valid, and match supported image formats.
     */
    MIME_VALID,

    /**
     * Step 4: Encoded file must decode successfully without corruption.
     */
    DECODE_SUCCESS,

    /**
     * Step 5: Decoded image width and height must be valid positive integers.
     */
    DIMENSIONS_VALID
}

/**
 * Result of the output validation sequence.
 */
sealed interface ValidationResult {
    /**
     * Returned only after all 5 validation checks have passed successfully.
     */
    data class Success(
        val file: File,
        val mimeType: String,
        val width: Int,
        val height: Int,
        val fileSizeBytes: Long
    ) : ValidationResult

    /**
     * Returned immediately if any of the 5 validation checks fail.
     */
    data class Failure(
        val step: ValidationStep,
        val reason: String
    ) : ValidationResult
}

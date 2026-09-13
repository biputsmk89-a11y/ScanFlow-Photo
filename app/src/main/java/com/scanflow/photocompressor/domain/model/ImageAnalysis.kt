package com.scanflow.photocompressor.domain.model

/**
 * Detailed image analysis result produced before pipeline processing.
 */
data class ImageAnalysis(
    val width: Int,
    val height: Int,
    val mimeType: String,
    val fileSizeBytes: Long,
    val orientation: Int,
    val hasAlpha: Boolean,
    val estimatedMemoryBytes: Long,
    val exifAvailable: Boolean
) {
    val estimatedMemoryMB: Float
        get() = estimatedMemoryBytes / (1024f * 1024f)

    val fileSizeMB: Float
        get() = fileSizeBytes / (1024f * 1024f)

    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height else 1f
}

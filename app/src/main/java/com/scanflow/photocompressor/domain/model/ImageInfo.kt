package com.scanflow.photocompressor.domain.model

import android.net.Uri

/**
 * Core model representing an image loaded into the application.
 */
data class ImageInfo(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long, // bytes
    val width: Int,
    val height: Int,
    val format: ImageFormat,
    val mimeType: String,
    val hasAlpha: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
) {
    val fileSizeKB: Double get() = fileSize / 1024.0
    val fileSizeMB: Double get() = fileSize / (1024.0 * 1024.0)
    val resolution: String get() = "${width}x${height}"
    val megapixels: Double get() = (width.toLong() * height.toLong()) / 1_000_000.0
}

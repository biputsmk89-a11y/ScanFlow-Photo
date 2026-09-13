package com.scanflow.photocompressor.domain.model

import android.net.Uri
import java.util.UUID

/**
 * Lightweight, memory-safe domain representation of an image asset.
 *
 * CRITICAL MEMORY RULE:
 * This model only holds lightweight metadata (Uri, dimensions, size, mimeType).
 * Full-size Bitmap instances are NEVER retained in permanent UI or ViewModel state.
 */
data class ImageAsset(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: Uri,
    val mimeType: String,
    val displayName: String?,
    val fileSizeBytes: Long?,
    val width: Int,
    val height: Int,
    val orientation: Int = 0,
    val hasAlpha: Boolean = false,
    val hasExif: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height else 1f

    val fileSizeBytesSafe: Long
        get() = fileSizeBytes ?: 0L

    val fileSizeKB: Float
        get() = fileSizeBytesSafe / 1024f

    val fileSizeMB: Float
        get() = fileSizeBytesSafe / (1024f * 1024f)

    val resolution: String
        get() = "${width}x${height}"
}

/**
 * Extension to convert legacy ImageInfo to memory-safe ImageAsset.
 */
fun ImageInfo.toImageAsset(
    orientation: Int = 0,
    hasAlpha: Boolean = false,
    hasExif: Boolean = false
): ImageAsset = ImageAsset(
    id = uri.toString(),
    sourceUri = uri,
    mimeType = mimeType,
    displayName = fileName,
    fileSizeBytes = fileSize,
    width = width,
    height = height,
    orientation = orientation,
    hasAlpha = hasAlpha,
    hasExif = hasExif
)

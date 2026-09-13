package com.scanflow.photocompressor.domain.model

import android.net.Uri
import com.scanflow.photocompressor.util.ReductionCalculator

/**
 * Result of an image compression operation.
 */
data class CompressionResult(
    val originalSize: Long,
    val compressedSize: Long,
    val outputUri: Uri,
    val outputFileName: String,
    val width: Int,
    val height: Int,
    val format: ImageFormat,
    val quality: Int,
    val durationMs: Long,
    val preservedExif: Boolean = false
) {
    val originalBytes: Long get() = originalSize
    val outputBytes: Long get() = compressedSize

    val savedBytes: Long get() = ReductionCalculator.calculateSavedBytes(originalBytes, outputBytes)
    val reductionPercent: Double get() = ReductionCalculator.calculateReductionPercent(originalBytes, outputBytes)
    val savedPercentage: Double get() = reductionPercent
    val compressionRatio: Double get() = if (originalBytes > 0) outputBytes.toDouble() / originalBytes else 1.0
    val originalSizeMB: Double get() = originalSize / (1024.0 * 1024.0)
    val compressedSizeMB: Double get() = compressedSize / (1024.0 * 1024.0)
}

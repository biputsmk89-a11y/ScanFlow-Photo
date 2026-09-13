package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import com.scanflow.photocompressor.domain.model.ImageFormat
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core compression engine using Android's Bitmap.compress API.
 * Supports JPEG, PNG, and WEBP formats with quality control.
 */
@Singleton
class CompressionEngine @Inject constructor() {

    /**
     * Compress a bitmap to a byte array with the specified format and quality.
     *
     * @param bitmap The source bitmap to compress.
     * @param format The target image format.
     * @param quality The compression quality (1-100). Higher = better quality, larger file.
     * @return Compressed image as a byte array.
     */
    fun compress(bitmap: Bitmap, format: ImageFormat, quality: Int): ByteArray {
        val clampedQuality = quality.coerceIn(1, 100)
        val compressFormat = toCompressFormat(format)

        return ByteArrayOutputStream().use { outputStream ->
            bitmap.compress(compressFormat, clampedQuality, outputStream)
            outputStream.toByteArray()
        }
    }

    /**
     * Compress a bitmap to achieve a target file size using adaptive binary search.
     *
     * Algorithm flow:
     * ANALYZE INPUT -> SET TARGET -> INITIAL QUALITY RANGE -> ENCODE MIDPOINT ->
     * MEASURE OUTPUT -> TOO LARGE? (LOWER/RAISE) -> REPEAT -> TARGET MET / ITERATION LIMIT.
     *
     * If quality reduction alone cannot satisfy the target:
     * QUALITY REDUCTION -> DIMENSION REDUCTION -> COMPRESS AGAIN.
     *
     * Never produces corrupt output.
     */
    fun compressToTargetSize(
        bitmap: Bitmap,
        format: ImageFormat,
        targetSizeBytes: Long,
        minQuality: Int = 20,
        maxQuality: Int = 95,
        maxQualityIterations: Int = 7,
        maxDimensionReductions: Int = 3
    ): TargetCompressionResult {
        require(targetSizeBytes > 0) { "Target size must be greater than 0 bytes" }

        var workingBitmap = bitmap
        var ownsWorkingBitmap = false

        try {
            var dimensionReductionAttempt = 0

            while (dimensionReductionAttempt <= maxDimensionReductions) {
                // 1. INITIAL QUALITY RANGE
                var low = minQuality
                var high = maxQuality
                var bestBytes: ByteArray? = null
                var bestQuality = minQuality

                // 2. ENCODE MIDPOINT & MEASURE OUTPUT (Iterative Binary Search)
                var iterations = 0
                while (low <= high && iterations < maxQualityIterations) {
                    iterations++
                    val mid = (low + high) / 2
                    val encoded = compress(workingBitmap, format, mid)

                    if (encoded.size <= targetSizeBytes) {
                        // Candidate satisfies target size: record it and try higher quality
                        bestBytes = encoded
                        bestQuality = mid
                        low = mid + 1
                    } else {
                        // Output too large: reduce quality
                        high = mid - 1
                    }
                }

                // 3. TARGET MET?
                if (bestBytes != null && bestBytes.size <= targetSizeBytes) {
                    return TargetCompressionResult(
                        bytes = bestBytes,
                        quality = bestQuality,
                        finalBitmap = workingBitmap,
                        isFeasible = true,
                        message = "Target reached"
                    )
                }

                // If quality reduction was not enough, proceed to DIMENSION REDUCTION
                if (dimensionReductionAttempt < maxDimensionReductions &&
                    workingBitmap.width > 240 && workingBitmap.height > 240
                ) {
                    dimensionReductionAttempt++
                    val nextWidth = (workingBitmap.width * 0.8f).toInt().coerceAtLeast(1)
                    val nextHeight = (workingBitmap.height * 0.8f).toInt().coerceAtLeast(1)

                    val scaledBitmap = Bitmap.createScaledBitmap(workingBitmap, nextWidth, nextHeight, true)
                    if (ownsWorkingBitmap && workingBitmap !== bitmap) {
                        workingBitmap.recycle()
                    }
                    workingBitmap = scaledBitmap
                    ownsWorkingBitmap = true
                } else {
                    break
                }
            }

            // Target size not feasible within safe quality and dimension limits.
            // Produce valid, non-corrupt output at minQuality with clear explanation.
            val fallbackBytes = compress(workingBitmap, format, minQuality)
            val isFeasible = fallbackBytes.size <= targetSizeBytes

            return TargetCompressionResult(
                bytes = fallbackBytes,
                finalBitmap = workingBitmap,
                quality = minQuality,
                isFeasible = isFeasible,
                message = if (!isFeasible) {
                    "The target size could not be reached without significantly reducing image quality or dimensions."
                } else null
            )
        } catch (e: Exception) {
            if (ownsWorkingBitmap && workingBitmap !== bitmap) {
                workingBitmap.recycle()
            }
            throw e
        }
    }

    /**
     * Estimate the compressed size at a given quality without fully compressing.
     * This is an approximation based on a small sample.
     */
    fun estimateCompressedSize(
        bitmap: Bitmap,
        format: ImageFormat,
        quality: Int
    ): Long {
        // For a quick estimate, compress the full image
        // In production, you could sample a portion for faster estimates
        val bytes = compress(bitmap, format, quality)
        return bytes.size.toLong()
    }

    /**
     * Convert ImageFormat to Android's Bitmap.CompressFormat.
     */
    private fun toCompressFormat(format: ImageFormat): Bitmap.CompressFormat {
        return when (format) {
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.WEBP -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
            ImageFormat.WEBP_LOSSLESS -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }
    }
}

/**
 * Result model for target size compression algorithm.
 */
data class TargetCompressionResult(
    val bytes: ByteArray,
    val quality: Int,
    val finalBitmap: Bitmap,
    val isFeasible: Boolean,
    val message: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TargetCompressionResult
        if (!bytes.contentEquals(other.bytes)) return false
        if (quality != other.quality) return false
        if (finalBitmap != other.finalBitmap) return false
        if (isFeasible != other.isFeasible) return false
        if (message != other.message) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + quality
        result = 31 * result + finalBitmap.hashCode()
        result = 31 * result + isFeasible.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }
}

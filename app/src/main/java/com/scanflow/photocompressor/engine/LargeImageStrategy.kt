package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Custom exception thrown when a very large image (50-100MB) cannot be safely processed
 * by the device's available memory, triggering Graceful Rejection.
 */
class LargeImageRejectedException(
    val fileSizeMB: Double,
    val rawWidth: Int,
    val rawHeight: Int,
    message: String
) : IllegalStateException(message)

/**
 * Image Size Tiers according to specification:
 * - Normal (1–5 MB): Fast processing
 * - Large (10–30 MB): Stable processing with adaptive downsampling
 * - Very large (50–100 MB): Safe fallback or graceful rejection
 *
 * Rule: "Jangan memaksakan processing jika device tidak mampu."
 */
enum class ImageSizeTier {
    NORMAL,     // 1–5 MB: fast
    LARGE,      // 10–30 MB: stable
    VERY_LARGE  // 50–100 MB: safe fallback / graceful rejection
}

data class TierDecodePlan(
    val tier: ImageSizeTier,
    val inSampleSize: Int,
    val preferredConfig: Bitmap.Config,
    val maxDimension: Int,
    val shouldReject: Boolean = false,
    val rejectionReason: String? = null
)

object LargeImageStrategy {

    const val ONE_MB = 1024L * 1024L
    const val CRITICAL_HEAP_HEADROOM_BYTES = 48L * ONE_MB // Minimum free heap required for system & UI safety

    /**
     * Determines the size tier based on the input file size in bytes:
     * - < 10 MB: NORMAL (covers 1-5 MB)
     * - 10 MB to < 50 MB: LARGE (covers 10-30 MB)
     * - >= 50 MB: VERY_LARGE (covers 50-100 MB)
     */
    fun determineTier(fileSizeBytes: Long): ImageSizeTier {
        val sizeMB = fileSizeBytes.toDouble() / ONE_MB
        return when {
            sizeMB >= 50.0 -> ImageSizeTier.VERY_LARGE
            sizeMB >= 10.0 -> ImageSizeTier.LARGE
            else -> ImageSizeTier.NORMAL
        }
    }

    /**
     * Inspects available JVM heap memory.
     */
    fun getAvailableHeapMemory(): Long {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val allocated = totalMemory - freeMemory
        return (maxMemory - allocated).coerceAtLeast(0L)
    }

    /**
     * Creates a decode plan tailored specifically for the input size tier and current device memory:
     *
     * 1. Normal (1–5 MB):
     *    Fast decoding, standard ARGB_8888, full fidelity.
     *
     * 2. Large (10–30 MB):
     *    Stable decoding, adaptive power-of-two sampling to 3840px max, RGB_565 fallback under heap pressure.
     *
     * 3. Very large (50–100 MB):
     *    Safe fallback / graceful rejection.
     *    If available heap is critically low (< 48MB) or device cannot safely handle the footprint:
     *    gracefully rejects processing to protect device stability ("Jangan memaksakan processing jika device tidak mampu").
     *    Otherwise, applies safe aggressive downsampling (sample size >= 4, max 2048px, RGB_565).
     */
    fun createDecodePlan(
        fileSizeBytes: Long,
        rawWidth: Int,
        rawHeight: Int,
        reqWidth: Int = 0,
        reqHeight: Int = 0,
        availableHeapBytes: Long = getAvailableHeapMemory()
    ): TierDecodePlan {
        val tier = determineTier(fileSizeBytes)
        val sizeMB = fileSizeBytes.toDouble() / ONE_MB
        val targetConstraint = maxOf(reqWidth, reqHeight)

        return when (tier) {
            ImageSizeTier.NORMAL -> {
                // Normal: fast path
                TierDecodePlan(
                    tier = ImageSizeTier.NORMAL,
                    inSampleSize = 1,
                    preferredConfig = Bitmap.Config.ARGB_8888,
                    maxDimension = if (targetConstraint > 0) targetConstraint else 4096,
                    shouldReject = false
                )
            }

            ImageSizeTier.LARGE -> {
                // Large: stable path
                val config = if (availableHeapBytes < 64L * ONE_MB) {
                    Bitmap.Config.RGB_565
                } else {
                    Bitmap.Config.ARGB_8888
                }

                TierDecodePlan(
                    tier = ImageSizeTier.LARGE,
                    inSampleSize = 1,
                    preferredConfig = config,
                    maxDimension = if (targetConstraint > 0) minOf(targetConstraint, 3840) else 3840,
                    shouldReject = false
                )
            }

            ImageSizeTier.VERY_LARGE -> {
                // Very large (50–100 MB): safe fallback / graceful rejection
                val estimatedFootprintArgb = rawWidth.toLong() * rawHeight.toLong() * 4L

                // If device available memory is critically low (< 48MB) or cannot safely fit estimated footprint, gracefully reject
                if (availableHeapBytes < CRITICAL_HEAP_HEADROOM_BYTES || (availableHeapBytes < estimatedFootprintArgb / 4)) {
                    return TierDecodePlan(
                        tier = ImageSizeTier.VERY_LARGE,
                        inSampleSize = 4,
                        preferredConfig = Bitmap.Config.RGB_565,
                        maxDimension = 2048,
                        shouldReject = true,
                        rejectionReason = "Device has only ${availableHeapBytes / ONE_MB}MB available memory. Processing a very large image (${String.format("%.1f", sizeMB)}MB) was safely rejected to prevent device freeze or crash."
                    )
                }

                // If memory is available: execute Safe Fallback
                TierDecodePlan(
                    tier = ImageSizeTier.VERY_LARGE,
                    inSampleSize = 4,
                    preferredConfig = Bitmap.Config.RGB_565,
                    maxDimension = if (targetConstraint > 0) minOf(targetConstraint, 2048) else 2048,
                    shouldReject = false
                )
            }
        }
    }
}

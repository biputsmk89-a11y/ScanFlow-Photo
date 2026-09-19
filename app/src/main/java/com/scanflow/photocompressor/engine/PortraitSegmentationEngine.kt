package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 100% On-Device Offline Portrait Segmentation Engine using Google ML Kit.
 * Extracts human subjects (hair, face, clothing) and produces transparent cutouts
 * or composite studio backgrounds with professional studio-grade edges.
 *
 * Quality pipeline:
 * 1. ML Kit confidence mask → raw alpha map with wide transition (0.12–0.88)
 * 2. Smoothstep interpolation for natural, non-linear feathering
 * 3. Morphological alpha cleanup to remove stray isolated pixels
 * 4. Multi-pass (2×) 3×3 edge-aware Gaussian smoothing
 * 5. Anti-aliased, dithered Paint for compositing
 *
 * Result: Clean, professional edges comparable to studio passport photos
 * across all output resolutions (low and high).
 */
@Singleton
class PortraitSegmentationEngine @Inject constructor() {

    companion object {
        /** Number of smoothing passes for edge refinement. 2 = studio quality. */
        private const val EDGE_SMOOTH_PASSES = 2

        /** Minimum confidence to be considered part of the subject */
        private const val ALPHA_THRESHOLD_LOW = 0.12f

        /** Confidence above which pixel is fully opaque (subject) */
        private const val ALPHA_THRESHOLD_HIGH = 0.88f

        /** Range for normalized interpolation */
        private const val ALPHA_RANGE = ALPHA_THRESHOLD_HIGH - ALPHA_THRESHOLD_LOW
    }

    private val segmenter: Segmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
        Segmentation.getClient(options)
    }

    /**
     * Removes the background from a portrait bitmap, returning a transparent ARGB_8888 bitmap.
     * Uses multi-stage refinement for studio-grade edge quality at any resolution.
     */
    suspend fun removeBackground(sourceBitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        runCatching {
            val workingBitmap = if (sourceBitmap.config != Bitmap.Config.ARGB_8888) {
                sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                sourceBitmap
            }

            val inputImage = InputImage.fromBitmap(workingBitmap, 0)
            val mask = Tasks.await(segmenter.process(inputImage))

            val width = workingBitmap.width
            val height = workingBitmap.height
            val maskWidth = mask.width
            val maskHeight = mask.height
            val maskBuffer = mask.buffer
            maskBuffer.rewind()

            val srcPixels = IntArray(width * height)
            workingBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)

            // Stage 1: Build raw alpha map with smoothstep interpolation
            val rawAlpha = FloatArray(width * height)

            if (maskWidth == width && maskHeight == height) {
                for (i in 0 until width * height) {
                    val confidence = maskBuffer.float
                    rawAlpha[i] = confidenceToAlpha(confidence)
                }
            } else {
                val maskData = FloatArray(maskWidth * maskHeight)
                maskBuffer.asFloatBuffer().get(maskData)

                for (y in 0 until height) {
                    val maskY = (y * maskHeight / height).coerceIn(0, maskHeight - 1)
                    val maskRowOffset = maskY * maskWidth
                    val imgRowOffset = y * width

                    for (x in 0 until width) {
                        val maskX = (x * maskWidth / width).coerceIn(0, maskWidth - 1)
                        val confidence = maskData[maskRowOffset + maskX]
                        rawAlpha[imgRowOffset + x] = confidenceToAlpha(confidence)
                    }
                }
            }

            // Stage 2: Morphological cleanup — remove stray isolated pixels
            val cleanedAlpha = morphologicalCleanup(rawAlpha, width, height)

            // Stage 3: Multi-pass 3×3 edge-aware Gaussian smoothing
            var refinedAlpha = cleanedAlpha
            for (pass in 0 until EDGE_SMOOTH_PASSES) {
                refinedAlpha = smoothAlphaEdges(refinedAlpha, width, height)
            }

            // Stage 4: Apply refined alpha to source pixels
            val outPixels = IntArray(width * height)
            for (i in 0 until width * height) {
                val originalPixel = srcPixels[i]
                val originalAlpha = (originalPixel ushr 24) and 0xFF
                val finalAlpha = (originalAlpha * refinedAlpha[i]).toInt().coerceIn(0, 255)
                outPixels[i] = (finalAlpha shl 24) or (originalPixel and 0x00FFFFFF)
            }

            val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            outputBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)

            if (workingBitmap != sourceBitmap) {
                workingBitmap.recycle()
            }

            outputBitmap
        }.getOrElse {
            // Fallback: return a copy of the source bitmap if ML Kit fails or non-portrait
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * Converts ML Kit confidence (0..1) to alpha using smoothstep interpolation.
     * Smoothstep produces a natural S-curve transition that avoids harsh edges
     * while maintaining crisp separation between subject and background.
     */
    private fun confidenceToAlpha(confidence: Float): Float {
        return when {
            confidence <= ALPHA_THRESHOLD_LOW -> 0f
            confidence >= ALPHA_THRESHOLD_HIGH -> 1f
            else -> {
                // Normalized linear value
                val t = ((confidence - ALPHA_THRESHOLD_LOW) / ALPHA_RANGE).coerceIn(0f, 1f)
                // Smoothstep: 3t² - 2t³ (produces natural S-curve feathering)
                t * t * (3f - 2f * t)
            }
        }
    }

    /**
     * Morphological alpha cleanup: removes stray isolated transparent/opaque pixels
     * at edges. A pixel is "stray" if fewer than 3 of its 8 neighbors share its
     * extreme state (fully opaque or fully transparent). This cleans up noise
     * without affecting the natural edge transition.
     */
    private fun morphologicalCleanup(alpha: FloatArray, width: Int, height: Int): FloatArray {
        val result = alpha.copyOf()

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val center = alpha[idx]

                // Only clean fully opaque or fully transparent stray pixels
                if (center == 0f || center == 1f) {
                    var sameCount = 0
                    // Check 8-connected neighbors
                    if (alpha[idx - width - 1] == center) sameCount++
                    if (alpha[idx - width] == center) sameCount++
                    if (alpha[idx - width + 1] == center) sameCount++
                    if (alpha[idx - 1] == center) sameCount++
                    if (alpha[idx + 1] == center) sameCount++
                    if (alpha[idx + width - 1] == center) sameCount++
                    if (alpha[idx + width] == center) sameCount++
                    if (alpha[idx + width + 1] == center) sameCount++

                    // If fewer than 3 neighbors share the same state → stray pixel
                    if (sameCount < 3) {
                        // Replace with average of neighbors for smooth blending
                        val avg = (
                            alpha[idx - width - 1] + alpha[idx - width] + alpha[idx - width + 1] +
                            alpha[idx - 1] + alpha[idx + 1] +
                            alpha[idx + width - 1] + alpha[idx + width] + alpha[idx + width + 1]
                        ) / 8f
                        result[idx] = avg
                    }
                }
            }
        }

        return result
    }

    /**
     * 3×3 edge-aware weighted Gaussian smoothing kernel.
     * Only smooths pixels in the transition zone (0 < alpha < 1) to preserve
     * crisp subject interior and clean transparent background.
     * Weights: corners=1, edges=2, center=4. Total=16.
     */
    private fun smoothAlphaEdges(alpha: FloatArray, width: Int, height: Int): FloatArray {
        val result = alpha.copyOf()

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val center = alpha[idx]

                // Only smooth transition-zone pixels (edge area)
                if (center > 0f && center < 1f) {
                    // 3×3 weighted Gaussian average (center-heavy for sharpness)
                    val sum =
                        alpha[idx - width - 1]      + alpha[idx - width] * 2f + alpha[idx - width + 1] +
                        alpha[idx - 1] * 2f          + center * 4f             + alpha[idx + 1] * 2f +
                        alpha[idx + width - 1]       + alpha[idx + width] * 2f + alpha[idx + width + 1]
                    result[idx] = (sum / 16f).coerceIn(0f, 1f)
                }
            }
        }

        return result
    }

    /**
     * Composites the portrait onto a solid background color.
     * Uses anti-aliased, filtered, dithered Paint for professional studio compositing.
     */
    suspend fun replaceBackground(sourceBitmap: Bitmap, backgroundColor: Int): Bitmap = withContext(Dispatchers.Default) {
        val cutout = removeBackground(sourceBitmap)
        val result = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(backgroundColor)
        // Professional composite paint: anti-alias + bilinear filter + dither
        val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isDither = true
        }
        canvas.drawBitmap(cutout, 0f, 0f, compositePaint)
        cutout.recycle()
        result
    }
}

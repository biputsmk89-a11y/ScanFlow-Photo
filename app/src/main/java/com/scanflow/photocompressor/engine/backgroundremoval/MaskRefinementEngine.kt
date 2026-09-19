package com.scanflow.photocompressor.engine.backgroundremoval

import java.util.BitSet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-precision mask refinement pipeline for studio passport and ID photography.
 * Transforms coarse ML Kit confidence maps into razor-sharp, anti-aliased, hole-free,
 * island-free continuous alpha mattes.
 */
@Singleton
class MaskRefinementEngine @Inject constructor() {

    /**
     * Executes complete mask refinement pipeline.
     */
    fun refine(
        rawMask: FloatArray,
        maskWidth: Int,
        maskHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        options: BackgroundRemovalOptions = BackgroundRemovalOptions()
    ): AlphaMask {
        // Stage 1: Continuous 2D sub-pixel bilinear resampling
        val resampled = resampleBilinear(rawMask, maskWidth, maskHeight, targetWidth, targetHeight)

        // Stage 2: S-Curve Hermite Smoothstep Alpha Mapping
        val alphaBuffer = FloatArray(targetWidth * targetHeight)
        for (i in alphaBuffer.indices) {
            alphaBuffer[i] = confidenceToAlpha(
                confidence = resampled[i],
                lowThreshold = options.lowConfidenceThreshold,
                highThreshold = options.highConfidenceThreshold
            )
        }

        // Stage 3: Connected Component Island Pruning (eradicates floating dots)
        val cleanAlpha = if (options.enableIslandPruning) {
            pruneNoiseIslands(alphaBuffer, targetWidth, targetHeight)
        } else {
            alphaBuffer
        }

        // Stage 4: Morphological Hole Closing (solid interior for hair, face, clothes)
        val closedAlpha = if (options.enableHoleClosing) {
            closeSubjectHoles(cleanAlpha, targetWidth, targetHeight)
        } else {
            cleanAlpha
        }

        // Stage 5: Multi-pass Edge-aware Gaussian Smoothing (anti-aliased natural boundary)
        var smoothedAlpha = closedAlpha
        for (pass in 0 until options.edgeFeatherRadius) {
            smoothedAlpha = smoothAlphaEdges(smoothedAlpha, targetWidth, targetHeight)
        }

        return AlphaMask(targetWidth, targetHeight, smoothedAlpha)
    }

    /**
     * Continuous 2D sub-pixel bilinear resampling of confidence map.
     * Completely eliminates blocky 8x8 staircasing and jagged pixel steps.
     */
    fun resampleBilinear(
        maskData: FloatArray,
        maskWidth: Int,
        maskHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): FloatArray {
        if (maskWidth == targetWidth && maskHeight == targetHeight) {
            return maskData.copyOf()
        }

        val output = FloatArray(targetWidth * targetHeight)
        val scaleX = maskWidth.toFloat() / targetWidth.toFloat()
        val scaleY = maskHeight.toFloat() / targetHeight.toFloat()

        for (y in 0 until targetHeight) {
            val gy = (y + 0.5f) * scaleY - 0.5f
            val y0 = gy.toInt().coerceIn(0, maskHeight - 1)
            val y1 = (y0 + 1).coerceIn(0, maskHeight - 1)
            val fy = (gy - y0).coerceIn(0f, 1f)
            val row0 = y0 * maskWidth
            val row1 = y1 * maskWidth
            val outRow = y * targetWidth

            for (x in 0 until targetWidth) {
                val gx = (x + 0.5f) * scaleX - 0.5f
                val x0 = gx.toInt().coerceIn(0, maskWidth - 1)
                val x1 = (x0 + 1).coerceIn(0, maskWidth - 1)
                val fx = (gx - x0).coerceIn(0f, 1f)

                val c00 = maskData[row0 + x0]
                val c10 = maskData[row0 + x1]
                val c01 = maskData[row1 + x0]
                val c11 = maskData[row1 + x1]

                val top = c00 + fx * (c10 - c00)
                val bottom = c01 + fx * (c11 - c01)
                output[outRow + x] = top + fy * (bottom - top)
            }
        }
        return output
    }

    /**
     * Converts raw confidence (0..1) to alpha using smoothstep interpolation.
     */
    fun confidenceToAlpha(
        confidence: Float,
        lowThreshold: Float = 0.28f,
        highThreshold: Float = 0.82f
    ): Float {
        val range = highThreshold - lowThreshold
        return when {
            confidence <= lowThreshold -> 0f
            confidence >= highThreshold -> 1f
            else -> {
                val t = ((confidence - lowThreshold) / range).coerceIn(0f, 1f)
                t * t * (3f - 2f * t)
            }
        }
    }

    /**
     * Connected-Component noise pruning via Breadth-First Search (BFS).
     * Identifies and erases small isolated clusters of positive alpha in the background
     * that do not belong to the main human portrait mass.
     */
    fun pruneNoiseIslands(alpha: FloatArray, width: Int, height: Int): FloatArray {
        val result = alpha.copyOf()
        val visited = BitSet(width * height)
        val minIslandPixels = maxOf(400, (width * height) / 800)

        var queue = IntArray(1024)
        var component = IntArray(1024)

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val idx = rowOffset + x
                if (result[idx] > 0.05f && !visited.get(idx)) {
                    var head = 0
                    var tail = 0
                    var compCount = 0
                    var maxY = y

                    if (tail >= queue.size) queue = queue.copyOf(queue.size * 2)
                    queue[tail++] = idx
                    visited.set(idx)

                    while (head < tail) {
                        val curr = queue[head++]
                        if (compCount >= component.size) component = component.copyOf(component.size * 2)
                        component[compCount++] = curr

                        val cy = curr / width
                        val cx = curr % width
                        if (cy > maxY) maxY = cy

                        val neighbors = intArrayOf(
                            if (cx > 0) curr - 1 else -1,
                            if (cx < width - 1) curr + 1 else -1,
                            if (cy > 0) curr - width else -1,
                            if (cy < height - 1) curr + width else -1
                        )

                        for (n in neighbors) {
                            if (n != -1 && !visited.get(n) && result[n] > 0.05f) {
                                visited.set(n)
                                if (tail >= queue.size) queue = queue.copyOf(queue.size * 2)
                                queue[tail++] = n
                            }
                        }
                    }

                    // A component is noise if small and not touching the lower torso/body region
                    val touchesTorso = maxY >= (height * 0.60f).toInt()
                    if (compCount < minIslandPixels && !touchesTorso) {
                        for (i in 0 until compCount) {
                            result[component[i]] = 0f
                        }
                    }
                }
            }
        }

        return result
    }

    /**
     * Morphological hole closing on subject core.
     * Encloses pinholes or transparency dips inside hair, clothing, or face.
     */
    fun closeSubjectHoles(alpha: FloatArray, width: Int, height: Int): FloatArray {
        val result = alpha.copyOf()
        val step = maxOf(1, minOf(width, height) / 300)

        for (y in step * 4 until height - step * 4) {
            val rowOffset = y * width
            for (x in step * 4 until width - step * 4) {
                val idx = rowOffset + x
                val center = result[idx]

                if (center < 0.90f) {
                    var leftSolid = false
                    var rightSolid = false
                    var upSolid = false
                    var downSolid = false

                    for (d in 1..4) {
                        if (result[idx - d * step] >= 0.90f) leftSolid = true
                        if (result[idx + d * step] >= 0.90f) rightSolid = true
                        if (leftSolid && rightSolid) break
                    }

                    for (d in 1..4) {
                        if (result[idx - d * step * width] >= 0.90f) upSolid = true
                        if (result[idx + d * step * width] >= 0.90f) downSolid = true
                        if (upSolid && downSolid) break
                    }

                    if (leftSolid && rightSolid && upSolid && downSolid) {
                        result[idx] = maxOf(center, 1.0f)
                    }
                }
            }
        }

        return result
    }

    /**
     * 3×3 edge-aware weighted Gaussian smoothing kernel.
     * Only smooths pixels in the transition zone (0 < alpha < 1).
     */
    fun smoothAlphaEdges(alpha: FloatArray, width: Int, height: Int): FloatArray {
        val result = alpha.copyOf()

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val center = alpha[idx]

                if (center > 0f && center < 1f) {
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
}

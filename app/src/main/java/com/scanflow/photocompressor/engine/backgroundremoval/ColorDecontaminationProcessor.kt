package com.scanflow.photocompressor.engine.backgroundremoval

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Solves the edge halo and color spill problem.
 * Replaces the RGB of semi-transparent border pixels with the true color of adjacent
 * solid foreground pixels (hair, skin, clothing) so that compositing onto new backgrounds
 * (e.g. Royal Blue, Red, White) does not carry over remnants of the old background.
 */
@Singleton
class ColorDecontaminationProcessor @Inject constructor() {

    companion object {
        private const val DEFAULT_RADIUS = 3
    }

    /**
     * Decontaminates edge pixels by propagating solid foreground color into transition pixels.
     * Removes the dirty halo/fringe of the original background.
     */
    fun decontaminate(
        srcPixels: IntArray,
        alpha: FloatArray,
        width: Int,
        height: Int,
        searchRadius: Int = DEFAULT_RADIUS
    ): IntArray {
        val effectiveRadius = if (searchRadius == DEFAULT_RADIUS) {
            maxOf(DEFAULT_RADIUS, minOf(width, height) / 100)
        } else {
            searchRadius
        }
        val result = srcPixels.copyOf()

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val idx = rowOffset + x
                val a = alpha[idx]

                // Only process transition boundary zone
                if (a > 0.01f && a < 0.95f) {
                    var sumR = 0
                    var sumG = 0
                    var sumB = 0
                    var count = 0

                    for (r in 1..effectiveRadius) {
                        val yMin = maxOf(0, y - r)
                        val yMax = minOf(height - 1, y + r)
                        val xMin = maxOf(0, x - r)
                        val xMax = minOf(width - 1, x + r)

                        for (ny in yMin..yMax) {
                            val nRow = ny * width
                            for (nx in xMin..xMax) {
                                val nIdx = nRow + nx
                                if (alpha[nIdx] >= 0.85f) {
                                    val distSq = (nx - x) * (nx - x) + (ny - y) * (ny - y)
                                    if (distSq <= r * r) {
                                        val col = srcPixels[nIdx]
                                        sumR += (col shr 16) and 0xFF
                                        sumG += (col shr 8) and 0xFF
                                        sumB += col and 0xFF
                                        count++
                                    }
                                }
                            }
                        }
                        if (count > 0) break
                    }

                    if (count > 0) {
                        val origAlpha = (srcPixels[idx] ushr 24) and 0xFF
                        val avgR = sumR / count
                        val avgG = sumG / count
                        val avgB = sumB / count
                        result[idx] = (origAlpha shl 24) or (avgR shl 16) or (avgG shl 8) or avgB
                    }
                }
            }
        }

        return result
    }
}

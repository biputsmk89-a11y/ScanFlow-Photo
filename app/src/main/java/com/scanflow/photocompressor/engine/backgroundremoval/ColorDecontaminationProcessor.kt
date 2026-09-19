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
     */
    fun decontaminate(
        srcPixels: IntArray,
        alpha: FloatArray,
        width: Int,
        height: Int,
        searchRadius: Int = DEFAULT_RADIUS
    ): IntArray {
        val result = srcPixels.copyOf()

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val idx = rowOffset + x
                val a = alpha[idx]

                // Only process transition boundary zone
                if (a > 0.01f && a < 0.95f) {
                    var bestColor = -1
                    var minDistanceSq = Int.MAX_VALUE

                    for (r in 1..searchRadius) {
                        val yMin = maxOf(0, y - r)
                        val yMax = minOf(height - 1, y + r)
                        val xMin = maxOf(0, x - r)
                        val xMax = minOf(width - 1, x + r)

                        for (ny in yMin..yMax) {
                            val nRow = ny * width
                            for (nx in xMin..xMax) {
                                val nIdx = nRow + nx
                                if (alpha[nIdx] >= 0.95f) {
                                    val distSq = (nx - x) * (nx - x) + (ny - y) * (ny - y)
                                    if (distSq < minDistanceSq) {
                                        minDistanceSq = distSq
                                        bestColor = srcPixels[nIdx]
                                    }
                                }
                            }
                        }
                        if (bestColor != -1) break
                    }

                    if (bestColor != -1) {
                        result[idx] = bestColor
                    }
                }
            }
        }

        return result
    }
}

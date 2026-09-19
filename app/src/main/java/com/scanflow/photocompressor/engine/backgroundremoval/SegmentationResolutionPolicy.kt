package com.scanflow.photocompressor.engine.backgroundremoval

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines safe working dimensions for AI background removal.
 * Prevents OutOfMemory errors when handling high-resolution camera images (e.g. 8000x6000)
 * while preserving fine hair, glasses, ear, and clothing details.
 */
@Singleton
class SegmentationResolutionPolicy @Inject constructor() {

    companion object {
        const val MIN_WORKING_DIMENSION = 512
        const val MAX_WORKING_DIMENSION = 2048
        const val DEFAULT_TARGET_DIMENSION = 1536
        private const val CRITICAL_HEAP_HEADROOM_MB = 48L
    }

    /**
     * Calculates optimal inference dimensions for a given source image size.
     */
    fun calculateWorkingDimensions(rawWidth: Int, rawHeight: Int): Pair<Int, Int> {
        if (rawWidth <= 0 || rawHeight <= 0) {
            return Pair(DEFAULT_TARGET_DIMENSION, DEFAULT_TARGET_DIMENSION)
        }

        val maxDim = maxOf(rawWidth, rawHeight)
        val availableHeapMb = (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory()) / (1024 * 1024)

        val ceiling = if (availableHeapMb < CRITICAL_HEAP_HEADROOM_MB) {
            1024
        } else {
            MAX_WORKING_DIMENSION
        }

        if (maxDim <= ceiling) {
            return Pair(rawWidth, rawHeight)
        }

        val scale = ceiling.toFloat() / maxDim.toFloat()
        val targetWidth = (rawWidth * scale).toInt().coerceAtLeast(MIN_WORKING_DIMENSION)
        val targetHeight = (rawHeight * scale).toInt().coerceAtLeast(MIN_WORKING_DIMENSION)

        return Pair(targetWidth, targetHeight)
    }
}

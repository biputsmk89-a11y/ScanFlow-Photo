package com.scanflow.photocompressor.domain.model

/**
 * Represents an aspect ratio for cropping operations.
 */
data class AspectRatio(
    val ratioX: Int,
    val ratioY: Int
) {
    val floatRatio: Float get() = if (ratioY > 0) ratioX.toFloat() / ratioY.toFloat() else 1f

    companion object {
        val SQUARE = AspectRatio(1, 1)
        val RATIO_4_5 = AspectRatio(4, 5)
        val RATIO_3_4 = AspectRatio(3, 4)
        val RATIO_4_3 = AspectRatio(4, 3)
        val RATIO_16_9 = AspectRatio(16, 9)
        val RATIO_9_16 = AspectRatio(9, 16)
        val RATIO_3_2 = AspectRatio(3, 2)
        val RATIO_2_3 = AspectRatio(2, 3)
    }
}

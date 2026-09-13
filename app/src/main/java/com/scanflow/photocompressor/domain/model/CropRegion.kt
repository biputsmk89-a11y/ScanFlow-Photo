package com.scanflow.photocompressor.domain.model

/**
 * Defines a crop region within an image.
 */
data class CropRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    init {
        require(x >= 0) { "x must be >= 0" }
        require(y >= 0) { "y must be >= 0" }
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }
    }

    val right: Int get() = x + width
    val bottom: Int get() = y + height
    val aspectRatio: Float get() = width.toFloat() / height.toFloat()
}

/**
 * Predefined aspect ratios for cropping.
 */
enum class AspectRatioPreset(val label: String, val ratioX: Int, val ratioY: Int) {
    FREE("Free", 0, 0),
    SQUARE("1:1", 1, 1),
    RATIO_4_5("4:5", 4, 5),
    RATIO_3_4("3:4", 3, 4),
    RATIO_4_3("4:3", 4, 3),
    RATIO_16_9("16:9", 16, 9),
    RATIO_9_16("9:16", 9, 16),
    CUSTOM("Custom", 0, 0);

    val ratio: Float?
        get() = if (ratioX > 0 && ratioY > 0) ratioX.toFloat() / ratioY.toFloat() else null
}

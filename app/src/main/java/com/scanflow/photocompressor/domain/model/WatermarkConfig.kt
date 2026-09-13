package com.scanflow.photocompressor.domain.model

/**
 * Configuration for text watermark overlay.
 */
data class WatermarkConfig(
    val text: String,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val opacity: Float = 0.5f, // 0.0 to 1.0
    val fontSize: Float = 24f,
    val color: Long = 0xFFFFFFFF, // ARGB long
    val rotation: Float = 0f,
    val margin: Int = 16 // dp
) {
    init {
        require(text.isNotBlank()) { "Watermark text must not be blank" }
        require(opacity in 0f..1f) { "Opacity must be between 0.0 and 1.0" }
        require(fontSize > 0f) { "Font size must be > 0" }
    }
}

enum class WatermarkPosition(val label: String) {
    TOP_LEFT("Top Left"),
    TOP_CENTER("Top Center"),
    TOP_RIGHT("Top Right"),
    CENTER_LEFT("Center Left"),
    CENTER("Center"),
    CENTER_RIGHT("Center Right"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_CENTER("Bottom Center"),
    BOTTOM_RIGHT("Bottom Right")
}

package com.scanflow.photocompressor.domain.model

/**
 * Standard resolution presets for image resizing.
 * Supported presets: 1080, 1440, 1600, 1920, 2560, Custom.
 */
enum class ResizePreset(
    val label: String,
    val dimension: Int
) {
    P_1080("1080", 1080),
    P_1440("1440", 1440),
    P_1600("1600", 1600),
    P_1920("1920", 1920),
    P_2560("2560", 2560),
    CUSTOM("Custom", 0)
}

package com.scanflow.photocompressor.domain.model

/**
 * High-level mode of image compression.
 */
enum class CompressionMode(val label: String) {
    QUICK("Quick"),
    QUALITY("Quality"),
    TARGET_SIZE("Target Size")
}

/**
 * Quick compression presets with balanced defaults.
 */
enum class QuickPreset(
    val label: String,
    val quality: Int,
    val description: String,
    val maxWidth: Int = 0,
    val maxHeight: Int = 0
) {
    SMALL("Small", 50, "Maximum space savings, aggressive compression", 1280, 1280),
    BALANCED("Balanced", 75, "Optimal balance between quality and file size", 2048, 2048),
    HIGH_QUALITY("High Quality", 90, "Preserves fine details with subtle compression", 0, 0)
}

/**
 * Preset target file sizes.
 */
enum class TargetSizePreset(
    val label: String,
    val bytes: Long
) {
    SIZE_100_KB("100 KB", 100L * 1024L),
    SIZE_250_KB("250 KB", 250L * 1024L),
    SIZE_500_KB("500 KB", 500L * 1024L),
    SIZE_1_MB("1 MB", 1024L * 1024L),
    SIZE_2_MB("2 MB", 2L * 1024L * 1024L),
    SIZE_5_MB("5 MB", 5L * 1024L * 1024L),
    CUSTOM("Custom", 0L)
}

/**
 * Preset quality steps as requested.
 */
val QUALITY_STEPS = listOf(40, 50, 60, 70, 80, 90, 95, 100)

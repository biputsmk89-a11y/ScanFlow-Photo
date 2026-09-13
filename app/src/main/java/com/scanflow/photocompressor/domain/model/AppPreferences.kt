package com.scanflow.photocompressor.domain.model

/**
 * Theme mode preferences.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Strategy for resolving filename collisions when saving output files.
 */
enum class ConflictStrategy {
    /**
     * Appends an incremental number (e.g. IMG_1234_compressed_1.jpg).
     */
    INCREMENT,

    /**
     * Appends a timestamp suffix to guarantee uniqueness.
     */
    TIMESTAMP,

    /**
     * Replaces existing file if permitted by storage policy.
     */
    OVERWRITE
}

/**
 * Default behavior preferences for compression and processing.
 */
data class DefaultBehavior(
    val preserveExif: Boolean = true,
    val keepAspectRatio: Boolean = true,
    val autoCleanTemp: Boolean = true,
    val conflictStrategy: ConflictStrategy = ConflictStrategy.INCREMENT
)

/**
 * Global application preferences stored in DataStore.
 */
data class AppPreferences(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val defaultQuality: Int = 80,
    val defaultFormat: ImageFormat = ImageFormat.JPEG,
    val behavior: DefaultBehavior = DefaultBehavior()
)

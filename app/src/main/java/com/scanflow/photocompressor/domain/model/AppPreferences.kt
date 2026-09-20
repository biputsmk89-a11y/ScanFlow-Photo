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
 * Prefix presets for output file naming.
 */
enum class NamingPrefixType(val displayName: String, val prefix: String) {
    ORIGINAL("Original Name", ""),
    SCAN("ScanFlow (SCAN_)", "SCAN_"),
    IMG("Camera (IMG_)", "IMG_"),
    DOC("Document (DOC_)", "DOC_"),
    CUSTOM("Custom", "")
}

/**
 * Configuration for output file naming templates.
 */
data class FileNamingConfig(
    val prefixType: NamingPrefixType = NamingPrefixType.ORIGINAL,
    val customPrefixText: String = "SCAN",
    val includeTimestamp: Boolean = false
) {
    fun getEffectivePrefix(baseOriginal: String): String {
        return when (prefixType) {
            NamingPrefixType.ORIGINAL -> baseOriginal
            NamingPrefixType.SCAN -> "SCAN_${baseOriginal.removePrefix("SCAN_")}"
            NamingPrefixType.IMG -> "IMG_${baseOriginal.removePrefix("IMG_")}"
            NamingPrefixType.DOC -> "DOC_${baseOriginal.removePrefix("DOC_")}"
            NamingPrefixType.CUSTOM -> {
                val clean = customPrefixText.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
                if (clean.isNotBlank()) "${clean}_$baseOriginal" else baseOriginal
            }
        }
    }
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
    val behavior: DefaultBehavior = DefaultBehavior(),
    val namingConfig: FileNamingConfig = FileNamingConfig()
)

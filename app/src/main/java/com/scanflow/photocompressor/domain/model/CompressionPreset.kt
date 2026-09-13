package com.scanflow.photocompressor.domain.model

import java.util.UUID

/**
 * A named preset for compression settings.
 * Adheres to Preset Engine specification.
 */
data class CompressionPreset(
    val id: String,
    val name: String,
    val quality: Int?,
    val maxDimension: Int?,
    val targetBytes: Long?,
    val format: ImageFormat?,
    val removeGps: Boolean
) {
    // Secondary constructors for backwards compatibility and ease of instantiation
    constructor(
        id: String = UUID.randomUUID().toString(),
        name: String,
        quality: Int? = null,
        maxDimension: Int? = null,
        targetBytes: Long? = null,
        format: ImageFormat? = null,
        removeGps: Boolean = false,
        maxWidth: Int = 0,
        maxHeight: Int = 0,
        preserveExif: Boolean = true
    ) : this(
        id = id,
        name = name,
        quality = quality,
        maxDimension = maxDimension ?: if (maxWidth > 0 || maxHeight > 0) maxOf(maxWidth, maxHeight) else null,
        targetBytes = targetBytes,
        format = format,
        removeGps = removeGps || !preserveExif
    )

    @Suppress("UNUSED_PARAMETER")
    constructor(
        id: Long,
        name: String,
        quality: Int = 80,
        maxWidth: Int = 0,
        maxHeight: Int = 0,
        format: ImageFormat = ImageFormat.JPEG,
        preserveExif: Boolean = true,
        isDefault: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ) : this(
        id = if (id > 0) id.toString() else UUID.randomUUID().toString(),
        name = name,
        quality = quality,
        maxDimension = if (maxWidth > 0 || maxHeight > 0) maxOf(maxWidth, maxHeight) else null,
        targetBytes = null,
        format = format,
        removeGps = !preserveExif
    )

    @Suppress("UNUSED_PARAMETER")
    constructor(
        name: String,
        quality: Int = 80,
        maxWidth: Int = 0,
        maxHeight: Int = 0,
        format: ImageFormat = ImageFormat.JPEG,
        preserveExif: Boolean = true,
        isDefault: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ) : this(
        id = UUID.randomUUID().toString(),
        name = name,
        quality = quality,
        maxDimension = if (maxWidth > 0 || maxHeight > 0) maxOf(maxWidth, maxHeight) else null,
        targetBytes = null,
        format = format,
        removeGps = !preserveExif
    )

    // Backwards-compatible accessors
    val maxWidth: Int get() = maxDimension ?: 0
    val maxHeight: Int get() = maxDimension ?: 0
    val preserveExif: Boolean get() = !removeGps
    val isDefault: Boolean get() = defaults.any { it.id == id }
    val createdAt: Long get() = System.currentTimeMillis()

    companion object {
        // --- Required System Presets ---

        val WHATSAPP = CompressionPreset(
            id = "whatsapp",
            name = "WhatsApp",
            quality = 70,
            maxDimension = 1600,
            targetBytes = null,
            format = ImageFormat.JPEG,
            removeGps = true
        )

        val EMAIL = CompressionPreset(
            id = "email",
            name = "Email",
            quality = 75,
            maxDimension = 1280,
            targetBytes = 1_000_000L, // 1 MB typical email attachment limit
            format = ImageFormat.JPEG,
            removeGps = true
        )

        val WEBSITE = CompressionPreset(
            id = "website",
            name = "Website",
            quality = 80,
            maxDimension = 1920,
            targetBytes = null,
            format = ImageFormat.WEBP,
            removeGps = true
        )

        /**
         * Example school upload preset configuration (Max width: 1600px, Quality: 80, Target: 500 KB, Format: JPEG, Remove GPS: Yes).
         * Note: This is a representative reference preset; upload limits and file requirements vary across schools and institutions.
         */
        val SCHOOL_UPLOAD = CompressionPreset(
            id = "school_upload",
            name = "School Upload",
            quality = 80,
            maxDimension = 1600,
            targetBytes = 500_000L, // Example 500 KB target; limits vary per school portal
            format = ImageFormat.JPEG,
            removeGps = true
        )

        val GOVERNMENT_UPLOAD = CompressionPreset(
            id = "government_upload",
            name = "Government Upload",
            quality = 85,
            maxDimension = 800,
            targetBytes = 200_000L, // 200 KB strict limit for CPNS/Visa/Government portals
            format = ImageFormat.JPEG,
            removeGps = true
        )

        val SOCIAL_MEDIA = CompressionPreset(
            id = "social_media",
            name = "Social Media",
            quality = 85,
            maxDimension = 1080,
            targetBytes = null,
            format = ImageFormat.JPEG,
            removeGps = true
        )

        val CUSTOM = CompressionPreset(
            id = "custom",
            name = "Custom",
            quality = null,
            maxDimension = null,
            targetBytes = null,
            format = null,
            removeGps = false
        )

        // System presets list
        val defaults = listOf(
            WHATSAPP,
            EMAIL,
            WEBSITE,
            SCHOOL_UPLOAD,
            GOVERNMENT_UPLOAD,
            SOCIAL_MEDIA,
            CUSTOM
        )

        // Backwards-compatible aliases for legacy callers
        val BALANCED = SOCIAL_MEDIA
        val SMALL_SIZE = EMAIL
        val HIGH_QUALITY = WEBSITE
        val WEB_OPTIMIZED = WEBSITE
        val SOCIAL_SQUARE = SOCIAL_MEDIA
        val SOCIAL_STORY = CompressionPreset(
            id = "social_story",
            name = "Social Story",
            quality = 85,
            maxDimension = 1920,
            targetBytes = null,
            format = ImageFormat.JPEG,
            removeGps = true
        )
        val PASSPORT = GOVERNMENT_UPLOAD
        val THUMBNAIL = CompressionPreset(
            id = "thumbnail",
            name = "Thumbnail",
            quality = 70,
            maxDimension = 512,
            targetBytes = null,
            format = ImageFormat.JPEG,
            removeGps = true
        )
    }
}

package com.scanflow.photocompressor.domain.model

/**
 * Tier representing the subscription/licensing level of the user.
 */
enum class UserTier(val displayName: String) {
    FREE("Free"),
    PRO("Pro")
}

/**
 * Granular features that can be gated or enhanced by the Pro tier.
 */
enum class ProFeature(
    val title: String,
    val description: String
) {
    UNLIMITED_BATCH(
        title = "Unlimited Batch",
        description = "Process unlimited photos simultaneously in background queues."
    ),
    TARGET_FILE_SIZE(
        title = "Target File Size",
        description = "Compress photos to exact target file sizes using smart binary search."
    ),
    ADVANCED_PRESETS(
        title = "Advanced Presets",
        description = "Unlock customized compression presets and threshold controls."
    ),
    METADATA_REMOVAL(
        title = "Metadata Removal",
        description = "Strip all EXIF, GPS location, and camera metadata tags."
    ),
    PASSPORT_TOOL(
        title = "Passport & ID Photo",
        description = "Generate passport photos with multi-photo print sheets and guide marks."
    ),
    SOCIAL_PRESETS(
        title = "Social Media Presets",
        description = "Export pre-formatted photos for Instagram, Facebook, TikTok, YouTube, etc."
    ),
    WHATSAPP_PRESETS(
        title = "WhatsApp Presets",
        description = "Optimize photos specifically for WhatsApp sharing without compression loss."
    ),
    ADVANCED_PDF(
        title = "Advanced PDF",
        description = "Generate multi-page PDFs with custom page sizes and layout controls."
    ),
    NO_ADS(
        title = "No Ads",
        description = "Enjoy clean, distraction-free photo processing with zero ads."
    ),
    ADVANCED_CONTROLS(
        title = "Advanced Controls",
        description = "Access fine-grained chroma subsampling, resampling algorithms, and DPI settings."
    )
}

package com.scanflow.photocompressor.domain.model

import android.graphics.Color
import android.net.Uri

// =========================================================================
// 1. PDF MODELS
// Pipeline: Select images -> Reorder -> Page size -> Margins -> Orientation -> Quality -> Generate
// =========================================================================

enum class PdfPageSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4", 595, 842),
    A5("A5", 420, 595),
    LETTER("Letter", 612, 792),
    ORIGINAL("Original", 0, 0),
    CUSTOM("Custom", 600, 800)
}

enum class PdfMargin(val displayName: String, val marginPt: Int) {
    NONE("No Margin", 0),
    SMALL("Narrow (0.25 in)", 18),
    NORMAL("Normal (0.5 in)", 36)
}

enum class PdfOrientation(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    AUTO("Auto")
}

enum class PdfQuality(val displayName: String, val compressionQuality: Int, val maxDimension: Int) {
    LOW("Compact (Small File)", 50, 1080),
    MEDIUM("Balanced (Standard)", 75, 1440),
    HIGH("High Quality (Crisp Print)", 90, 2048)
}

data class PdfConfig(
    val title: String = "document",
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val margin: PdfMargin = PdfMargin.NORMAL,
    val orientation: PdfOrientation = PdfOrientation.AUTO,
    val quality: PdfQuality = PdfQuality.MEDIUM,
    val customWidthPt: Int = 600,
    val customHeightPt: Int = 800
)

// =========================================================================
// 2. PASSPORT PHOTO MODELS
// Presets: 2 × 3, 3 × 4, 4 × 6, Custom
// Print layout: 1, 2, 4, 6, 8, 9, 12
// No automatic official compliance claims.
// =========================================================================

enum class PassportSpec(
    val displayName: String,
    val description: String,
    val ratioX: Int,
    val ratioY: Int,
    val targetWidthPx: Int,
    val targetHeightPx: Int
) {
    PRESET_2X3(
        displayName = "2 × 3",
        description = "Standard 2 × 3 aspect ratio",
        ratioX = 2,
        ratioY = 3,
        targetWidthPx = 400,
        targetHeightPx = 600
    ),
    PRESET_3X4(
        displayName = "3 × 4",
        description = "Standard 3 × 4 aspect ratio",
        ratioX = 3,
        ratioY = 4,
        targetWidthPx = 450,
        targetHeightPx = 600
    ),
    PRESET_4X6(
        displayName = "4 × 6",
        description = "Standard 4 × 6 aspect ratio",
        ratioX = 2,
        ratioY = 3,
        targetWidthPx = 600,
        targetHeightPx = 900
    ),
    CUSTOM(
        displayName = "Custom",
        description = "Custom aspect ratio",
        ratioX = 1,
        ratioY = 1,
        targetWidthPx = 600,
        targetHeightPx = 600
    )
}

enum class IdFrameStyle(val displayName: String, val borderWidthDp: Float) {
    NONE("No Frame", 0f),
    THIN("Thin Border", 2f),
    CLASSIC("Classic Border", 6f),
    PROFESSIONAL("Professional ID", 8f)
}

enum class PassportBackground(val displayName: String, val colorInt: Int?) {
    ORIGINAL("Original", null),
    BLUE("Blue", Color.rgb(0, 85, 165)),
    RED("Red", Color.rgb(211, 47, 47)),
    WHITE("White", Color.WHITE),
    GRAY("Light Gray", Color.rgb(224, 224, 224)),
    CUSTOM("Custom", null)
}

enum class PassportPrintLayout(
    val displayName: String,
    val copies: Int,
    val cols: Int,
    val rows: Int
) {
    COPIES_1("1", 1, 1, 1),
    COPIES_2("2", 2, 2, 1),
    COPIES_4("4", 4, 2, 2),
    COPIES_6("6", 6, 2, 3),
    COPIES_8("8", 8, 2, 4),
    COPIES_9("9", 9, 3, 3),
    COPIES_12("12", 12, 3, 4)
}

data class PassportConfig(
    val spec: PassportSpec = PassportSpec.PRESET_3X4,
    val background: PassportBackground = PassportBackground.ORIGINAL,
    val customBackgroundColor: Int? = null,
    val frameStyle: IdFrameStyle = IdFrameStyle.PROFESSIONAL,
    val zoom: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val rotationDegrees: Float = 0f,
    val printLayout: PassportPrintLayout = PassportPrintLayout.COPIES_1,
    val addCutMarks: Boolean = true,
    val disclaimer: String = "Check the submission requirements before printing.",
    val complianceNotice: String = "Passport & ID Photo Studio adalah alat foto identitas offline yang membantu pengguna melakukan crop, resize, positioning, penyesuaian warna latar, framing, dan print layout secara lokal di perangkat. Fitur ini tidak menggunakan AI background removal atau cloud processing."
) {
    /**
     * Resolves the effective background color (null for original).
     */
    val effectiveBackgroundColor: Int?
        get() = if (background == PassportBackground.CUSTOM) customBackgroundColor else background.colorInt
}

// =========================================================================
// 3. SOCIAL MEDIA MODELS
// Pipeline: Platform -> Preset -> Crop -> Resize -> Compress
// =========================================================================

enum class SocialPlatform(val displayName: String) {
    INSTAGRAM("Instagram"),
    FACEBOOK("Facebook"),
    TIKTOK("TikTok"),
    YOUTUBE("YouTube"),
    LINKEDIN("LinkedIn"),
    CUSTOM("Custom")
}

enum class SocialContentType(val displayName: String) {
    POST("Post"),
    STORY("Story"),
    COVER("Cover"),
    THUMBNAIL("Thumbnail"),
    PROFILE("Profile")
}

/**
 * Internal preset configuration. The user selects Platform and Content Type without
 * being forced to know or manage raw pixel dimensions.
 */
data class InternalSocialPreset(
    val platform: SocialPlatform,
    val type: SocialContentType,
    val ratioX: Int,
    val ratioY: Int,
    val targetWidth: Int,
    val targetHeight: Int,
    val recommendedQuality: Int = 85
)

object SocialPresetRegistry {
    private val PRESETS: Map<Pair<SocialPlatform, SocialContentType>, InternalSocialPreset> = mapOf(
        // Instagram
        (SocialPlatform.INSTAGRAM to SocialContentType.POST) to InternalSocialPreset(SocialPlatform.INSTAGRAM, SocialContentType.POST, 1, 1, 1080, 1080),
        (SocialPlatform.INSTAGRAM to SocialContentType.STORY) to InternalSocialPreset(SocialPlatform.INSTAGRAM, SocialContentType.STORY, 9, 16, 1080, 1920),
        (SocialPlatform.INSTAGRAM to SocialContentType.COVER) to InternalSocialPreset(SocialPlatform.INSTAGRAM, SocialContentType.COVER, 16, 9, 1080, 608),
        (SocialPlatform.INSTAGRAM to SocialContentType.THUMBNAIL) to InternalSocialPreset(SocialPlatform.INSTAGRAM, SocialContentType.THUMBNAIL, 1, 1, 1080, 1080),
        (SocialPlatform.INSTAGRAM to SocialContentType.PROFILE) to InternalSocialPreset(SocialPlatform.INSTAGRAM, SocialContentType.PROFILE, 1, 1, 320, 320),

        // Facebook
        (SocialPlatform.FACEBOOK to SocialContentType.POST) to InternalSocialPreset(SocialPlatform.FACEBOOK, SocialContentType.POST, 191, 100, 1200, 630),
        (SocialPlatform.FACEBOOK to SocialContentType.STORY) to InternalSocialPreset(SocialPlatform.FACEBOOK, SocialContentType.STORY, 9, 16, 1080, 1920),
        (SocialPlatform.FACEBOOK to SocialContentType.COVER) to InternalSocialPreset(SocialPlatform.FACEBOOK, SocialContentType.COVER, 16, 9, 820, 312),
        (SocialPlatform.FACEBOOK to SocialContentType.THUMBNAIL) to InternalSocialPreset(SocialPlatform.FACEBOOK, SocialContentType.THUMBNAIL, 191, 100, 1200, 630),
        (SocialPlatform.FACEBOOK to SocialContentType.PROFILE) to InternalSocialPreset(SocialPlatform.FACEBOOK, SocialContentType.PROFILE, 1, 1, 360, 360),

        // TikTok
        (SocialPlatform.TIKTOK to SocialContentType.POST) to InternalSocialPreset(SocialPlatform.TIKTOK, SocialContentType.POST, 9, 16, 1080, 1920),
        (SocialPlatform.TIKTOK to SocialContentType.STORY) to InternalSocialPreset(SocialPlatform.TIKTOK, SocialContentType.STORY, 9, 16, 1080, 1920),
        (SocialPlatform.TIKTOK to SocialContentType.COVER) to InternalSocialPreset(SocialPlatform.TIKTOK, SocialContentType.COVER, 9, 16, 1080, 1920),
        (SocialPlatform.TIKTOK to SocialContentType.THUMBNAIL) to InternalSocialPreset(SocialPlatform.TIKTOK, SocialContentType.THUMBNAIL, 9, 16, 1080, 1920),
        (SocialPlatform.TIKTOK to SocialContentType.PROFILE) to InternalSocialPreset(SocialPlatform.TIKTOK, SocialContentType.PROFILE, 1, 1, 300, 300),

        // YouTube
        (SocialPlatform.YOUTUBE to SocialContentType.POST) to InternalSocialPreset(SocialPlatform.YOUTUBE, SocialContentType.POST, 16, 9, 1200, 675),
        (SocialPlatform.YOUTUBE to SocialContentType.STORY) to InternalSocialPreset(SocialPlatform.YOUTUBE, SocialContentType.STORY, 9, 16, 1080, 1920),
        (SocialPlatform.YOUTUBE to SocialContentType.COVER) to InternalSocialPreset(SocialPlatform.YOUTUBE, SocialContentType.COVER, 16, 9, 2560, 1440),
        (SocialPlatform.YOUTUBE to SocialContentType.THUMBNAIL) to InternalSocialPreset(SocialPlatform.YOUTUBE, SocialContentType.THUMBNAIL, 16, 9, 1280, 720),
        (SocialPlatform.YOUTUBE to SocialContentType.PROFILE) to InternalSocialPreset(SocialPlatform.YOUTUBE, SocialContentType.PROFILE, 1, 1, 800, 800),

        // LinkedIn
        (SocialPlatform.LINKEDIN to SocialContentType.POST) to InternalSocialPreset(SocialPlatform.LINKEDIN, SocialContentType.POST, 191, 100, 1200, 627),
        (SocialPlatform.LINKEDIN to SocialContentType.STORY) to InternalSocialPreset(SocialPlatform.LINKEDIN, SocialContentType.STORY, 9, 16, 1080, 1920),
        (SocialPlatform.LINKEDIN to SocialContentType.COVER) to InternalSocialPreset(SocialPlatform.LINKEDIN, SocialContentType.COVER, 4, 1, 1584, 396),
        (SocialPlatform.LINKEDIN to SocialContentType.THUMBNAIL) to InternalSocialPreset(SocialPlatform.LINKEDIN, SocialContentType.THUMBNAIL, 191, 100, 1200, 627),
        (SocialPlatform.LINKEDIN to SocialContentType.PROFILE) to InternalSocialPreset(SocialPlatform.LINKEDIN, SocialContentType.PROFILE, 1, 1, 400, 400),

        // Custom
        (SocialPlatform.CUSTOM to SocialContentType.POST) to InternalSocialPreset(SocialPlatform.CUSTOM, SocialContentType.POST, 1, 1, 1080, 1080),
        (SocialPlatform.CUSTOM to SocialContentType.STORY) to InternalSocialPreset(SocialPlatform.CUSTOM, SocialContentType.STORY, 9, 16, 1080, 1920),
        (SocialPlatform.CUSTOM to SocialContentType.COVER) to InternalSocialPreset(SocialPlatform.CUSTOM, SocialContentType.COVER, 16, 9, 1280, 720),
        (SocialPlatform.CUSTOM to SocialContentType.THUMBNAIL) to InternalSocialPreset(SocialPlatform.CUSTOM, SocialContentType.THUMBNAIL, 4, 3, 1024, 768),
        (SocialPlatform.CUSTOM to SocialContentType.PROFILE) to InternalSocialPreset(SocialPlatform.CUSTOM, SocialContentType.PROFILE, 1, 1, 512, 512)
    )

    fun getPreset(platform: SocialPlatform, type: SocialContentType): InternalSocialPreset {
        return PRESETS[platform to type] ?: InternalSocialPreset(platform, type, 1, 1, 1080, 1080)
    }
}

// =========================================================================
// 4. WHATSAPP MODELS (Rule 68)
// Do NOT promise absolute file sizes. Use Small, Balanced, High Quality, Custom.
// =========================================================================

enum class WhatsAppTier(
    val displayName: String,
    val description: String,
    val maxDimension: Int,
    val quality: Int,
    val targetSizeBytes: Long
) {
    SMALL(
        displayName = "Small",
        description = "Compact & fast sharing (low data usage)",
        maxDimension = 800,
        quality = 65,
        targetSizeBytes = 200 * 1024L
    ),
    BALANCED(
        displayName = "Balanced",
        description = "Standard WhatsApp chat photo (Recommended)",
        maxDimension = 1280,
        quality = 78,
        targetSizeBytes = 500 * 1024L
    ),
    HIGH_QUALITY(
        displayName = "High Quality",
        description = "Maximum detail & HD photo clarity",
        maxDimension = 2048,
        quality = 88,
        targetSizeBytes = 1500 * 1024L
    ),
    CUSTOM(
        displayName = "Custom",
        description = "User-defined compression preference",
        maxDimension = 1920,
        quality = 80,
        targetSizeBytes = 0L
    )
}

data class WhatsAppConfig(
    val tier: WhatsAppTier = WhatsAppTier.BALANCED,
    val customTargetSizeKB: Int = 300,
    val customMaxDimension: Int = 1280,
    val customQuality: Int = 80
)

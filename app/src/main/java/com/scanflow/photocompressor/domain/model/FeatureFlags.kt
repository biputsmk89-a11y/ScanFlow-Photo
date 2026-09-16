package com.scanflow.photocompressor.domain.model

/**
 * 106. FEATURE FLAGS
 * Centralized feature flags for controlling advanced functionality.
 * Allows toggling unfinished or experimental features safely without leaving dead code.
 */
object FeatureFlags {
    /**
     * Controls multi-image PDF generation tool.
     */
    const val ENABLE_PDF: Boolean = true

    /**
     * Controls passport & ID photo cropping/print layouts.
     */
    const val ENABLE_PASSPORT: Boolean = true

    /**
     * Controls social media preset optimization (IG, FB, TikTok, YouTube).
     */
    const val ENABLE_SOCIAL: Boolean = true

    /**
     * Controls WhatsApp status and chat image optimization.
     */
    const val ENABLE_WHATSAPP: Boolean = true
}

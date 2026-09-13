package com.scanflow.photocompressor.domain.account

/**
 * Architectural contract establishing that ScanFlow Photo requires NO ACCOUNT
 * for any core application features.
 *
 * All photo processing (compression, resizing, cropping, converting, rotating, watermarking,
 * PDF generation, passport photo creation, social media export, and WhatsApp optimization)
 * functions 100% locally and privately on the device.
 *
 * An account is STRICTLY OPT-IN and ONLY required if future optional cloud features are enabled:
 * - Cloud Sync
 * - Cloud Backup
 * - AI Cloud Services
 * - Multi-Device Sync
 */
interface AccountContract {
    /**
     * Always returns false for core application workflows.
     * Core application never presents a mandatory login wall or blocks processing.
     */
    val isLoginRequiredForCore: Boolean get() = false

    /**
     * Future cloud features that require authentication.
     */
    enum class CloudServiceRequirement(val serviceName: String) {
        CLOUD_SYNC("Cloud Sync"),
        CLOUD_BACKUP("Cloud Backup"),
        AI_CLOUD_SERVICES("AI Cloud Services"),
        MULTI_DEVICE_SYNC("Multi-Device Sync")
    }

    /**
     * Checks whether an account is required for a given service.
     */
    fun isAccountRequiredFor(service: CloudServiceRequirement): Boolean = true
}

/**
 * Default implementation of [AccountContract] asserting no login is required for core app.
 */
class DefaultAccountContract : AccountContract

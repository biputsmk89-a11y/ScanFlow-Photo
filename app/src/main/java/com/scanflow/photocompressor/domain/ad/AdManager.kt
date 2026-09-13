package com.scanflow.photocompressor.domain.ad

import android.app.Activity

/**
 * 74. ADMOB POLICY
 * Contexts defining where an ad can potentially be requested.
 */
enum class AdPlacementContext {
    MAIN_SCREEN_RETURN,
    HISTORY_VIEW,
    DASHBOARD_IDLE,

    // STRICTLY FORBIDDEN CONTEXTS (Never show interstitial here):
    PROCESSING,
    CROP,
    SAVE
}

/**
 * Clean AdManager abstraction respecting user experience and Pro entitlements.
 */
interface AdManager {
    /**
     * False for Pro users (PRO = No Ads).
     */
    val isAdEnabled: Boolean

    /**
     * Checks whether an interstitial ad is permitted at the specified placement.
     * Guaranteed to return false during processing, crop, or save.
     */
    fun canShowInterstitialAt(placement: AdPlacementContext): Boolean

    /**
     * Shows an interstitial if allowed by policy, and invokes [onDismiss] upon completion
     * or immediate dismissal. Never blocks or crashes the main workflow.
     */
    fun showInterstitialIfAllowed(
        placement: AdPlacementContext,
        activity: Activity,
        onDismiss: () -> Unit
    )
}

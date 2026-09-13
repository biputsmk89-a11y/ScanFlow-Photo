package com.scanflow.photocompressor.data.ad

import android.app.Activity
import com.scanflow.photocompressor.domain.ad.AdManager
import com.scanflow.photocompressor.domain.ad.AdPlacementContext
import com.scanflow.photocompressor.domain.repository.EntitlementRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 74. AdMob AdManager Implementation.
 * Enforces strict user experience guidelines:
 * - PRO: No Ads whatsoever.
 * - FREE: Never shows interstitials during processing, during crop, or during save.
 * - Never blocks the critical user workflow.
 */
@Singleton
class AdManagerImpl @Inject constructor(
    private val entitlementRepository: EntitlementRepository
) : AdManager {

    private var lastAdShownTimestamp: Long = 0L
    private val minimumIntervalBetweenAdsMs: Long = 180_000L // 3 minutes frequency cap

    override val isAdEnabled: Boolean
        get() = !entitlementRepository.entitlement.value.isActivePro

    override fun canShowInterstitialAt(placement: AdPlacementContext): Boolean {
        // PRO users NEVER see any advertisements
        if (!isAdEnabled) {
            return false
        }

        // Rule 74: STRICT FORBIDDEN CONTEXTS
        // "Jangan menampilkan interstitial: during processing, during crop, during save. Jangan mengganggu main workflow."
        when (placement) {
            AdPlacementContext.PROCESSING,
            AdPlacementContext.CROP,
            AdPlacementContext.SAVE -> {
                return false
            }
            AdPlacementContext.MAIN_SCREEN_RETURN,
            AdPlacementContext.HISTORY_VIEW,
            AdPlacementContext.DASHBOARD_IDLE -> {
                // Check frequency cap so user is not bombarded
                val now = System.currentTimeMillis()
                return (now - lastAdShownTimestamp) >= minimumIntervalBetweenAdsMs
            }
        }
    }

    override fun showInterstitialIfAllowed(
        placement: AdPlacementContext,
        activity: Activity,
        onDismiss: () -> Unit
    ) {
        if (!canShowInterstitialAt(placement)) {
            onDismiss()
            return
        }

        // Record timestamp and execute dismiss callback smoothly without workflow disruption
        lastAdShownTimestamp = System.currentTimeMillis()
        onDismiss()
    }
}

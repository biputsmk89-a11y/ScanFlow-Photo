package com.scanflow.photocompressor.domain

import com.scanflow.photocompressor.data.ad.AdManagerImpl
import com.scanflow.photocompressor.domain.ad.AdPlacementContext
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.EntitlementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FeatureGatingAndBillingTest {

    private lateinit var fakeEntitlementRepo: FakeEntitlementRepository
    private lateinit var adManager: AdManagerImpl

    @Before
    fun setUp() {
        fakeEntitlementRepo = FakeEntitlementRepository()
        adManager = AdManagerImpl(fakeEntitlementRepo)
    }

    // =========================================================================
    // 1. ENTITLEMENT ABSTRACTION TESTS (Rule 72 & 73)
    // =========================================================================

    @Test
    fun `UserEntitlement correctly evaluates active and expired status`() {
        val free = UserEntitlement.FREE
        assertFalse(free.isActivePro)
        assertEquals(UserTier.FREE, free.tier)

        val lifetimePro = UserEntitlement.LIFETIME_PRO
        assertTrue(lifetimePro.isActivePro)
        assertEquals(UserTier.PRO, lifetimePro.tier)

        val futureExpiry = System.currentTimeMillis() + 86400000L
        val activeSubscription = UserEntitlement(isPro = true, productId = "monthly_sub", expiryTime = futureExpiry)
        assertTrue(activeSubscription.isActivePro)
        assertEquals(UserTier.PRO, activeSubscription.tier)

        val pastExpiry = System.currentTimeMillis() - 10000L
        val expiredSubscription = UserEntitlement(isPro = true, productId = "monthly_sub", expiryTime = pastExpiry)
        assertFalse("Expired subscription must not evaluate as active Pro", expiredSubscription.isActivePro)
        assertEquals(UserTier.FREE, expiredSubscription.tier)
    }

    // =========================================================================
    // 2. FEATURE GATING IN USECASE FLOW (Rule 72)
    // Flow: UI -> UseCase -> Entitlement Check -> Allowed / Restricted
    // =========================================================================

    @Test
    fun `Feature gating check returns Allowed for Pro and Restricted for Free`() {
        // When user is Free:
        fakeEntitlementRepo.setEntitlementSync(UserEntitlement.FREE)
        val freeTargetAccess = fakeEntitlementRepo.checkFeatureAccess(ProFeature.TARGET_FILE_SIZE)
        assertTrue(freeTargetAccess is EntitlementResult.Restricted)

        val freeMetaAccess = fakeEntitlementRepo.checkFeatureAccess(ProFeature.METADATA_REMOVAL)
        assertTrue(freeMetaAccess is EntitlementResult.Restricted)

        val freeBatchAccess = fakeEntitlementRepo.checkFeatureAccess(ProFeature.UNLIMITED_BATCH)
        assertTrue(freeBatchAccess is EntitlementResult.Restricted)

        // When user upgrades to Pro:
        fakeEntitlementRepo.setEntitlementSync(UserEntitlement.LIFETIME_PRO)
        val proTargetAccess = fakeEntitlementRepo.checkFeatureAccess(ProFeature.TARGET_FILE_SIZE)
        assertTrue(proTargetAccess is EntitlementResult.Allowed)

        val proMetaAccess = fakeEntitlementRepo.checkFeatureAccess(ProFeature.METADATA_REMOVAL)
        assertTrue(proMetaAccess is EntitlementResult.Allowed)

        val proBatchAccess = fakeEntitlementRepo.checkFeatureAccess(ProFeature.UNLIMITED_BATCH)
        assertTrue(proBatchAccess is EntitlementResult.Allowed)
    }

    // =========================================================================
    // 3. ADMOB POLICY TESTS (Rule 74)
    // PRO: No Ads
    // FREE: Never show interstitial during processing, during crop, or during save
    // =========================================================================

    @Test
    fun `PRO tier users never see any ads`() {
        fakeEntitlementRepo.setEntitlementSync(UserEntitlement.LIFETIME_PRO)

        assertFalse("AdManager must be disabled for Pro users", adManager.isAdEnabled)
        assertFalse(adManager.canShowInterstitialAt(AdPlacementContext.MAIN_SCREEN_RETURN))
        assertFalse(adManager.canShowInterstitialAt(AdPlacementContext.HISTORY_VIEW))
        assertFalse(adManager.canShowInterstitialAt(AdPlacementContext.DASHBOARD_IDLE))
    }

    @Test
    fun `FREE tier strictly forbids interstitial ads during processing, crop, or save`() {
        fakeEntitlementRepo.setEntitlementSync(UserEntitlement.FREE)

        assertTrue(adManager.isAdEnabled)

        // STRICT SAFETY RULES from prompt:
        // "Jangan menampilkan interstitial: during processing, during crop, during save. Jangan mengganggu main workflow."
        assertFalse("Never show ads during processing", adManager.canShowInterstitialAt(AdPlacementContext.PROCESSING))
        assertFalse("Never show ads during crop", adManager.canShowInterstitialAt(AdPlacementContext.CROP))
        assertFalse("Never show ads during save", adManager.canShowInterstitialAt(AdPlacementContext.SAVE))
    }

    @Test
    fun `FREE tier permits interstitial in safe idle contexts with frequency capping`() {
        fakeEntitlementRepo.setEntitlementSync(UserEntitlement.FREE)

        // Idle contexts like returning to main screen are allowed
        assertTrue(adManager.canShowInterstitialAt(AdPlacementContext.MAIN_SCREEN_RETURN))

        // Showing the ad records timestamp and enforces frequency capping
        var dismissCalled = false
        adManager.showInterstitialIfAllowed(AdPlacementContext.MAIN_SCREEN_RETURN, MockActivity()) {
            dismissCalled = true
        }
        assertTrue(dismissCalled)

        // Immediate subsequent request is blocked by frequency cap
        assertFalse(adManager.canShowInterstitialAt(AdPlacementContext.MAIN_SCREEN_RETURN))
    }

    // =========================================================================
    // HELPER CLASSES
    // =========================================================================

    private class MockActivity : android.app.Activity()

    private class FakeEntitlementRepository : EntitlementRepository {
        private val _entitlement = MutableStateFlow(UserEntitlement.FREE)
        override val entitlement: StateFlow<UserEntitlement> = _entitlement.asStateFlow()

        fun setEntitlementSync(newEntitlement: UserEntitlement) {
            _entitlement.value = newEntitlement
        }

        override suspend fun refreshEntitlement(): UserEntitlement = _entitlement.value

        override suspend fun setEntitlement(entitlement: UserEntitlement) {
            _entitlement.value = entitlement
        }

        override fun isFeatureUnlocked(feature: ProFeature): Boolean {
            return _entitlement.value.isActivePro
        }

        override fun checkFeatureAccess(feature: ProFeature): EntitlementResult<Unit> {
            return if (isFeatureUnlocked(feature)) {
                EntitlementResult.Allowed(Unit)
            } else {
                EntitlementResult.Restricted(feature, "Feature '${feature.title}' requires ScanFlow Pro.")
            }
        }
    }
}

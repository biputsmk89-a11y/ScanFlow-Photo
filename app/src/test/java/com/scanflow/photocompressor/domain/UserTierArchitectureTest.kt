package com.scanflow.photocompressor.domain

import com.scanflow.photocompressor.domain.model.ProFeature
import com.scanflow.photocompressor.domain.model.UserTier
import com.scanflow.photocompressor.domain.repository.UserTierRepository
import com.scanflow.photocompressor.domain.usecase.CheckProFeatureUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UserTierArchitectureTest {

    private lateinit var mockRepository: FakeUserTierRepository
    private lateinit var useCase: CheckProFeatureUseCase

    @Before
    fun setUp() {
        mockRepository = FakeUserTierRepository()
        useCase = CheckProFeatureUseCase(mockRepository)
    }

    @Test
    fun `default user tier is Free and Pro features are locked`() = runBlocking {
        assertEquals(UserTier.FREE, mockRepository.currentTierValue)

        // Free tier cannot access Pro features
        assertFalse(useCase.canAccess(ProFeature.UNLIMITED_BATCH))
        assertFalse(useCase.canAccess(ProFeature.TARGET_FILE_SIZE))
        assertFalse(useCase.canAccess(ProFeature.ADVANCED_PRESETS))
        assertFalse(useCase.canAccess(ProFeature.METADATA_REMOVAL))
        assertFalse(useCase.canAccess(ProFeature.PASSPORT_TOOL))
    }

    @Test
    fun `upgrading to Pro unlocks all Pro features`() = runBlocking {
        useCase.upgradeToPro()

        assertEquals(UserTier.PRO, mockRepository.currentTierValue)

        // Pro tier can access all Pro features
        assertTrue(useCase.canAccess(ProFeature.UNLIMITED_BATCH))
        assertTrue(useCase.canAccess(ProFeature.TARGET_FILE_SIZE))
        assertTrue(useCase.canAccess(ProFeature.ADVANCED_PRESETS))
        assertTrue(useCase.canAccess(ProFeature.METADATA_REMOVAL))
        assertTrue(useCase.canAccess(ProFeature.PASSPORT_TOOL))
        assertTrue(useCase.canAccess(ProFeature.SOCIAL_PRESETS))
        assertTrue(useCase.canAccess(ProFeature.WHATSAPP_PRESETS))
        assertTrue(useCase.canAccess(ProFeature.ADVANCED_PDF))
        assertTrue(useCase.canAccess(ProFeature.NO_ADS))
        assertTrue(useCase.canAccess(ProFeature.ADVANCED_CONTROLS))
    }

    @Test
    fun `downgrading locks Pro features again`() = runBlocking {
        useCase.upgradeToPro()
        assertTrue(useCase.canAccess(ProFeature.UNLIMITED_BATCH))

        useCase.downgradeToFree()
        assertFalse(useCase.canAccess(ProFeature.UNLIMITED_BATCH))
    }

    private class FakeUserTierRepository : UserTierRepository {
        private val _tier = MutableStateFlow(UserTier.FREE)
        val currentTierValue: UserTier get() = _tier.value

        override val currentTier: Flow<UserTier> = _tier.asStateFlow()

        override suspend fun setUserTier(tier: UserTier) {
            _tier.value = tier
        }

        override fun isFeatureAvailable(feature: ProFeature, currentTier: UserTier): Boolean {
            return currentTier == UserTier.PRO
        }

        override suspend fun isPro(): Boolean {
            return _tier.value == UserTier.PRO
        }
    }
}

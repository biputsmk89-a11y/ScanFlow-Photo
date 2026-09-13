package com.scanflow.photocompressor.domain.usecase

import com.scanflow.photocompressor.domain.model.ProFeature
import com.scanflow.photocompressor.domain.model.UserTier
import com.scanflow.photocompressor.domain.repository.UserTierRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CheckProFeatureUseCase @Inject constructor(
    private val userTierRepository: UserTierRepository
) {
    val currentTier: Flow<UserTier> = userTierRepository.currentTier
    val isPro: Flow<Boolean> = currentTier.map { it == UserTier.PRO }

    suspend fun canAccess(feature: ProFeature): Boolean {
        val tier = currentTier.first()
        return userTierRepository.isFeatureAvailable(feature, tier)
    }

    suspend fun upgradeToPro() {
        userTierRepository.setUserTier(UserTier.PRO)
    }

    suspend fun downgradeToFree() {
        userTierRepository.setUserTier(UserTier.FREE)
    }
}

package com.scanflow.photocompressor.domain.repository

import com.scanflow.photocompressor.domain.model.ProFeature
import com.scanflow.photocompressor.domain.model.UserTier
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining access and entitlement state for Free and Pro tiers.
 */
interface UserTierRepository {
    val currentTier: Flow<UserTier>
    suspend fun setUserTier(tier: UserTier)
    fun isFeatureAvailable(feature: ProFeature, currentTier: UserTier): Boolean
    suspend fun isPro(): Boolean
}

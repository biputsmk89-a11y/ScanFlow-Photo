package com.scanflow.photocompressor.domain.repository

import com.scanflow.photocompressor.domain.model.EntitlementResult
import com.scanflow.photocompressor.domain.model.ProFeature
import com.scanflow.photocompressor.domain.model.UserEntitlement
import kotlinx.coroutines.flow.StateFlow

/**
 * 72 & 73. Entitlement Repository Abstraction
 * Manages user entitlements, caching, and feature gating checks.
 */
interface EntitlementRepository {
    val entitlement: StateFlow<UserEntitlement>

    suspend fun refreshEntitlement(): UserEntitlement
    suspend fun setEntitlement(entitlement: UserEntitlement)
    fun isFeatureUnlocked(feature: ProFeature): Boolean
    fun checkFeatureAccess(feature: ProFeature): EntitlementResult<Unit>
}

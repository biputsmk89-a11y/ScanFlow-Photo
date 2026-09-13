package com.scanflow.photocompressor.domain.model

/**
 * 72. FEATURE GATING
 * Entitlement abstraction representing the user's active license state.
 * Never locks the architecture to a primitive boolean.
 */
data class UserEntitlement(
    val isPro: Boolean,
    val productId: String? = null,
    val expiryTime: Long? = null
) {
    val isActivePro: Boolean
        get() = isPro && (expiryTime == null || expiryTime > System.currentTimeMillis())

    val tier: UserTier
        get() = if (isActivePro) UserTier.PRO else UserTier.FREE

    companion object {
        val FREE = UserEntitlement(isPro = false)
        val LIFETIME_PRO = UserEntitlement(isPro = true, productId = "scanflow_pro_lifetime", expiryTime = null)
    }
}

/**
 * 73. BILLING
 * Purchase state abstraction for subscription and in-app purchases.
 */
sealed interface PurchaseState {
    data object Idle : PurchaseState
    data object Pending : PurchaseState
    data class Success(
        val orderId: String,
        val productId: String,
        val purchaseToken: String,
        val isAcknowledged: Boolean
    ) : PurchaseState
    data class Error(val code: Int, val message: String) : PurchaseState
    data object Restored : PurchaseState
}

/**
 * Result of a feature access check at UseCase level.
 * Flow: UI -> UseCase -> Entitlement Check -> Allowed / Restricted
 */
sealed interface EntitlementResult<out T> {
    data class Allowed<T>(val data: T) : EntitlementResult<T>
    data class Restricted(val requiredFeature: ProFeature, val message: String) : EntitlementResult<Nothing>

    val isAllowed: Boolean get() = this is Allowed
}

/**
 * Strongly-typed exception thrown when a restricted Pro feature is requested directly.
 */
class FeatureRestrictedException(
    val requiredFeature: ProFeature,
    override val message: String = "Feature '${requiredFeature.title}' requires ScanFlow Pro."
) : IllegalStateException(message)

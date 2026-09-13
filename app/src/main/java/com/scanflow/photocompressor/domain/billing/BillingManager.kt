package com.scanflow.photocompressor.domain.billing

import android.app.Activity
import com.scanflow.photocompressor.domain.model.PurchaseState
import com.scanflow.photocompressor.domain.model.UserEntitlement
import kotlinx.coroutines.flow.StateFlow

/**
 * 73. Server-side verification abstraction.
 * Ready for production backend verification without storing billing secrets or keys in client source code.
 */
interface ServerVerificationProvider {
    suspend fun verifyPurchase(
        orderId: String,
        purchaseToken: String,
        productId: String
    ): Result<UserEntitlement>
}

/**
 * 73. Clean BillingManager abstraction decoupling payment providers
 * (Google Play Billing, RevenueCat, etc.) from the application core.
 */
interface BillingManager {
    val purchaseState: StateFlow<PurchaseState>

    suspend fun startConnection(): Result<Unit>
    suspend fun launchBillingFlow(activity: Activity, productId: String): Result<Unit>
    suspend fun restorePurchases(): Result<UserEntitlement>
    fun endConnection()
}

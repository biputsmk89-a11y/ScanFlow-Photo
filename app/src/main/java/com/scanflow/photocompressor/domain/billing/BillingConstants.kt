package com.scanflow.photocompressor.domain.billing

/**
 * Standard Google Play Billing Product IDs and SKU definitions for ScanFlow Photo.
 * These IDs must match the In-App Products and Subscriptions configured in Google Play Console.
 */
object BillingConstants {
    // One-time In-App Purchase (Lifetime License)
    const val SKU_PRO_LIFETIME = "scanflow_pro_lifetime"

    // Recurring Subscription (Monthly Pro)
    const val SKU_PRO_MONTHLY = "scanflow_pro_monthly"

    // Base Plan ID for subscriptions in Play Console
    const val BASE_PLAN_MONTHLY = "monthly-plan"

    // All active product IDs
    val ALL_PRODUCT_IDS = listOf(
        SKU_PRO_LIFETIME,
        SKU_PRO_MONTHLY
    )
}

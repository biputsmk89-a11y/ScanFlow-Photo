package com.scanflow.photocompressor.engine

import android.app.ActivityManager
import android.content.Context

/**
 * OPTIONAL ADAPTIVE CONCURRENCY SPECIFICATION:
 *
 * Principles:
 * - "Jangan implementasikan sebelum basic batch stabil."
 * - "low-memory device = 1"
 * - "normal device = 1–2"
 * - "Namun stability selalu lebih penting daripada speed."
 *
 * Defaults strictly to 1 for guaranteed stability.
 */
object AdaptiveConcurrencyPolicy {

    const val DEFAULT_CONCURRENCY = 1
    const val MAX_CONCURRENCY = 2

    /**
     * Resolves concurrency under the stability-first rule.
     * Unless adaptive concurrency is explicitly enabled and basic batch is verified stable,
     * this always returns 1.
     */
    fun resolveConcurrency(
        context: Context?,
        allowAdaptive: Boolean = false
    ): Int {
        if (!allowAdaptive || context == null) {
            return DEFAULT_CONCURRENCY
        }

        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val isLowRam = activityManager?.isLowRamDevice ?: false

            if (isLowRam) {
                1 // low-memory device = 1
            } else {
                val availableHeapMb = LargeImageStrategy.getAvailableHeapMemory() / LargeImageStrategy.ONE_MB
                if (availableHeapMb >= 128) 2 else 1 // normal device = 1–2
            }
        } catch (_: Exception) {
            DEFAULT_CONCURRENCY
        }
    }
}

package com.scanflow.photocompressor.domain.repository

import com.scanflow.photocompressor.domain.model.OnboardingState
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing the persistent first-install onboarding state.
 */
interface OnboardingRepository {
    /**
     * Observable stream of the onboarding completion state.
     */
    val onboardingStateFlow: Flow<OnboardingState>

    /**
     * Checks whether the user has already completed onboarding.
     */
    suspend fun hasCompletedOnboarding(): Boolean

    /**
     * Marks the onboarding as completed permanently in DataStore.
     */
    suspend fun completeOnboarding()

    /**
     * Resets the onboarding state for developer / debug testing.
     * Must only be operable in DEBUG mode.
     */
    suspend fun resetOnboardingForDebug()
}

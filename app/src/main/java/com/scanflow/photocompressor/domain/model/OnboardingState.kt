package com.scanflow.photocompressor.domain.model

/**
 * State model representing the status of the first-install onboarding.
 */
data class OnboardingState(
    val hasCompleted: Boolean = false
)

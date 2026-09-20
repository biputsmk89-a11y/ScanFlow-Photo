package com.scanflow.photocompressor.ui.onboarding

/**
 * UI State for the first-launch onboarding flow.
 */
data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 4,
    val isNavigatingToHome: Boolean = false
)

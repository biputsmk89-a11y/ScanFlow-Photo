package com.scanflow.photocompressor.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

sealed interface OnboardingEvent {
    object NavigateToHome : OnboardingEvent
    data class ScrollToPage(val page: Int) : OnboardingEvent
}

/**
 * ViewModel managing the first-install onboarding state and safe navigation.
 * Guarantees atomic completion write to DataStore and single-dispatch navigation.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    private val isCompleting = AtomicBoolean(false)

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page.coerceIn(0, it.totalPages - 1)) }
    }

    fun onNextClicked() {
        val current = _uiState.value.currentPage
        val total = _uiState.value.totalPages
        if (current < total - 1) {
            val nextPage = current + 1
            _uiState.update { it.copy(currentPage = nextPage) }
            _events.tryEmit(OnboardingEvent.ScrollToPage(nextPage))
        } else {
            completeOnboarding()
        }
    }

    fun onPreviousClicked() {
        val current = _uiState.value.currentPage
        if (current > 0) {
            val prevPage = current - 1
            _uiState.update { it.copy(currentPage = prevPage) }
            _events.tryEmit(OnboardingEvent.ScrollToPage(prevPage))
        }
    }

    fun onSkipClicked() {
        completeOnboarding()
    }

    fun completeOnboarding() {
        if (isCompleting.compareAndSet(false, true)) {
            _uiState.update { it.copy(isNavigatingToHome = true) }
            viewModelScope.launch {
                try {
                    onboardingRepository.completeOnboarding()
                } finally {
                    _events.emit(OnboardingEvent.NavigateToHome)
                }
            }
        }
    }
}

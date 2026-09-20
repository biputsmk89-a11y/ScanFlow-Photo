package com.scanflow.photocompressor.presentation

import com.scanflow.photocompressor.domain.repository.OnboardingRepository
import com.scanflow.photocompressor.ui.onboarding.OnboardingEvent
import com.scanflow.photocompressor.ui.onboarding.OnboardingViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private lateinit var viewModel: OnboardingViewModel
    private lateinit var onboardingRepository: OnboardingRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        onboardingRepository = mockk(relaxed = true)
        viewModel = OnboardingViewModel(onboardingRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has currentPage 0 and totalPages 4`() {
        val state = viewModel.uiState.value
        assertEquals(0, state.currentPage)
        assertEquals(4, state.totalPages)
        assertFalse(state.isNavigatingToHome)
    }

    @Test
    fun `onPageChanged updates currentPage within valid range`() {
        viewModel.onPageChanged(2)
        assertEquals(2, viewModel.uiState.value.currentPage)

        viewModel.onPageChanged(10) // out of bounds
        assertEquals(3, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `onNextClicked increments page when not on last page`() = runTest {
        viewModel.onNextClicked()
        assertEquals(1, viewModel.uiState.value.currentPage)

        viewModel.onNextClicked()
        assertEquals(2, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `onPreviousClicked decrements page when not on first page`() {
        viewModel.onPageChanged(2)
        viewModel.onPreviousClicked()
        assertEquals(1, viewModel.uiState.value.currentPage)

        viewModel.onPreviousClicked()
        assertEquals(0, viewModel.uiState.value.currentPage)

        viewModel.onPreviousClicked()
        assertEquals(0, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `onSkipClicked completes onboarding and emits NavigateToHome event`() = runTest {
        val events = mutableListOf<OnboardingEvent>()
        val job = launch {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.onSkipClicked()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { onboardingRepository.completeOnboarding() }
        assertTrue(events.contains(OnboardingEvent.NavigateToHome))
        assertTrue(viewModel.uiState.value.isNavigatingToHome)

        job.cancel()
    }

    @Test
    fun `completeOnboarding prevents duplicate execution on multi-clicks`() = runTest {
        viewModel.completeOnboarding()
        viewModel.completeOnboarding()
        viewModel.completeOnboarding()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { onboardingRepository.completeOnboarding() }
    }
}

package com.scanflow.photocompressor.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OnboardingRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var repository: OnboardingRepositoryImpl

    @Before
    fun setup() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val testFile = File(tempFolder.root, "test_onboarding_${System.nanoTime()}.preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { testFile }
        )
        repository = OnboardingRepositoryImpl(testDataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `initial onboarding state is not completed`() = runTest {
        val state = repository.onboardingStateFlow.first()
        assertFalse("Initial onboarding state must be false", state.hasCompleted)
        assertFalse("hasCompletedOnboarding() must return false initially", repository.hasCompletedOnboarding())
    }

    @Test
    fun `completeOnboarding persists true to DataStore`() = runTest {
        repository.completeOnboarding()

        val state = repository.onboardingStateFlow.first()
        assertTrue("Onboarding state must be completed after calling completeOnboarding", state.hasCompleted)
        assertTrue("hasCompletedOnboarding() must return true after calling completeOnboarding", repository.hasCompletedOnboarding())
    }

    @Test
    fun `resetOnboardingForDebug sets completion state to false`() = runTest {
        repository.resetOnboardingForDebug()
        val state = repository.onboardingStateFlow.first()
        assertFalse("State should be false after reset", state.hasCompleted)
    }
}

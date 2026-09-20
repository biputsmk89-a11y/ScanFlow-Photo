package com.scanflow.photocompressor.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.scanflow.photocompressor.BuildConfig
import com.scanflow.photocompressor.domain.model.OnboardingState
import com.scanflow.photocompressor.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore Preferences implementation of OnboardingRepository.
 * Manages the single-launch persistent completion flag (has_completed_onboarding).
 */
@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : OnboardingRepository {

    private object PreferencesKeys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    override val onboardingStateFlow: Flow<OnboardingState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val hasCompleted = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
            OnboardingState(hasCompleted = hasCompleted)
        }

    override suspend fun hasCompletedOnboarding(): Boolean {
        return try {
            val preferences = dataStore.data
                .catch { emit(emptyPreferences()) }
                .first()
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun completeOnboarding() {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = true
            }
        } catch (e: IOException) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = true
            }
        }
    }

    override suspend fun resetOnboardingForDebug() {
        if (BuildConfig.DEBUG) {
            try {
                dataStore.edit { preferences ->
                    preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = false
                }
            } catch (e: IOException) {
                dataStore.edit { preferences ->
                    preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = false
                }
            }
        }
    }
}

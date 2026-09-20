package com.scanflow.photocompressor.domain.repository

import com.scanflow.photocompressor.domain.model.AppPreferences
import com.scanflow.photocompressor.domain.model.ConflictStrategy
import com.scanflow.photocompressor.domain.model.DefaultBehavior
import com.scanflow.photocompressor.domain.model.FileNamingConfig
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing persistent application preferences using DataStore.
 */
interface PreferencesRepository {
    /**
     * Flow of user preferences.
     */
    val preferencesFlow: Flow<AppPreferences>

    /**
     * Update app theme (SYSTEM, LIGHT, DARK).
     */
    suspend fun setThemeMode(mode: ThemeMode)

    /**
     * Update default compression quality (1-100).
     */
    suspend fun setDefaultQuality(quality: Int)

    /**
     * Update default export image format.
     */
    suspend fun setDefaultFormat(format: ImageFormat)

    /**
     * Update default behavior settings.
     */
    suspend fun setDefaultBehavior(behavior: DefaultBehavior)

    /**
     * Update conflict resolution strategy.
     */
    suspend fun setConflictStrategy(strategy: ConflictStrategy)

    /**
     * Update file naming configuration.
     */
    suspend fun setFileNamingConfig(config: FileNamingConfig)

    /**
     * Reset all settings to application defaults.
     */
    suspend fun resetToDefaults()
}

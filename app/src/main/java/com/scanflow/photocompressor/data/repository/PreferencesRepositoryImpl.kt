package com.scanflow.photocompressor.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.scanflow.photocompressor.domain.model.AppPreferences
import com.scanflow.photocompressor.domain.model.ConflictStrategy
import com.scanflow.photocompressor.domain.model.DefaultBehavior
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ThemeMode
import com.scanflow.photocompressor.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore Preferences implementation of PreferencesRepository.
 * Manages theme, default quality, default format, and behavior options.
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_QUALITY = intPreferencesKey("default_quality")
        val DEFAULT_FORMAT = stringPreferencesKey("default_format")
        val PRESERVE_EXIF = booleanPreferencesKey("preserve_exif")
        val KEEP_ASPECT_RATIO = booleanPreferencesKey("keep_aspect_ratio")
        val AUTO_CLEAN_TEMP = booleanPreferencesKey("auto_clean_temp")
        val CONFLICT_STRATEGY = stringPreferencesKey("conflict_strategy")
    }

    override val preferencesFlow: Flow<AppPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)

            val quality = preferences[PreferencesKeys.DEFAULT_QUALITY] ?: 80

            val formatStr = preferences[PreferencesKeys.DEFAULT_FORMAT] ?: ImageFormat.JPEG.name
            val format = runCatching { ImageFormat.valueOf(formatStr) }.getOrDefault(ImageFormat.JPEG)

            val preserveExif = preferences[PreferencesKeys.PRESERVE_EXIF] ?: true
            val keepAspectRatio = preferences[PreferencesKeys.KEEP_ASPECT_RATIO] ?: true
            val autoCleanTemp = preferences[PreferencesKeys.AUTO_CLEAN_TEMP] ?: true

            val strategyStr = preferences[PreferencesKeys.CONFLICT_STRATEGY] ?: ConflictStrategy.INCREMENT.name
            val conflictStrategy = runCatching { ConflictStrategy.valueOf(strategyStr) }.getOrDefault(ConflictStrategy.INCREMENT)

            AppPreferences(
                theme = themeMode,
                defaultQuality = quality.coerceIn(1, 100),
                defaultFormat = format,
                behavior = DefaultBehavior(
                    preserveExif = preserveExif,
                    keepAspectRatio = keepAspectRatio,
                    autoCleanTemp = autoCleanTemp,
                    conflictStrategy = conflictStrategy
                )
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setDefaultQuality(quality: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_QUALITY] = quality.coerceIn(1, 100)
        }
    }

    override suspend fun setDefaultFormat(format: ImageFormat) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_FORMAT] = format.name
        }
    }

    override suspend fun setDefaultBehavior(behavior: DefaultBehavior) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRESERVE_EXIF] = behavior.preserveExif
            preferences[PreferencesKeys.KEEP_ASPECT_RATIO] = behavior.keepAspectRatio
            preferences[PreferencesKeys.AUTO_CLEAN_TEMP] = behavior.autoCleanTemp
            preferences[PreferencesKeys.CONFLICT_STRATEGY] = behavior.conflictStrategy.name
        }
    }

    override suspend fun setConflictStrategy(strategy: ConflictStrategy) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONFLICT_STRATEGY] = strategy.name
        }
    }

    override suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

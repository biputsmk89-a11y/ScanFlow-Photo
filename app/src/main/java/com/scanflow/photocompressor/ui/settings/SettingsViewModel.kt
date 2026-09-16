package com.scanflow.photocompressor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.model.AppPreferences
import com.scanflow.photocompressor.domain.model.ConflictStrategy
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ThemeMode
import com.scanflow.photocompressor.domain.billing.BillingConstants
import com.scanflow.photocompressor.domain.billing.BillingManager
import com.scanflow.photocompressor.domain.repository.PreferencesRepository
import com.scanflow.photocompressor.domain.repository.UserTierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val cacheSize: String = "0 B",
    val outputDirSize: String = "0 B",
    val outputFileCount: Int = 0,
    val availableStorage: String = "Calculating...",
    val outputDirectoryPath: String = "",
    val isPro: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val preferencesRepository: PreferencesRepository,
    private val userTierRepository: UserTierRepository,
    private val billingManager: BillingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshStorageStats()
        observePreferences()
        observeEntitlement()
    }

    private fun observeEntitlement() {
        viewModelScope.launch {
            userTierRepository.currentTier.collect { tier ->
                _uiState.update { it.copy(isPro = (tier == com.scanflow.photocompressor.domain.model.UserTier.PRO)) }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setDefaultQuality(quality: Int) {
        viewModelScope.launch {
            preferencesRepository.setDefaultQuality(quality)
        }
    }

    fun setDefaultFormat(format: ImageFormat) {
        viewModelScope.launch {
            preferencesRepository.setDefaultFormat(format)
        }
    }

    fun setPreserveExif(preserve: Boolean) {
        viewModelScope.launch {
            val currentBehavior = _uiState.value.preferences.behavior
            preferencesRepository.setDefaultBehavior(currentBehavior.copy(preserveExif = preserve))
        }
    }

    fun setKeepAspectRatio(keep: Boolean) {
        viewModelScope.launch {
            val currentBehavior = _uiState.value.preferences.behavior
            preferencesRepository.setDefaultBehavior(currentBehavior.copy(keepAspectRatio = keep))
        }
    }

    fun setConflictStrategy(strategy: ConflictStrategy) {
        viewModelScope.launch {
            preferencesRepository.setConflictStrategy(strategy)
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetToDefaults()
            _uiState.update { it.copy(message = "Settings reset to defaults") }
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val tempDir = fileManager.getTempDirectory()
            val tempSize = tempDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val outputSize = fileManager.getOutputDirectorySize()
            val fileCount = fileManager.getOutputFileCount()
            val available = fileManager.getAvailableStorage()
            val outPath = fileManager.getOutputDirectory().absolutePath

            _uiState.update {
                it.copy(
                    cacheSize = fileManager.formatFileSize(tempSize),
                    outputDirSize = fileManager.formatFileSize(outputSize),
                    outputFileCount = fileCount,
                    availableStorage = fileManager.formatFileSize(available),
                    outputDirectoryPath = outPath
                )
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            fileManager.cleanTempFiles()
            refreshStorageStats()
            _uiState.update { it.copy(message = "Cache cleared successfully") }
        }
    }



    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

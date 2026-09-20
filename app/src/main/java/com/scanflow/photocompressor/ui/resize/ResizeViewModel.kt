package com.scanflow.photocompressor.ui.resize

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.ResizeImageUseCase
import com.scanflow.photocompressor.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ResizeTab {
    PRESET,
    DIMENSIONS,
    PERCENTAGE,
    MAX_DIMENSION
}

data class ResizeUiState(
    val selectedImageUri: Uri? = null,
    val imageInfo: ImageInfo? = null,
    val activeTab: ResizeTab = ResizeTab.PRESET,
    val selectedPreset: ResizePreset = ResizePreset.P_1080,
    val maxDimensionInput: String = "1080",
    val targetWidth: String = "",
    val targetHeight: String = "",
    val lockAspectRatio: Boolean = true,
    val percentage: Float = 0.5f,
    val usePercentage: Boolean = false,
    val quality: Int = 90,
    val isProcessing: Boolean = false,
    val result: CompressionResult? = null,
    val error: String? = null
)

@HiltViewModel
class ResizeViewModel @Inject constructor(
    private val resizeImageUseCase: ResizeImageUseCase,
    private val imageRepository: ImageRepository,
    private val preferencesRepository: PreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResizeUiState())
    val uiState: StateFlow<ResizeUiState> = _uiState.asStateFlow()

    init {
        preferencesRepository?.let { repo ->
            viewModelScope.launch {
                repo.preferencesFlow.first().let { prefs ->
                    _uiState.update { current ->
                        current.copy(
                            quality = prefs.defaultQuality,
                            lockAspectRatio = prefs.behavior.keepAspectRatio
                        )
                    }
                }
            }
        }
    }

    fun selectImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val info = imageRepository.getImageInfo(uri)
                _uiState.update {
                    it.copy(
                        selectedImageUri = uri,
                        imageInfo = info,
                        targetWidth = info.width.toString(),
                        targetHeight = info.height.toString(),
                        result = null,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setActiveTab(tab: ResizeTab) {
        _uiState.update { it.copy(activeTab = tab, usePercentage = (tab == ResizeTab.PERCENTAGE)) }
    }

    fun setPreset(preset: ResizePreset) {
        _uiState.update { state ->
            val info = state.imageInfo
            if (preset == ResizePreset.CUSTOM || info == null) {
                state.copy(selectedPreset = preset)
            } else {
                val ratio = info.width.toFloat() / info.height.toFloat()
                val (w, h) = if (info.width >= info.height) {
                    val targetW = minOf(info.width, preset.dimension)
                    val targetH = (targetW / ratio).toInt().coerceAtLeast(1)
                    Pair(targetW, targetH)
                } else {
                    val targetH = minOf(info.height, preset.dimension)
                    val targetW = (targetH * ratio).toInt().coerceAtLeast(1)
                    Pair(targetW, targetH)
                }
                state.copy(
                    selectedPreset = preset,
                    targetWidth = w.toString(),
                    targetHeight = h.toString()
                )
            }
        }
    }

    fun setMaxDimensionInput(value: String) {
        _uiState.update { it.copy(maxDimensionInput = value) }
    }

    fun setTargetWidth(w: String) {
        _uiState.update { state ->
            if (state.lockAspectRatio && state.imageInfo != null && w.toIntOrNull() != null) {
                val ratio = state.imageInfo.height.toFloat() / state.imageInfo.width
                state.copy(targetWidth = w, targetHeight = (w.toInt() * ratio).toInt().toString(), selectedPreset = ResizePreset.CUSTOM)
            } else {
                state.copy(targetWidth = w, selectedPreset = ResizePreset.CUSTOM)
            }
        }
    }

    fun setTargetHeight(h: String) {
        _uiState.update { state ->
            if (state.lockAspectRatio && state.imageInfo != null && h.toIntOrNull() != null) {
                val ratio = state.imageInfo.width.toFloat() / state.imageInfo.height
                state.copy(targetHeight = h, targetWidth = (h.toInt() * ratio).toInt().toString(), selectedPreset = ResizePreset.CUSTOM)
            } else {
                state.copy(targetHeight = h, selectedPreset = ResizePreset.CUSTOM)
            }
        }
    }

    fun toggleAspectLock() { _uiState.update { it.copy(lockAspectRatio = !it.lockAspectRatio) } }
    fun setPercentage(p: Float) { _uiState.update { it.copy(percentage = p) } }
    fun togglePercentageMode() {
        _uiState.update {
            val newUsePct = !it.usePercentage
            it.copy(
                usePercentage = newUsePct,
                activeTab = if (newUsePct) ResizeTab.PERCENTAGE else ResizeTab.DIMENSIONS
            )
        }
    }
    fun setQuality(q: Int) { _uiState.update { it.copy(quality = q) } }

    fun resize() {
        val state = _uiState.value
        val uri = state.selectedImageUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            val result = when (state.activeTab) {
                ResizeTab.PRESET -> {
                    if (state.selectedPreset == ResizePreset.CUSTOM) {
                        resizeImageUseCase(
                            inputUri = uri,
                            targetWidth = state.targetWidth.toIntOrNull() ?: 0,
                            targetHeight = state.targetHeight.toIntOrNull() ?: 0,
                            maintainAspectRatio = state.lockAspectRatio,
                            quality = state.quality
                        )
                    } else {
                        resizeImageUseCase(
                            inputUri = uri,
                            preset = state.selectedPreset,
                            maintainAspectRatio = state.lockAspectRatio,
                            quality = state.quality
                        )
                    }
                }
                ResizeTab.DIMENSIONS -> {
                    resizeImageUseCase(
                        inputUri = uri,
                        targetWidth = state.targetWidth.toIntOrNull() ?: 0,
                        targetHeight = state.targetHeight.toIntOrNull() ?: 0,
                        maintainAspectRatio = state.lockAspectRatio,
                        quality = state.quality
                    )
                }
                ResizeTab.PERCENTAGE -> {
                    resizeImageUseCase(
                        inputUri = uri,
                        percentage = state.percentage,
                        quality = state.quality
                    )
                }
                ResizeTab.MAX_DIMENSION -> {
                    val maxDim = state.maxDimensionInput.toIntOrNull() ?: 1080
                    resizeImageUseCase(
                        inputUri = uri,
                        maxDimension = maxDim,
                        maintainAspectRatio = state.lockAspectRatio,
                        quality = state.quality
                    )
                }
            }
            result.fold(
                onSuccess = { r -> _uiState.update { it.copy(isProcessing = false, result = r) } },
                onFailure = { e -> _uiState.update { it.copy(isProcessing = false, error = e.message) } }
            )
        }
    }

    fun reset() { _uiState.value = ResizeUiState() }
}

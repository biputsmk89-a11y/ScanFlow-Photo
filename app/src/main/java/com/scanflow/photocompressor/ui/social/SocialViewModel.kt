package com.scanflow.photocompressor.ui.social

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.engine.SocialMediaEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialUiState(
    val selectedImageUri: Uri? = null,
    val selectedPlatform: SocialPlatform = SocialPlatform.INSTAGRAM,
    val selectedType: SocialContentType = SocialContentType.POST,
    val fittingMode: SocialFittingMode = SocialFittingMode.FIT,
    val zoom: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val quality: Int = 85,
    val isProcessing: Boolean = false,
    val result: CompressionResult? = null,
    val errorMessage: String? = null
) {
    val currentPreset: InternalSocialPreset
        get() = SocialPresetRegistry.getPreset(selectedPlatform, selectedType)

    val targetAspectRatio: Float
        get() = currentPreset.ratioX.toFloat() / currentPreset.ratioY.toFloat()
}

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val socialMediaEngine: SocialMediaEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    fun selectImage(uri: Uri) {
        _uiState.update {
            it.copy(selectedImageUri = uri, zoom = 1.0f, panX = 0f, panY = 0f, result = null, errorMessage = null)
        }
    }

    fun selectPlatform(platform: SocialPlatform) {
        val preset = SocialPresetRegistry.getPreset(platform, _uiState.value.selectedType)
        _uiState.update {
            it.copy(
                selectedPlatform = platform,
                quality = preset.recommendedQuality,
                result = null
            )
        }
    }

    fun selectType(type: SocialContentType) {
        val preset = SocialPresetRegistry.getPreset(_uiState.value.selectedPlatform, type)
        _uiState.update {
            it.copy(
                selectedType = type,
                quality = preset.recommendedQuality,
                result = null
            )
        }
    }

    fun setFittingMode(mode: SocialFittingMode) {
        _uiState.update { it.copy(fittingMode = mode, zoom = 1.0f, panX = 0f, panY = 0f, result = null) }
    }

    fun updateTransform(zoomDelta: Float, panXDelta: Float, panYDelta: Float) {
        _uiState.update {
            val newZoom = (it.zoom * zoomDelta).coerceIn(1.0f, 3.0f)
            val newPanX = (it.panX + panXDelta).coerceIn(-600f, 600f)
            val newPanY = (it.panY + panYDelta).coerceIn(-600f, 600f)
            it.copy(zoom = newZoom, panX = newPanX, panY = newPanY, result = null)
        }
    }

    fun resetTransform() {
        _uiState.update { it.copy(zoom = 1.0f, panX = 0f, panY = 0f) }
    }

    fun updateQuality(quality: Int) {
        _uiState.update { it.copy(quality = quality, result = null) }
    }

    fun processSocialImage(customCropRegion: CropRegion? = null) {
        val uri = _uiState.value.selectedImageUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val result = socialMediaEngine.processSocialMediaImage(
                sourceUri = uri,
                platform = _uiState.value.selectedPlatform,
                type = _uiState.value.selectedType,
                quality = _uiState.value.quality,
                fittingMode = _uiState.value.fittingMode,
                customCropRegion = customCropRegion
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isProcessing = false, result = result.getOrNull()) }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to process social media image"
                    )
                }
            }
        }
    }
}

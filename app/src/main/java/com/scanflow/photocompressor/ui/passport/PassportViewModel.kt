package com.scanflow.photocompressor.ui.passport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.engine.PassportEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PassportUiState(
    val selectedImageUri: Uri? = null,
    val config: PassportConfig = PassportConfig(),
    val isProcessing: Boolean = false,
    val result: CompressionResult? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PassportViewModel @Inject constructor(
    private val passportEngine: PassportEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassportUiState())
    val uiState: StateFlow<PassportUiState> = _uiState.asStateFlow()

    fun selectImage(uri: Uri) {
        _uiState.update {
            it.copy(selectedImageUri = uri, result = null, errorMessage = null)
        }
    }

    fun updateSpec(spec: PassportSpec) {
        _uiState.update { it.copy(config = it.config.copy(spec = spec), result = null) }
    }

    fun updateBackground(background: PassportBackground) {
        _uiState.update { it.copy(config = it.config.copy(background = background), result = null) }
    }

    fun updatePrintLayout(layout: PassportPrintLayout) {
        _uiState.update { it.copy(config = it.config.copy(printLayout = layout), result = null) }
    }

    fun updateFrameStyle(style: IdFrameStyle) {
        _uiState.update { it.copy(config = it.config.copy(frameStyle = style), result = null) }
    }

    fun updateZoom(zoom: Float) {
        _uiState.update { it.copy(config = it.config.copy(zoom = zoom.coerceIn(0.5f, 3.0f)), result = null) }
    }

    fun updatePan(panX: Float, panY: Float) {
        _uiState.update { it.copy(config = it.config.copy(panX = panX, panY = panY), result = null) }
    }

    fun updateRotation(degrees: Float) {
        _uiState.update { it.copy(config = it.config.copy(rotationDegrees = degrees), result = null) }
    }

    fun autoCenter() {
        _uiState.update {
            it.copy(
                config = it.config.copy(panX = 0f, panY = 0f),
                result = null
            )
        }
    }

    fun resetPosition() {
        _uiState.update {
            it.copy(
                config = it.config.copy(panX = 0f, panY = 0f, zoom = 1.0f, rotationDegrees = 0f),
                result = null
            )
        }
    }

    fun updateCustomBackgroundColor(colorInt: Int) {
        _uiState.update {
            it.copy(
                config = it.config.copy(
                    background = PassportBackground.CUSTOM,
                    customBackgroundColor = colorInt
                ),
                result = null
            )
        }
    }

    fun updateAddCutMarks(add: Boolean) {
        _uiState.update { it.copy(config = it.config.copy(addCutMarks = add), result = null) }
    }

    fun processPassport(customCropRegion: CropRegion? = null) {
        val uri = _uiState.value.selectedImageUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val result = passportEngine.processPassportPhoto(
                sourceUri = uri,
                config = _uiState.value.config,
                customCropRegion = customCropRegion
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isProcessing = false, result = result.getOrNull()) }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to process passport photo"
                    )
                }
            }
        }
    }
}

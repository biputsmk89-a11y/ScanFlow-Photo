package com.scanflow.photocompressor.ui.watermark

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.AddWatermarkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatermarkUiState(
    val selectedImageUri: Uri? = null,
    val imageInfo: ImageInfo? = null,
    val text: String = "© Photo",
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val opacity: Float = 0.5f,
    val fontSize: Float = 24f,
    val quality: Int = 90,
    val isProcessing: Boolean = false,
    val result: CompressionResult? = null,
    val error: String? = null
)

@HiltViewModel
class WatermarkViewModel @Inject constructor(
    private val addWatermarkUseCase: AddWatermarkUseCase,
    private val imageRepository: ImageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WatermarkUiState())
    val uiState: StateFlow<WatermarkUiState> = _uiState.asStateFlow()

    fun selectImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val info = imageRepository.getImageInfo(uri)
                _uiState.update { it.copy(selectedImageUri = uri, imageInfo = info, result = null, error = null) }
            } catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun setText(t: String) { _uiState.update { it.copy(text = t) } }
    fun setPosition(p: WatermarkPosition) { _uiState.update { it.copy(position = p) } }
    fun setOpacity(o: Float) { _uiState.update { it.copy(opacity = o) } }
    fun setFontSize(s: Float) { _uiState.update { it.copy(fontSize = s) } }

    fun apply() {
        val state = _uiState.value
        val uri = state.selectedImageUri ?: return
        if (state.text.isBlank()) { _uiState.update { it.copy(error = "Text cannot be empty") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            val config = WatermarkConfig(text = state.text, position = state.position, opacity = state.opacity, fontSize = state.fontSize)
            addWatermarkUseCase(uri, config, state.quality).fold(
                onSuccess = { r -> _uiState.update { it.copy(isProcessing = false, result = r) } },
                onFailure = { e -> _uiState.update { it.copy(isProcessing = false, error = e.message) } }
            )
        }
    }

    fun reset() { _uiState.value = WatermarkUiState() }
}

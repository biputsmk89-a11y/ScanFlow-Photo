package com.scanflow.photocompressor.ui.rotate

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.RotateImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RotateUiState(
    val selectedImageUri: Uri? = null,
    val imageInfo: ImageInfo? = null,
    val rotation: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val quality: Int = 90,
    val isProcessing: Boolean = false,
    val result: CompressionResult? = null,
    val error: String? = null
)

@HiltViewModel
class RotateViewModel @Inject constructor(
    private val rotateImageUseCase: RotateImageUseCase,
    private val imageRepository: ImageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RotateUiState())
    val uiState: StateFlow<RotateUiState> = _uiState.asStateFlow()

    fun selectImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val info = imageRepository.getImageInfo(uri)
                _uiState.update { it.copy(selectedImageUri = uri, imageInfo = info, result = null, error = null) }
            } catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun rotate90CW() { _uiState.update { it.copy(rotation = (it.rotation + 90f) % 360f) } }
    fun rotate90CCW() { _uiState.update { it.copy(rotation = (it.rotation + 270f) % 360f) } }
    fun setRotation(d: Float) { _uiState.update { it.copy(rotation = d) } }
    fun toggleFlipH() { _uiState.update { it.copy(flipH = !it.flipH) } }
    fun toggleFlipV() { _uiState.update { it.copy(flipV = !it.flipV) } }

    fun apply() {
        val state = _uiState.value
        val uri = state.selectedImageUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            rotateImageUseCase(uri, state.rotation, state.flipH, state.flipV, state.quality).fold(
                onSuccess = { r -> _uiState.update { it.copy(isProcessing = false, result = r) } },
                onFailure = { e -> _uiState.update { it.copy(isProcessing = false, error = e.message) } }
            )
        }
    }

    fun reset() { _uiState.value = RotateUiState() }
}

package com.scanflow.photocompressor.ui.convert

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.ConvertFormatUseCase
import com.scanflow.photocompressor.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BackgroundColorOption(val label: String, val colorValue: Int) {
    WHITE("White", android.graphics.Color.WHITE),
    BLACK("Black", android.graphics.Color.BLACK),
    LIGHT_GRAY("Light Gray", android.graphics.Color.LTGRAY)
}

data class ConvertUiState(
    val selectedImageUri: Uri? = null,
    val imageInfo: ImageInfo? = null,
    val targetFormat: ImageFormat = ImageFormat.JPEG,
    val quality: Int = 90,
    val selectedBgColor: BackgroundColorOption = BackgroundColorOption.WHITE,
    val isProcessing: Boolean = false,
    val result: CompressionResult? = null,
    val error: String? = null
) {
    val showTransparencyWarning: Boolean
        get() = (imageInfo?.hasAlpha == true) && targetFormat == ImageFormat.JPEG
}

@HiltViewModel
class ConvertViewModel @Inject constructor(
    private val convertFormatUseCase: ConvertFormatUseCase,
    private val imageRepository: ImageRepository,
    private val preferencesRepository: PreferencesRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConvertUiState())
    val uiState: StateFlow<ConvertUiState> = _uiState.asStateFlow()

    init {
        preferencesRepository?.let { repo ->
            viewModelScope.launch {
                repo.preferencesFlow.first().let { prefs ->
                    _uiState.update { current ->
                        current.copy(
                            targetFormat = prefs.defaultFormat,
                            quality = prefs.defaultQuality
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
                _uiState.update { it.copy(selectedImageUri = uri, imageInfo = info, result = null, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setFormat(f: ImageFormat) {
        _uiState.update { it.copy(targetFormat = f) }
    }

    fun setBackgroundColor(option: BackgroundColorOption) {
        _uiState.update { it.copy(selectedBgColor = option) }
    }

    fun setQuality(q: Int) {
        _uiState.update { it.copy(quality = q) }
    }

    fun convert() {
        val state = _uiState.value
        val uri = state.selectedImageUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            convertFormatUseCase(
                inputUri = uri,
                targetFormat = state.targetFormat,
                quality = state.quality,
                backgroundColor = state.selectedBgColor.colorValue
            ).fold(
                onSuccess = { r -> _uiState.update { it.copy(isProcessing = false, result = r) } },
                onFailure = { e -> _uiState.update { it.copy(isProcessing = false, error = e.message) } }
            )
        }
    }

    fun reset() {
        _uiState.value = ConvertUiState()
    }
}

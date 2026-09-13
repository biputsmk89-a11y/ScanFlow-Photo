package com.scanflow.photocompressor.ui.whatsapp

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.engine.WhatsAppEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WhatsAppUiState(
    val selectedImageUri: Uri? = null,
    val config: WhatsAppConfig = WhatsAppConfig(),
    val isProcessing: Boolean = false,
    val result: CompressionResult? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class WhatsAppViewModel @Inject constructor(
    private val whatsAppEngine: WhatsAppEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatsAppUiState())
    val uiState: StateFlow<WhatsAppUiState> = _uiState.asStateFlow()

    fun selectImage(uri: Uri) {
        _uiState.update {
            it.copy(selectedImageUri = uri, result = null, errorMessage = null)
        }
    }

    fun selectTier(tier: WhatsAppTier) {
        _uiState.update {
            it.copy(config = it.config.copy(tier = tier), result = null)
        }
    }

    fun updateCustomTargetSize(kb: Int) {
        _uiState.update {
            it.copy(config = it.config.copy(customTargetSizeKB = kb), result = null)
        }
    }

    fun updateCustomQuality(quality: Int) {
        _uiState.update {
            it.copy(config = it.config.copy(customQuality = quality), result = null)
        }
    }

    fun updateCustomMaxDimension(maxDim: Int) {
        _uiState.update {
            it.copy(config = it.config.copy(customMaxDimension = maxDim), result = null)
        }
    }

    fun processWhatsAppImage() {
        val uri = _uiState.value.selectedImageUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val result = whatsAppEngine.processWhatsAppImage(
                sourceUri = uri,
                config = _uiState.value.config
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isProcessing = false, result = result.getOrNull()) }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to optimize image for WhatsApp"
                    )
                }
            }
        }
    }
}

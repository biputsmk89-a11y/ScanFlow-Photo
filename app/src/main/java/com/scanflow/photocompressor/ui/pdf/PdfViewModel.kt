package com.scanflow.photocompressor.ui.pdf

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.engine.PdfEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

import com.scanflow.photocompressor.domain.repository.HistoryRepository

data class PdfUiState(
    val selectedImages: List<ImageInfo> = emptyList(),
    val config: PdfConfig = PdfConfig(),
    val isGenerating: Boolean = false,
    val generatedFile: File? = null,
    val savedPdfUri: Uri? = null,
    val isSavedToDocuments: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PdfViewModel @Inject constructor(
    private val pdfEngine: PdfEngine,
    private val imageRepository: ImageRepository,
    private val historyRepository: HistoryRepository,
    private val fileManager: com.scanflow.photocompressor.data.storage.FileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfUiState())
    val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

    fun addImages(uris: List<Uri>) {
        viewModelScope.launch {
            val newInfos = uris.map { uri -> imageRepository.getImageInfo(uri) }
            _uiState.update { it.copy(selectedImages = it.selectedImages + newInfos, errorMessage = null) }
        }
    }

    fun removeImage(index: Int) {
        _uiState.update {
            val updated = it.selectedImages.toMutableList().apply {
                if (index in indices) removeAt(index)
            }
            it.copy(selectedImages = updated)
        }
    }

    fun moveImageUp(index: Int) {
        if (index <= 0) return
        _uiState.update {
            val updated = it.selectedImages.toMutableList()
            val item = updated.removeAt(index)
            updated.add(index - 1, item)
            it.copy(selectedImages = updated)
        }
    }

    fun moveImageDown(index: Int) {
        _uiState.update {
            if (index >= it.selectedImages.size - 1) return@update it
            val updated = it.selectedImages.toMutableList()
            val item = updated.removeAt(index)
            updated.add(index + 1, item)
            it.copy(selectedImages = updated)
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(config = it.config.copy(title = title)) }
    }

    fun updatePageSize(pageSize: PdfPageSize) {
        _uiState.update { it.copy(config = it.config.copy(pageSize = pageSize)) }
    }

    fun updateMargin(margin: PdfMargin) {
        _uiState.update { it.copy(config = it.config.copy(margin = margin)) }
    }

    fun updateOrientation(orientation: PdfOrientation) {
        _uiState.update { it.copy(config = it.config.copy(orientation = orientation)) }
    }

    fun updateQuality(quality: PdfQuality) {
        _uiState.update { it.copy(config = it.config.copy(quality = quality)) }
    }

    fun generatePdf() {
        val state = _uiState.value
        if (state.selectedImages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one photo") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null, generatedFile = null) }
            val uris = state.selectedImages.map { it.uri }
            val result = pdfEngine.generatePdf(uris, state.config)
            if (result.isSuccess) {
                val file = result.getOrNull()
                var savedUri: Uri? = null
                if (file != null) {
                    try {
                        savedUri = fileManager.savePdfToDocuments(file, state.config.title.ifBlank { "document" })
                    } catch (e: Exception) {
                        // fallback to temporary file provider
                    }

                    runCatching {
                        val totalOriginalBytes = state.selectedImages.sumOf { it.fileSize }
                        val pdfBytes = file.length()
                        historyRepository.addEntry(
                            ProcessingHistory(
                                operationType = OperationType.PDF,
                                itemCount = state.selectedImages.size,
                                originalBytes = totalOriginalBytes,
                                outputBytes = pdfBytes,
                                inputFileName = "${state.selectedImages.size} photos",
                                outputUri = (savedUri ?: Uri.fromFile(file)).toString(),
                                outputFileName = file.name,
                                width = 0,
                                height = 0
                            )
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generatedFile = file,
                        savedPdfUri = savedUri,
                        isSavedToDocuments = savedUri != null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to generate PDF"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

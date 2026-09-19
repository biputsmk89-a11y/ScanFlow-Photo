package com.scanflow.photocompressor.ui.passport

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.engine.BitmapUtils
import com.scanflow.photocompressor.engine.PassportEngine
import com.scanflow.photocompressor.engine.PortraitSegmentationEngine
import com.scanflow.photocompressor.engine.backgroundremoval.MaskEditStroke
import com.scanflow.photocompressor.engine.backgroundremoval.MaskEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import javax.inject.Inject

enum class PassportStage {
    IDLE,
    ANALYZE,
    SEGMENT,
    REFINE,
    COMPOSITE,
    RESIZE,
    EXPORT
}

enum class QaPreviewMode {
    COMPOSITE,
    ORIGINAL,
    CHECKERBOARD
}

data class PassportUiState(
    val selectedImageUri: Uri? = null,
    val cutoutUri: Uri? = null,
    val isSegmenting: Boolean = false,
    val currentStage: PassportStage = PassportStage.IDLE,
    val stageProgressText: String = "",
    val qaPreviewMode: QaPreviewMode = QaPreviewMode.COMPOSITE,
    val isRefining: Boolean = false,
    val canUndoRefine: Boolean = false,
    val canRedoRefine: Boolean = false,
    val config: PassportConfig = PassportConfig(),
    val isProcessing: Boolean = false,
    val result: CompressionResult? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PassportViewModel @Inject constructor(
    private val passportEngine: PassportEngine,
    private val portraitSegmentationEngine: PortraitSegmentationEngine,
    private val bitmapUtils: BitmapUtils,
    private val fileManager: FileManager,
    private val maskEditor: MaskEditor = MaskEditor()
) : ViewModel() {

    var ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO

    private val _uiState = MutableStateFlow(PassportUiState())
    val uiState: StateFlow<PassportUiState> = _uiState.asStateFlow()

    fun selectImage(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                cutoutUri = null,
                isSegmenting = true,
                currentStage = PassportStage.ANALYZE,
                stageProgressText = "Preparing photo...",
                result = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        currentStage = PassportStage.SEGMENT,
                        stageProgressText = "Detecting subject (AI)..."
                    )
                }

                val cutoutUri = kotlinx.coroutines.withContext(ioDispatcher) {
                    val originalBitmap = bitmapUtils.decodeBitmap(uri, 2048, 2048)

                    _uiState.update {
                        it.copy(
                            currentStage = PassportStage.REFINE,
                            stageProgressText = "Refining studio edges..."
                        )
                    }

                    val cutoutBitmap = portraitSegmentationEngine.removeBackground(originalBitmap)
                    if (originalBitmap != cutoutBitmap) {
                        originalBitmap.recycle()
                    }

                    // Cache transparent cutout PNG for instant color switching
                    val tempFile = fileManager.createTempFile("passport_cutout_", "png")
                    FileOutputStream(tempFile).use { out ->
                        cutoutBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    cutoutBitmap.recycle()

                    Uri.fromFile(tempFile)
                }

                _uiState.update {
                    it.copy(
                        cutoutUri = cutoutUri,
                        isSegmenting = false,
                        currentStage = PassportStage.IDLE,
                        stageProgressText = ""
                    )
                }
            } catch (e: Exception) {
                // Graceful fallback to original image if segmentation encounters an error
                _uiState.update {
                    it.copy(
                        isSegmenting = false,
                        currentStage = PassportStage.IDLE,
                        stageProgressText = ""
                    )
                }
            }
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

    fun updateTransform(zoomDelta: Float, panDeltaX: Float, panDeltaY: Float) {
        _uiState.update { current ->
            val newZoom = (current.config.zoom * zoomDelta).coerceIn(0.5f, 3.5f)
            val newPanX = current.config.panX + panDeltaX
            val newPanY = current.config.panY + panDeltaY
            current.copy(
                config = current.config.copy(
                    zoom = newZoom,
                    panX = newPanX,
                    panY = newPanY
                ),
                result = null
            )
        }
    }

    fun updateRotation(degrees: Float) {
        _uiState.update { it.copy(config = it.config.copy(rotationDegrees = degrees), result = null) }
    }

    fun updateQaPreviewMode(mode: QaPreviewMode) {
        _uiState.update { it.copy(qaPreviewMode = mode) }
    }

    fun toggleRefineDialog(show: Boolean) {
        _uiState.update { it.copy(isRefining = show) }
    }

    fun applyRefineStroke(stroke: MaskEditStroke) {
        maskEditor.addStroke(stroke)
        _uiState.update {
            it.copy(
                canUndoRefine = maskEditor.canUndo(),
                canRedoRefine = maskEditor.canRedo()
            )
        }
    }

    fun undoRefine() {
        if (maskEditor.undo()) {
            _uiState.update {
                it.copy(
                    canUndoRefine = maskEditor.canUndo(),
                    canRedoRefine = maskEditor.canRedo()
                )
            }
        }
    }

    fun redoRefine() {
        if (maskEditor.redo()) {
            _uiState.update {
                it.copy(
                    canUndoRefine = maskEditor.canUndo(),
                    canRedoRefine = maskEditor.canRedo()
                )
            }
        }
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
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    currentStage = PassportStage.COMPOSITE,
                    stageProgressText = "Compositing studio background...",
                    errorMessage = null
                )
            }

            val result = passportEngine.processPassportPhoto(
                sourceUri = uri,
                config = _uiState.value.config,
                customCropRegion = customCropRegion,
                cutoutUri = _uiState.value.cutoutUri
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        currentStage = PassportStage.IDLE,
                        stageProgressText = "",
                        result = result.getOrNull()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        currentStage = PassportStage.IDLE,
                        stageProgressText = "",
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to process passport photo"
                    )
                }
            }
        }
    }
}

package com.scanflow.photocompressor.ui.crop

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.CropImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CropUiState(
    val selectedImageUri: Uri? = null,
    val imageInfo: ImageInfo? = null,
    val cropX: String = "0",
    val cropY: String = "0",
    val cropWidth: String = "",
    val cropHeight: String = "",
    val selectedAspectRatio: AspectRatioPreset = AspectRatioPreset.FREE,
    val customRatioX: String = "1",
    val customRatioY: String = "1",
    val zoomScale: Float = 1f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    val rotationDegrees: Int = 0,
    val quality: Int = 90,
    val viewportWidth: Int = 0,
    val viewportHeight: Int = 0,
    val isProcessing: Boolean = false,
    val result: CompressionResult? = null,
    val error: String? = null
)

@HiltViewModel
class CropViewModel @Inject constructor(
    private val cropImageUseCase: CropImageUseCase,
    private val imageRepository: ImageRepository,
    private val cropEngine: com.scanflow.photocompressor.engine.CropEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(CropUiState())
    val uiState: StateFlow<CropUiState> = _uiState.asStateFlow()

    fun selectImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val info = imageRepository.getImageInfo(uri)
                _uiState.update {
                    it.copy(
                        selectedImageUri = uri,
                        imageInfo = info,
                        cropX = "0",
                        cropY = "0",
                        cropWidth = info.width.toString(),
                        cropHeight = info.height.toString(),
                        zoomScale = 1f,
                        panOffsetX = 0f,
                        panOffsetY = 0f,
                        rotationDegrees = 0,
                        selectedAspectRatio = AspectRatioPreset.FREE,
                        result = null,
                        error = null
                    )
                }
                recalculateCropRegion()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setAspectRatio(ar: AspectRatioPreset) {
        _uiState.update { it.copy(selectedAspectRatio = ar) }
        recalculateCropRegion()
    }

    fun setCustomRatio(x: String, y: String) {
        _uiState.update { it.copy(customRatioX = x, customRatioY = y, selectedAspectRatio = AspectRatioPreset.CUSTOM) }
        recalculateCropRegion()
    }

    fun updateTransform(zoomDelta: Float, panDeltaX: Float, panDeltaY: Float) {
        _uiState.update { state ->
            val newScale = (state.zoomScale * zoomDelta).coerceIn(1f, 5f)
            val newPanX = state.panOffsetX + panDeltaX
            val newPanY = state.panOffsetY + panDeltaY
            state.copy(
                zoomScale = newScale,
                panOffsetX = newPanX,
                panOffsetY = newPanY
            )
        }
        recalculateCropRegion()
    }

    fun setZoom(scale: Float) {
        _uiState.update { it.copy(zoomScale = scale.coerceIn(1f, 5f)) }
        recalculateCropRegion()
    }

    fun rotate90() {
        _uiState.update { state ->
            val nextRotation = (state.rotationDegrees + 90) % 360
            state.copy(
                rotationDegrees = nextRotation,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
        }
        recalculateCropRegion()
    }

    fun resetTransform() {
        _uiState.update { state ->
            state.copy(
                zoomScale = 1f,
                panOffsetX = 0f,
                panOffsetY = 0f,
                rotationDegrees = 0
            )
        }
        recalculateCropRegion()
    }

    fun setViewportSize(width: Int, height: Int) {
        if (_uiState.value.viewportWidth != width || _uiState.value.viewportHeight != height) {
            _uiState.update { it.copy(viewportWidth = width, viewportHeight = height) }
            recalculateCropRegion()
        }
    }

    fun setCropX(v: String) { _uiState.update { it.copy(cropX = v) } }
    fun setCropY(v: String) { _uiState.update { it.copy(cropY = v) } }
    fun setCropWidth(v: String) { _uiState.update { it.copy(cropWidth = v) } }
    fun setCropHeight(v: String) { _uiState.update { it.copy(cropHeight = v) } }
    fun setQuality(q: Int) { _uiState.update { it.copy(quality = q) } }

    private fun recalculateCropRegion() {
        val state = _uiState.value
        val info = state.imageInfo ?: return

        // Effective dimensions based on rotation
        val isRotated90or270 = state.rotationDegrees == 90 || state.rotationDegrees == 270
        val effWidth = if (isRotated90or270) info.height else info.width
        val effHeight = if (isRotated90or270) info.width else info.height

        val (ratioX, ratioY) = when (state.selectedAspectRatio) {
            AspectRatioPreset.CUSTOM -> {
                val rx = state.customRatioX.toIntOrNull() ?: 1
                val ry = state.customRatioY.toIntOrNull() ?: 1
                Pair(rx, ry)
            }
            AspectRatioPreset.FREE -> Pair(0, 0)
            else -> Pair(state.selectedAspectRatio.ratioX, state.selectedAspectRatio.ratioY)
        }

        // Calculate scaling factor between viewport display pixels and bitmap source pixels
        val scaleFactor = if (state.viewportWidth > 0 && state.viewportHeight > 0 && effWidth > 0 && effHeight > 0) {
            val containerAspect = state.viewportWidth.toFloat() / state.viewportHeight.toFloat()
            val imageAspect = effWidth.toFloat() / effHeight.toFloat()
            val displayedWidth = if (imageAspect > containerAspect) {
                state.viewportWidth.toFloat()
            } else {
                state.viewportHeight.toFloat() * imageAspect
            }
            effWidth.toFloat() / displayedWidth.coerceAtLeast(1f)
        } else {
            1f
        }

        // Invert pan: dragging the image rightwards (+panX) brings the left portion of the image into center view,
        // so the crop window must move leftwards (-X) in image coordinates.
        val bitmapPanX = -state.panOffsetX * scaleFactor
        val bitmapPanY = -state.panOffsetY * scaleFactor

        val region = cropEngine.calculateAspectCropRegion(
            imageWidth = effWidth,
            imageHeight = effHeight,
            targetRatioX = ratioX,
            targetRatioY = ratioY,
            zoomScale = state.zoomScale,
            panOffsetX = bitmapPanX,
            panOffsetY = bitmapPanY
        )

        _uiState.update {
            it.copy(
                cropX = region.x.toString(),
                cropY = region.y.toString(),
                cropWidth = region.width.toString(),
                cropHeight = region.height.toString()
            )
        }
    }

    fun crop() {
        val state = _uiState.value
        val uri = state.selectedImageUri ?: return
        val region = try {
            CropRegion(
                x = state.cropX.toIntOrNull() ?: 0,
                y = state.cropY.toIntOrNull() ?: 0,
                width = state.cropWidth.toIntOrNull() ?: return,
                height = state.cropHeight.toIntOrNull() ?: return
            )
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Invalid crop region: ${e.message}") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            cropImageUseCase(
                inputUri = uri,
                cropRegion = region,
                rotationDegrees = state.rotationDegrees.toFloat(),
                quality = state.quality
            ).fold(
                onSuccess = { r -> _uiState.update { it.copy(isProcessing = false, result = r) } },
                onFailure = { e -> _uiState.update { it.copy(isProcessing = false, error = e.message) } }
            )
        }
    }

    fun reset() {
        _uiState.value = CropUiState()
    }
}

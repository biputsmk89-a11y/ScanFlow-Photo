package com.scanflow.photocompressor.ui.compress

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.CompressImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompressViewModel @Inject constructor(
    private val compressImageUseCase: CompressImageUseCase,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompressUiState())
    val uiState: StateFlow<CompressUiState> = _uiState.asStateFlow()

    private var activeJob: kotlinx.coroutines.Job? = null

    fun selectImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(processingState = ProcessingState.Analyzing) }
            try {
                val firstUri = uris.first()
                val info = imageRepository.getImageInfo(firstUri)
                com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
                    com.scanflow.photocompressor.util.AnalyticsEvent.IMAGE_SELECTED,
                    mapOf("count" to uris.size)
                )
                _uiState.update {
                    it.copy(
                        selectedImageUri = firstUri,
                        selectedImageUris = uris,
                        imageInfo = info,
                        format = info.format,
                        processingState = ProcessingState.Idle,
                        result = null,
                        error = null,
                        typedError = null,
                        isSavedToGallery = false
                    )
                }
            } catch (e: Exception) {
                val typedErr = ProcessingError.fromThrowable(e)
                _uiState.update {
                    it.copy(
                        processingState = ProcessingState.Failed(typedErr),
                        typedError = typedErr,
                        error = typedErr.userFacingMessage
                    )
                }
            }
        }
    }

    fun selectImage(uri: Uri) {
        selectImages(listOf(uri))
    }

    fun setMode(mode: CompressionMode) {
        _uiState.update { current ->
            when (mode) {
                CompressionMode.QUICK -> current.copy(
                    mode = mode,
                    quality = current.quickPreset.quality,
                    maxWidth = current.quickPreset.maxWidth,
                    maxHeight = current.quickPreset.maxHeight
                )
                CompressionMode.QUALITY -> current.copy(mode = mode)
                CompressionMode.TARGET_SIZE -> current.copy(mode = mode)
            }
        }
    }

    fun setQuickPreset(preset: QuickPreset) {
        _uiState.update {
            it.copy(
                mode = CompressionMode.QUICK,
                quickPreset = preset,
                quality = preset.quality,
                maxWidth = preset.maxWidth,
                maxHeight = preset.maxHeight
            )
        }
    }

    fun setTargetSizePreset(preset: TargetSizePreset) {
        _uiState.update { it.copy(mode = CompressionMode.TARGET_SIZE, targetSizePreset = preset) }
    }

    fun setCustomTargetSizeKB(sizeKB: String) {
        val digits = sizeKB.filter { it.isDigit() }
        _uiState.update {
            it.copy(
                mode = CompressionMode.TARGET_SIZE,
                targetSizePreset = TargetSizePreset.CUSTOM,
                customTargetSizeKB = digits
            )
        }
    }

    fun setQuality(quality: Int) {
        _uiState.update { it.copy(quality = quality, mode = CompressionMode.QUALITY) }
    }

    fun setFormat(format: ImageFormat) {
        _uiState.update { it.copy(format = format) }
    }

    fun setMaxDimensions(width: Int, height: Int) {
        _uiState.update { it.copy(maxWidth = width, maxHeight = height) }
    }

    fun setTargetSizeMode(enabled: Boolean) {
        _uiState.update {
            it.copy(mode = if (enabled) CompressionMode.TARGET_SIZE else CompressionMode.QUICK)
        }
    }

    fun setTargetSizeKB(sizeKB: String) {
        setCustomTargetSizeKB(sizeKB)
    }

    fun setMetadataOption(option: MetadataOption) {
        _uiState.update { it.copy(metadataOption = option) }
    }

    fun cancelCompression() {
        activeJob?.cancel()
        activeJob = null
        _uiState.update {
            it.copy(
                isProcessing = false,
                processingState = ProcessingState.Cancelled,
                typedError = ProcessingError.ProcessingCancelled
            )
        }
    }

    fun saveResult() {
        com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
            com.scanflow.photocompressor.util.AnalyticsEvent.EXPORT_CLICKED
        )
        _uiState.update { it.copy(isSavedToGallery = true) }
    }

    fun compress() {
        val state = _uiState.value
        val uris = if (state.selectedImageUris.isNotEmpty()) state.selectedImageUris else listOfNotNull(state.selectedImageUri)
        if (uris.isEmpty()) return

        val effectiveQuality = when (state.mode) {
            CompressionMode.QUICK -> state.quickPreset.quality
            CompressionMode.QUALITY -> state.quality
            CompressionMode.TARGET_SIZE -> 80
        }

        val effectiveMaxWidth = when (state.mode) {
            CompressionMode.QUICK -> state.quickPreset.maxWidth
            else -> state.maxWidth
        }

        val effectiveMaxHeight = when (state.mode) {
            CompressionMode.QUICK -> state.quickPreset.maxHeight
            else -> state.maxHeight
        }

        val targetSizeBytes = if (state.mode == CompressionMode.TARGET_SIZE) {
            if (state.targetSizePreset == TargetSizePreset.CUSTOM) {
                (state.customTargetSizeKB.toLongOrNull() ?: 500L) * 1024L
            } else {
                state.targetSizePreset.bytes
            }
        } else {
            0L
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
                com.scanflow.photocompressor.util.AnalyticsEvent.PROCESSING_STARTED,
                mapOf("item_count" to uris.size, "mode" to state.mode.name)
            )

            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingState = ProcessingState.Processing(0f),
                    error = null,
                    typedError = null,
                    processedCount = 0,
                    totalToProcess = uris.size,
                    currentProgress = if (uris.size > 1) 0f else 0.42f,
                    isSavedToGallery = false
                )
            }

            try {
                if (uris.size == 1) {
                    val result = compressImageUseCase(
                        inputUri = uris.first(),
                        quality = effectiveQuality,
                        format = state.format,
                        maxWidth = effectiveMaxWidth,
                        maxHeight = effectiveMaxHeight,
                        targetSizeBytes = targetSizeBytes,
                        metadataOption = state.metadataOption
                    )

                    result.fold(
                        onSuccess = { compressionResult ->
                            val processingResult = ProcessingResult(
                                outputUri = compressionResult.outputUri,
                                outputMimeType = compressionResult.format.mimeType,
                                outputBytes = compressionResult.compressedSize,
                                width = compressionResult.width,
                                height = compressionResult.height,
                                originalBytes = compressionResult.originalSize,
                                reductionPercent = compressionResult.savedPercentage.toFloat(),
                                processingTimeMs = compressionResult.durationMs
                            )

                            com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
                                com.scanflow.photocompressor.util.AnalyticsEvent.PROCESSING_COMPLETED,
                                mapOf(
                                    "reduction_percent" to compressionResult.savedPercentage.toInt(),
                                    "duration_ms" to compressionResult.durationMs
                                )
                            )

                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    processingState = ProcessingState.Completed(processingResult),
                                    currentProgress = 1f,
                                    processedCount = 1,
                                    result = compressionResult,
                                    outputUris = listOf(compressionResult.outputUri)
                                )
                            }
                        },
                        onFailure = { error ->
                            val typedErr = ProcessingError.fromThrowable(error)
                            com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
                                com.scanflow.photocompressor.util.AnalyticsEvent.PROCESSING_FAILED,
                                mapOf("error_type" to typedErr::class.java.simpleName)
                            )

                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    processingState = ProcessingState.Failed(typedErr),
                                    typedError = typedErr,
                                    error = typedErr.userFacingMessage
                                )
                            }
                        }
                    )
                } else {
                    // Multi-photo batch sequential compression
                    var totalOrigBytes = 0L
                    var totalCompBytes = 0L
                    var lastOutputUri: Uri? = null
                    val collectedOutputUris = mutableListOf<Uri>()

                    for ((index, uri) in uris.withIndex()) {
                        kotlinx.coroutines.yield()
                        val res = compressImageUseCase(
                            inputUri = uri,
                            quality = effectiveQuality,
                            format = state.format,
                            maxWidth = effectiveMaxWidth,
                            maxHeight = effectiveMaxHeight,
                            targetSizeBytes = targetSizeBytes,
                            metadataOption = state.metadataOption
                        )
                        res.onSuccess { r ->
                            totalOrigBytes += r.originalSize
                            totalCompBytes += r.compressedSize
                            lastOutputUri = r.outputUri
                            collectedOutputUris.add(r.outputUri)
                        }
                        val progress = (index + 1).toFloat() / uris.size
                        _uiState.update {
                            it.copy(
                                processedCount = index + 1,
                                currentProgress = progress,
                                processingState = ProcessingState.Processing(progress)
                            )
                        }
                    }

                    val outUri = lastOutputUri
                    if (outUri != null) {
                        val aggregatedResult = CompressionResult(
                            originalSize = totalOrigBytes,
                            compressedSize = totalCompBytes,
                            outputUri = outUri,
                            outputFileName = "compressed_${uris.size}_photos",
                            width = 0,
                            height = 0,
                            format = state.format,
                            quality = effectiveQuality,
                            durationMs = 0L
                        )
                        val processingResult = ProcessingResult(
                            outputUri = outUri,
                            outputMimeType = state.format.mimeType,
                            outputBytes = totalCompBytes,
                            width = 0,
                            height = 0,
                            originalBytes = totalOrigBytes,
                            reductionPercent = if (totalOrigBytes > 0) ((totalOrigBytes - totalCompBytes).toFloat() / totalOrigBytes * 100f) else 0f,
                            processingTimeMs = 0L
                        )
                        com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
                            com.scanflow.photocompressor.util.AnalyticsEvent.BATCH_COMPLETED,
                            mapOf("item_count" to uris.size)
                        )
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                processingState = ProcessingState.Completed(processingResult),
                                result = aggregatedResult,
                                outputUris = collectedOutputUris
                            )
                        }
                    } else {
                        val typedErr = ProcessingError.InvalidImage
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                processingState = ProcessingState.Failed(typedErr),
                                typedError = typedErr,
                                error = typedErr.userFacingMessage
                            )
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingState = ProcessingState.Cancelled,
                        typedError = ProcessingError.ProcessingCancelled
                    )
                }
            } catch (e: Exception) {
                val typedErr = ProcessingError.fromThrowable(e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingState = ProcessingState.Failed(typedErr),
                        typedError = typedErr,
                        error = typedErr.userFacingMessage
                    )
                }
            }
        }
    }

    fun reset() {
        activeJob?.cancel()
        activeJob = null
        _uiState.value = CompressUiState()
    }
}

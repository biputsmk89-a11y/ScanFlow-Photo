package com.scanflow.photocompressor.ui.batch

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.BatchCompressUseCase
import com.scanflow.photocompressor.domain.usecase.ManagePresetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatchUiState(
    val selectedImages: List<ImageInfo> = emptyList(),
    val presets: List<CompressionPreset> = emptyList(),
    val selectedPreset: CompressionPreset? = null,
    val batchJob: BatchJob? = null,
    val isProcessing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BatchViewModel @Inject constructor(
    private val batchCompressUseCase: BatchCompressUseCase,
    private val imageRepository: ImageRepository,
    private val managePresetsUseCase: ManagePresetsUseCase,
    private val fileManager: FileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatchUiState())
    val uiState: StateFlow<BatchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                managePresetsUseCase.seedDefaults()
            } catch (_: Exception) { }
            managePresetsUseCase.getAllPresets().collect { presets ->
                val fallbackList = if (presets.isNotEmpty()) presets else CompressionPreset.defaults
                _uiState.update {
                    it.copy(
                        presets = fallbackList,
                        selectedPreset = it.selectedPreset ?: fallbackList.firstOrNull()
                    )
                }
            }
        }
    }

    fun addImages(uris: List<Uri>) {
        viewModelScope.launch {
            val images = uris.mapNotNull { uri ->
                try { imageRepository.getImageInfo(uri) } catch (e: Exception) { null }
            }
            _uiState.update { it.copy(selectedImages = it.selectedImages + images) }
        }
    }

    fun removeImage(index: Int) {
        _uiState.update {
            it.copy(selectedImages = it.selectedImages.toMutableList().apply { removeAt(index) })
        }
    }

    fun selectPreset(preset: CompressionPreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
    }

    private var batchJobInstance: kotlinx.coroutines.Job? = null

    fun startBatch() {
        val state = _uiState.value
        val preset = state.selectedPreset ?: state.presets.firstOrNull() ?: CompressionPreset.defaults.first()
        if (state.selectedImages.isEmpty()) {
            _uiState.update { it.copy(error = "Please select at least one photo to compress") }
            return
        }

        val ops = mutableListOf<ImageOperation>()
        val maxDim = preset.maxDimension ?: maxOf(preset.maxWidth, preset.maxHeight)
        if (maxDim > 0) {
            ops.add(
                ImageOperation.Resize(
                    width = maxDim,
                    height = maxDim,
                    maintainAspectRatio = true
                )
            )
        }
        ops.add(ImageOperation.Compress(quality = preset.quality ?: 80))
        preset.format?.let { ops.add(ImageOperation.Convert(format = it)) }
        if (preset.removeGps || !preset.preserveExif) {
            ops.add(ImageOperation.RemoveMetadata(removeExif = true))
        }

        val job = BatchJob(
            items = state.selectedImages.map { BatchItem(sourceUri = it.uri) },
            operation = ImagePipeline(ops)
        )
        executeBatch(job)
    }

    private fun executeBatch(job: BatchJob) {
        batchJobInstance?.cancel()
        batchJobInstance = viewModelScope.launch {
            _uiState.update { it.copy(batchJob = job, isProcessing = true, error = null) }
            try {
                batchCompressUseCase(job).collect { updatedJob ->
                    _uiState.update { it.copy(batchJob = updatedJob, isProcessing = !updatedJob.isComplete) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                cancelBatch()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Batch failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Rule 31: Cancellation Policy
     * - Stop pending items (QUEUED -> CANCELLED)
     * - Cancel active work where possible (PROCESSING -> CANCELLED)
     * - Cleanup temporary files via FileManager
     * - Preserve completed outputs (SUCCESS items and their output files remain intact)
     */
    fun cancelBatch() {
        batchJobInstance?.cancel()
        try {
            fileManager.cleanTempFiles()
        } catch (e: Exception) {
            Log.w("BatchViewModel", "Failed to clean temp files during cancel: ${e.message}")
        }
        _uiState.update { current ->
            val updated = current.batchJob?.let { job ->
                val items = job.items.map { item ->
                    if (item.status == BatchItemStatus.QUEUED || item.status == BatchItemStatus.PROCESSING) {
                        item.copy(status = BatchItemStatus.CANCELLED)
                    } else item
                }
                job.copy(items = items)
            }
            current.copy(isProcessing = false, batchJob = updated)
        }
    }

    /**
     * RETRY:
     * Failed items harus dapat di-retry.
     * Flow: Failed -> Retry -> Process Again.
     * Retry hanya item gagal jika memungkinkan (Completed items are preserved).
     */
    fun retryBatch() {
        val currentJob = _uiState.value.batchJob ?: return
        val failedOrCancelled = currentJob.items.filter {
            it.status == BatchItemStatus.FAILED || it.status == BatchItemStatus.CANCELLED
        }
        if (failedOrCancelled.isEmpty()) return

        val newItems = currentJob.items.map { item ->
            if (item.status == BatchItemStatus.FAILED || item.status == BatchItemStatus.CANCELLED) {
                item.copy(status = BatchItemStatus.QUEUED, error = null)
            } else {
                item
            }
        }
        val retryJob = currentJob.copy(items = newItems)
        executeBatch(retryJob)
    }

    /**
     * Retry a specific failed or cancelled item.
     */
    fun retryItem(itemId: String) {
        val currentJob = _uiState.value.batchJob ?: return
        if (_uiState.value.isProcessing) return

        val targetItem = currentJob.items.find { it.id == itemId } ?: return
        if (targetItem.status != BatchItemStatus.FAILED && targetItem.status != BatchItemStatus.CANCELLED) return

        val newItems = currentJob.items.map { item ->
            if (item.id == itemId) {
                item.copy(status = BatchItemStatus.QUEUED, error = null)
            } else {
                item
            }
        }
        val retryJob = currentJob.copy(items = newItems)
        executeBatch(retryJob)
    }

    fun reset() {
        batchJobInstance?.cancel()
        try {
            fileManager.cleanTempFiles()
        } catch (e: Exception) {
            Log.w("BatchViewModel", "Failed to clean temp files during reset: ${e.message}")
        }
        _uiState.value = BatchUiState(
            presets = _uiState.value.presets,
            selectedPreset = _uiState.value.selectedPreset
        )
    }
}

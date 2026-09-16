package com.scanflow.photocompressor.ui.compress

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*

/**
 * UI State for the single image compression screen.
 * Default mode is [CompressionMode.QUICK] with [QuickPreset.BALANCED].
 */
data class CompressUiState(
    val selectedImageUri: Uri? = null,
    val selectedImageUris: List<Uri> = emptyList(),
    val imageInfo: ImageInfo? = null,
    val mode: CompressionMode = CompressionMode.QUICK,
    val quickPreset: QuickPreset = QuickPreset.BALANCED, // Default: Balanced!
    val quality: Int = 75,
    val targetSizePreset: TargetSizePreset = TargetSizePreset.SIZE_500_KB,
    val customTargetSizeKB: String = "500",
    val format: ImageFormat = ImageFormat.JPEG,
    val maxWidth: Int = 0,
    val maxHeight: Int = 0,
    val metadataOption: MetadataOption = MetadataOption.KEEP_METADATA,
    val isProcessing: Boolean = false,
    val processedCount: Int = 0,
    val totalToProcess: Int = 1,
    val currentProgress: Float = -1f,
    val isSavedToGallery: Boolean = true,
    val result: CompressionResult? = null,
    val outputUris: List<Uri> = emptyList(),
    val error: String? = null,
    val processingState: ProcessingState = ProcessingState.Idle,
    val typedError: ProcessingError? = null
) {
    val isTargetSizeMode: Boolean get() = mode == CompressionMode.TARGET_SIZE
    val targetSizeKB: String get() = if (targetSizePreset == TargetSizePreset.CUSTOM) customTargetSizeKB else (targetSizePreset.bytes / 1024).toString()
    val isMultiple: Boolean get() = selectedImageUris.size > 1
    val selectedCount: Int get() = if (selectedImageUris.isNotEmpty()) selectedImageUris.size else if (selectedImageUri != null) 1 else 0
    val userErrorMessage: String? get() = typedError?.userFacingMessage ?: error
}


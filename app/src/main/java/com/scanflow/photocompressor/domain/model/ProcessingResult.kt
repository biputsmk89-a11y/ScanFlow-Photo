package com.scanflow.photocompressor.domain.model

import android.net.Uri

/**
 * Result model representing the completion of an image processing operation.
 * Conforms to the Image Engine specification.
 */
data class ProcessingResult(
    val outputUri: Uri,
    val outputMimeType: String,
    val outputBytes: Long,
    val width: Int,
    val height: Int,
    val originalBytes: Long,
    val reductionPercent: Float,
    val processingTimeMs: Long
)

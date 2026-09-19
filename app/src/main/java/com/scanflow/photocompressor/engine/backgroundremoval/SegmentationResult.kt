package com.scanflow.photocompressor.engine.backgroundremoval

/**
 * Result of portrait segmentation inference.
 */
data class SegmentationResult(
    val mask: AlphaMask,
    val confidence: Float,
    val processingTimeMs: Long,
    val inputWidth: Int,
    val inputHeight: Int
)

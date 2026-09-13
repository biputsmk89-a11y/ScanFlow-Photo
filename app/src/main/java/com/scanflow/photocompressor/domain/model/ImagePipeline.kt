package com.scanflow.photocompressor.domain.model

/**
 * Domain model representing an image processing pipeline configuration.
 * Encapsulates a sequence of operations to be executed on an image.
 */
data class ImagePipeline(
    val operations: List<ImageOperation> = emptyList()
)

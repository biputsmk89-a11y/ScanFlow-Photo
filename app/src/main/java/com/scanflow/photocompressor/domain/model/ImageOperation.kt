package com.scanflow.photocompressor.domain.model

/**
 * Sealed interface representing all possible operations in an ImagePipeline.
 * Operations not requested in a pipeline are strictly skipped during execution.
 */
sealed interface ImageOperation {

    data class Crop(
        val aspectRatio: AspectRatio? = null,
        val region: CropRegion? = null
    ) : ImageOperation

    data class Resize(
        val width: Int? = null,
        val height: Int? = null,
        val maintainAspectRatio: Boolean = true,
        val percentage: Float? = null,
        val maxDimension: Int? = null,
        val preset: ResizePreset? = null
    ) : ImageOperation {
        val targetWidth: Int get() = width ?: 0
        val targetHeight: Int get() = height ?: 0
    }

    data class Compress(
        val quality: Int
    ) : ImageOperation

    data class Convert(
        val format: ImageFormat,
        val backgroundColor: Int = android.graphics.Color.WHITE
    ) : ImageOperation

    data class RemoveMetadata(
        val removeExif: Boolean = true,
        val option: MetadataOption = if (removeExif) MetadataOption.REMOVE_ALL else MetadataOption.KEEP_METADATA
    ) : ImageOperation

    data class Metadata(
        val option: MetadataOption = MetadataOption.KEEP_METADATA
    ) : ImageOperation

    // Auxiliary pipeline operations
    data class Rotate(
        val degrees: Float
    ) : ImageOperation

    data class Flip(
        val horizontal: Boolean = false,
        val vertical: Boolean = false
    ) : ImageOperation

    data class Watermark(
        val config: WatermarkConfig
    ) : ImageOperation

    data class RemoveBackground(
        val backgroundColor: Int? = null,
        val options: com.scanflow.photocompressor.engine.backgroundremoval.BackgroundRemovalOptions = com.scanflow.photocompressor.engine.backgroundremoval.BackgroundRemovalOptions()
    ) : ImageOperation
}

package com.scanflow.photocompressor.engine

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for Social Media image preparation.
 * Pipeline:
 * Platform -> Content Type -> Crop -> Resize -> Compress
 *
 * Configuration is handled through internal presets without forcing the user
 * to know raw pixel dimensions.
 */
@Singleton
class SocialMediaEngine @Inject constructor(
    private val imagePipelineEngine: ImagePipelineEngine
) {

    /**
     * Executes the Social Media pipeline using internal preset.
     */
    suspend fun processSocialMediaImage(
        sourceUri: Uri,
        preset: InternalSocialPreset,
        quality: Int? = null,
        customCropRegion: CropRegion? = null
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        val effectiveQuality = (quality ?: preset.recommendedQuality).coerceIn(1, 100)

        val operations = mutableListOf<ImageOperation>()

        // 1. CROP to internal preset aspect ratio
        if (customCropRegion != null) {
            operations.add(ImageOperation.Crop(region = customCropRegion))
        } else {
            operations.add(
                ImageOperation.Crop(
                    aspectRatio = AspectRatio(preset.ratioX, preset.ratioY)
                )
            )
        }

        // 2. RESIZE to internal preset target dimensions
        operations.add(
            ImageOperation.Resize(
                width = preset.targetWidth,
                height = preset.targetHeight,
                maintainAspectRatio = false
            )
        )

        // 3. COMPRESS with platform-optimized quality
        operations.add(ImageOperation.Compress(quality = effectiveQuality))
        operations.add(ImageOperation.Convert(format = ImageFormat.JPEG))

        val pipeline = ImagePipeline(operations)

        imagePipelineEngine.execute(
            inputUri = sourceUri,
            pipeline = pipeline,
            operationType = OperationType.SOCIAL
        )
    }

    suspend fun processSocialMediaImage(
        sourceUri: Uri,
        platform: SocialPlatform,
        type: SocialContentType,
        quality: Int? = null,
        customCropRegion: CropRegion? = null
    ): Result<CompressionResult> {
        val preset = SocialPresetRegistry.getPreset(platform, type)
        return processSocialMediaImage(sourceUri, preset, quality, customCropRegion)
    }
}

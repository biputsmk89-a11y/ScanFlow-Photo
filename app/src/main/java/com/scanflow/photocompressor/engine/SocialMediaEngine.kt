package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for Social Media image preparation.
 * Pipeline:
 * Platform -> Content Type -> Fit/Fill Mode -> Crop/Canvas -> Resize -> Compress
 *
 * Configuration is handled through internal presets without forcing the user
 * to know raw pixel dimensions.
 */
@Singleton
class SocialMediaEngine @Inject constructor(
    private val imagePipelineEngine: ImagePipelineEngine,
    private val bitmapUtils: BitmapUtils,
    private val fileManager: FileManager,
    private val imageRepository: ImageRepository
) {

    /**
     * Executes the Social Media pipeline using internal preset.
     */
    suspend fun processSocialMediaImage(
        sourceUri: Uri,
        preset: InternalSocialPreset,
        quality: Int? = null,
        fittingMode: SocialFittingMode = SocialFittingMode.FIT,
        customCropRegion: CropRegion? = null
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        val effectiveQuality = (quality ?: preset.recommendedQuality).coerceIn(1, 100)

        // 1. FIT MODE: Preserves 100% of the original photo with aesthetic ambient blurred canvas
        if (fittingMode == SocialFittingMode.FIT && customCropRegion == null) {
            return@withContext try {
                val targetW = preset.targetWidth
                val targetH = preset.targetHeight
                val srcBitmap = bitmapUtils.decodeBitmap(sourceUri, targetW, targetH)

                val canvasBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(canvasBitmap)

                // Ambient blurred backdrop from source image
                val bgThumb = Bitmap.createScaledBitmap(srcBitmap, 80, (80f * targetH / targetW).toInt().coerceAtLeast(1), true)
                val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
                val destRect = Rect(0, 0, targetW, targetH)
                canvas.drawBitmap(bgThumb, null, destRect, bgPaint)
                bgThumb.recycle()

                // Subtle studio dimming overlay for crisp focus on center image
                canvas.drawColor(Color.argb(100, 0, 0, 0))

                // Fitted foreground photo (100% intact, zero pixel cropped)
                val scale = minOf(targetW.toFloat() / srcBitmap.width, targetH.toFloat() / srcBitmap.height)
                val fitW = (srcBitmap.width * scale).toInt()
                val fitH = (srcBitmap.height * scale).toInt()
                val left = (targetW - fitW) / 2
                val top = (targetH - fitH) / 2

                val fgPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
                val fgRect = Rect(left, top, left + fitW, top + fitH)
                canvas.drawBitmap(srcBitmap, null, fgRect, fgPaint)
                srcBitmap.recycle()

                val tempFile = fileManager.createTempFile("social_fit_", "jpg")
                FileOutputStream(tempFile).use { out ->
                    canvasBitmap.compress(Bitmap.CompressFormat.JPEG, effectiveQuality, out)
                }
                val savedSize = tempFile.length()
                canvasBitmap.recycle()

                val fileName = "social_${preset.platform.name.lowercase()}_${preset.type.name.lowercase()}_${System.currentTimeMillis()}"
                val finalSavedUri = imageRepository.saveFromFile(tempFile, fileName, ImageFormat.JPEG)

                Result.success(
                    CompressionResult(
                        originalSize = savedSize,
                        compressedSize = savedSize,
                        outputUri = finalSavedUri,
                        outputFileName = "$fileName.jpg",
                        width = targetW,
                        height = targetH,
                        format = ImageFormat.JPEG,
                        quality = effectiveQuality,
                        durationMs = 100L
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        // 2. FILL MODE: Crops to target aspect ratio
        val operations = mutableListOf<ImageOperation>()

        if (customCropRegion != null) {
            operations.add(ImageOperation.Crop(region = customCropRegion))
        } else {
            operations.add(
                ImageOperation.Crop(
                    aspectRatio = AspectRatio(preset.ratioX, preset.ratioY)
                )
            )
        }

        operations.add(
            ImageOperation.Resize(
                width = preset.targetWidth,
                height = preset.targetHeight,
                maintainAspectRatio = false
            )
        )

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
        fittingMode: SocialFittingMode = SocialFittingMode.FIT,
        customCropRegion: CropRegion? = null
    ): Result<CompressionResult> {
        val preset = SocialPresetRegistry.getPreset(platform, type)
        return processSocialMediaImage(sourceUri, preset, quality, fittingMode, customCropRegion)
    }
}

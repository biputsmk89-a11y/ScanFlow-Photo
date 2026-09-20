package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.engine.ResizeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for resizing an image.
 */
class ResizeImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
    private val historyRepository: HistoryRepository,
    private val resizeEngine: ResizeEngine,
    private val imagePipelineEngine: com.scanflow.photocompressor.engine.ImagePipelineEngine? = null,
    private val preferencesRepository: com.scanflow.photocompressor.domain.repository.PreferencesRepository? = null
) {
    /**
     * Resize an image to the specified dimensions.
     *
     * @param inputUri Source image URI.
     * @param targetWidth Target width (0 to auto-calculate from height).
     * @param targetHeight Target height (0 to auto-calculate from width).
     * @param maintainAspectRatio Whether to maintain the original aspect ratio.
     * @param percentage Alternative: resize by percentage (0.01 to 1.0).
     * @param quality Output quality (1-100).
     * @param format Output format.
     */
    suspend operator fun invoke(
        inputUri: Uri,
        targetWidth: Int = 0,
        targetHeight: Int = 0,
        maintainAspectRatio: Boolean = true,
        percentage: Float? = null,
        maxDimension: Int? = null,
        preset: ResizePreset? = null,
        quality: Int = 90,
        format: ImageFormat = ImageFormat.JPEG
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        if (imagePipelineEngine != null) {
            val ops = listOf(
                ImageOperation.Resize(
                    width = targetWidth,
                    height = targetHeight,
                    maintainAspectRatio = maintainAspectRatio,
                    percentage = percentage,
                    maxDimension = maxDimension,
                    preset = preset
                ),
                ImageOperation.Compress(quality),
                ImageOperation.Convert(format)
            )
            return@withContext imagePipelineEngine.execute(
                inputUri = inputUri,
                pipeline = ImagePipeline(ops),
                operationType = OperationType.RESIZE
            )
        }

        try {
            val startTime = System.currentTimeMillis()
            val imageInfo = imageRepository.getImageInfo(inputUri)
            val reqW = when {
                percentage != null && percentage > 0f -> (imageInfo.width * percentage).toInt()
                maxDimension != null && maxDimension > 0 -> maxDimension
                preset != null && preset.dimension > 0 -> preset.dimension
                else -> targetWidth
            }
            val reqH = when {
                percentage != null && percentage > 0f -> (imageInfo.height * percentage).toInt()
                maxDimension != null && maxDimension > 0 -> maxDimension
                preset != null && preset.dimension > 0 -> preset.dimension
                else -> targetHeight
            }
            val bitmap = imageRepository.loadBitmap(inputUri, reqW, reqH)

            val resized = when {
                preset != null && preset != ResizePreset.CUSTOM -> {
                    resizeEngine.resizeByPreset(bitmap, preset, maintainAspectRatio)
                }
                maxDimension != null && maxDimension > 0 -> {
                    resizeEngine.resizeByMaxDimension(bitmap, maxDimension, maintainAspectRatio)
                }
                percentage != null && percentage > 0f -> {
                    val newWidth = (bitmap.width * percentage).toInt()
                    val newHeight = (bitmap.height * percentage).toInt()
                    resizeEngine.resize(bitmap, newWidth, newHeight, maintainAspectRatio = false)
                }
                else -> {
                    resizeEngine.resize(bitmap, targetWidth, targetHeight, maintainAspectRatio)
                }
            }

            val namingConfig = preferencesRepository?.preferencesFlow?.let { flow ->
                runCatching { flow.first().namingConfig }.getOrNull()
            }
            val outputFileName = com.scanflow.photocompressor.data.storage.FileNamingEngine().generateFileName(
                originalName = imageInfo.fileName,
                operationType = OperationType.RESIZE,
                targetFormat = format,
                namingConfig = namingConfig
            )
            val outputUri = imageRepository.saveBitmap(resized, outputFileName, format, quality)
            val outputSize = imageRepository.getFileSize(outputUri)

            val width = resized.width
            val height = resized.height

            if (resized !== bitmap) bitmap.recycle()
            resized.recycle()

            val result = CompressionResult(
                originalSize = imageInfo.fileSize,
                compressedSize = outputSize,
                outputUri = outputUri,
                outputFileName = outputFileName,
                width = width,
                height = height,
                format = format,
                quality = quality,
                durationMs = System.currentTimeMillis() - startTime
            )

            historyRepository.addEntry(
                ProcessingHistory(
                    inputUri = inputUri.toString(),
                    inputFileName = imageInfo.fileName,
                    outputUri = outputUri.toString(),
                    outputFileName = outputFileName,
                    operation = OperationType.RESIZE,
                    originalSize = imageInfo.fileSize,
                    resultSize = outputSize,
                    width = width,
                    height = height
                )
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.engine.CropEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for cropping an image.
 */
class CropImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
    private val historyRepository: HistoryRepository,
    private val cropEngine: CropEngine,
    private val imagePipelineEngine: com.scanflow.photocompressor.engine.ImagePipelineEngine? = null
) {
    suspend operator fun invoke(
        inputUri: Uri,
        cropRegion: CropRegion,
        quality: Int = 90,
        format: ImageFormat = ImageFormat.JPEG
    ): Result<CompressionResult> = invoke(inputUri = inputUri, cropRegion = cropRegion, quality = quality, format = format)

    suspend operator fun invoke(
        inputUri: Uri,
        aspectRatio: AspectRatio,
        quality: Int = 90,
        format: ImageFormat = ImageFormat.JPEG
    ): Result<CompressionResult> = invoke(inputUri = inputUri, aspectRatio = aspectRatio, quality = quality, format = format)

    suspend operator fun invoke(
        inputUri: Uri,
        cropRegion: CropRegion? = null,
        aspectRatio: AspectRatio? = null,
        rotationDegrees: Float = 0f,
        quality: Int = 90,
        format: ImageFormat = ImageFormat.JPEG
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        if (imagePipelineEngine != null) {
            val ops = buildList {
                if (rotationDegrees != 0f) {
                    add(ImageOperation.Rotate(rotationDegrees))
                }
                add(ImageOperation.Crop(aspectRatio = aspectRatio, region = cropRegion))
                add(ImageOperation.Compress(quality))
                add(ImageOperation.Convert(format))
            }
            return@withContext imagePipelineEngine.execute(
                inputUri = inputUri,
                pipeline = ImagePipeline(ops),
                operationType = OperationType.CROP
            )
        }

        try {
            val startTime = System.currentTimeMillis()
            val imageInfo = imageRepository.getImageInfo(inputUri)
            var currentBitmap = imageRepository.loadBitmap(inputUri)

            if (rotationDegrees != 0f) {
                val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees) }
                val rotated = android.graphics.Bitmap.createBitmap(
                    currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true
                )
                if (rotated !== currentBitmap) {
                    currentBitmap.recycle()
                    currentBitmap = rotated
                }
            }

            val cropped = when {
                cropRegion != null -> cropEngine.crop(currentBitmap, cropRegion, imageInfo.width, imageInfo.height)
                aspectRatio != null -> cropEngine.cropToAspectRatio(currentBitmap, aspectRatio.ratioX, aspectRatio.ratioY)
                else -> currentBitmap
            }

            val outputFileName = "${imageInfo.fileName.substringBeforeLast(".")}_cropped_${System.currentTimeMillis()}.${format.extension}"
            val outputUri = imageRepository.saveBitmap(cropped, outputFileName, format, quality)
            val outputSize = imageRepository.getFileSize(outputUri)

            val width = cropped.width
            val height = cropped.height

            if (cropped !== currentBitmap) {
                currentBitmap.recycle()
            }
            cropped.recycle()

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
                    operation = OperationType.CROP,
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

package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.engine.RotateEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for rotating and flipping images.
 */
class RotateImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
    private val historyRepository: HistoryRepository,
    private val rotateEngine: RotateEngine,
    private val imagePipelineEngine: com.scanflow.photocompressor.engine.ImagePipelineEngine? = null
) {
    suspend operator fun invoke(
        inputUri: Uri,
        degrees: Float = 0f,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        quality: Int = 90,
        format: ImageFormat = ImageFormat.JPEG
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        if (imagePipelineEngine != null) {
            val ops = mutableListOf<ImageOperation>()
            if (degrees != 0f) {
                ops.add(ImageOperation.Rotate(degrees))
            }
            if (flipHorizontal || flipVertical) {
                ops.add(ImageOperation.Flip(flipHorizontal, flipVertical))
            }
            ops.add(ImageOperation.Compress(quality))
            ops.add(ImageOperation.Convert(format))
            val opType = if (flipHorizontal || flipVertical) OperationType.FLIP else OperationType.ROTATE
            return@withContext imagePipelineEngine.execute(
                inputUri = inputUri,
                pipeline = ImagePipeline(ops),
                operationType = opType
            )
        }

        try {
            val startTime = System.currentTimeMillis()
            val imageInfo = imageRepository.getImageInfo(inputUri)
            val bitmap = imageRepository.loadBitmap(inputUri)

            var result = bitmap

            // Apply rotation
            if (degrees != 0f) {
                val rotated = rotateEngine.rotate(result, degrees)
                if (rotated !== result) result.recycle()
                result = rotated
            }

            // Apply flip
            if (flipHorizontal || flipVertical) {
                val flipped = rotateEngine.flip(result, flipHorizontal, flipVertical)
                if (flipped !== result) result.recycle()
                result = flipped
            }

            val opType = if (flipHorizontal || flipVertical) OperationType.FLIP else OperationType.ROTATE
            val suffix = if (opType == OperationType.FLIP) "flipped" else "rotated"

            val outputFileName = "${imageInfo.fileName.substringBeforeLast(".")}_${suffix}_${System.currentTimeMillis()}.${format.extension}"
            val outputUri = imageRepository.saveBitmap(result, outputFileName, format, quality)
            val outputSize = imageRepository.getFileSize(outputUri)

            val width = result.width
            val height = result.height

            if (result !== bitmap) bitmap.recycle()
            result.recycle()

            val compressionResult = CompressionResult(
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
                    operation = opType,
                    originalSize = imageInfo.fileSize,
                    resultSize = outputSize,
                    width = width,
                    height = height
                )
            )

            Result.success(compressionResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

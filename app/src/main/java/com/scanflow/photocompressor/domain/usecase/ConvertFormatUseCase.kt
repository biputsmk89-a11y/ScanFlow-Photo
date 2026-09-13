package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.engine.FormatConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for converting image format.
 */
class ConvertFormatUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
    private val historyRepository: HistoryRepository,
    private val formatConverter: FormatConverter,
    private val imagePipelineEngine: com.scanflow.photocompressor.engine.ImagePipelineEngine? = null
) {
    suspend operator fun invoke(
        inputUri: Uri,
        targetFormat: ImageFormat,
        quality: Int = 90,
        backgroundColor: Int = android.graphics.Color.WHITE
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        if (imagePipelineEngine != null) {
            val ops = listOf(
                ImageOperation.Convert(targetFormat, backgroundColor),
                ImageOperation.Compress(quality)
            )
            return@withContext imagePipelineEngine.execute(
                inputUri = inputUri,
                pipeline = ImagePipeline(ops),
                operationType = OperationType.CONVERT
            )
        }

        try {
            val startTime = System.currentTimeMillis()
            val imageInfo = imageRepository.getImageInfo(inputUri)
            val bitmap = imageRepository.loadBitmap(inputUri)

            val workingBitmap = if (targetFormat == ImageFormat.JPEG && bitmap.hasAlpha()) {
                formatConverter.flattenAlpha(bitmap, backgroundColor)
            } else {
                bitmap
            }

            val outputFileName = com.scanflow.photocompressor.data.storage.FileNamingEngine().generateFileName(
                originalName = imageInfo.fileName,
                operationType = OperationType.CONVERT,
                targetFormat = targetFormat
            )
            val outputUri = imageRepository.saveBitmap(workingBitmap, outputFileName, targetFormat, quality)
            val outputSize = imageRepository.getFileSize(outputUri)

            val width = workingBitmap.width
            val height = workingBitmap.height

            if (workingBitmap !== bitmap) {
                workingBitmap.recycle()
            }
            bitmap.recycle()

            val result = CompressionResult(
                originalSize = imageInfo.fileSize,
                compressedSize = outputSize,
                outputUri = outputUri,
                outputFileName = outputFileName,
                width = width,
                height = height,
                format = targetFormat,
                quality = quality,
                durationMs = System.currentTimeMillis() - startTime
            )

            historyRepository.addEntry(
                ProcessingHistory(
                    inputUri = inputUri.toString(),
                    inputFileName = imageInfo.fileName,
                    outputUri = outputUri.toString(),
                    outputFileName = outputFileName,
                    operation = OperationType.CONVERT,
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

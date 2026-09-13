package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.engine.WatermarkEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for adding text watermark to images.
 */
class AddWatermarkUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
    private val historyRepository: HistoryRepository,
    private val watermarkEngine: WatermarkEngine,
    private val imagePipelineEngine: com.scanflow.photocompressor.engine.ImagePipelineEngine? = null
) {
    suspend operator fun invoke(
        inputUri: Uri,
        config: WatermarkConfig,
        quality: Int = 90,
        format: ImageFormat = ImageFormat.JPEG
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        if (imagePipelineEngine != null) {
            val ops = listOf(
                ImageOperation.Watermark(config),
                ImageOperation.Compress(quality),
                ImageOperation.Convert(format)
            )
            return@withContext imagePipelineEngine.execute(
                inputUri = inputUri,
                pipeline = ImagePipeline(ops),
                operationType = OperationType.WATERMARK
            )
        }

        try {
            val startTime = System.currentTimeMillis()
            val imageInfo = imageRepository.getImageInfo(inputUri)
            val bitmap = imageRepository.loadBitmap(inputUri)

            val watermarked = watermarkEngine.addTextWatermark(bitmap, config)

            val outputFileName = "${imageInfo.fileName.substringBeforeLast(".")}_watermarked_${System.currentTimeMillis()}.${format.extension}"
            val outputUri = imageRepository.saveBitmap(watermarked, outputFileName, format, quality)
            val outputSize = imageRepository.getFileSize(outputUri)

            val width = watermarked.width
            val height = watermarked.height

            bitmap.recycle()
            watermarked.recycle()

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
                    operation = OperationType.WATERMARK,
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

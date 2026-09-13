package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.engine.CompressionEngine
import com.scanflow.photocompressor.engine.ResizeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for compressing a single image.
 */
class CompressImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
    private val historyRepository: HistoryRepository,
    private val compressionEngine: CompressionEngine,
    private val resizeEngine: ResizeEngine,
    private val imagePipelineEngine: com.scanflow.photocompressor.engine.ImagePipelineEngine? = null,
    private val entitlementRepository: com.scanflow.photocompressor.domain.repository.EntitlementRepository? = null
) {
    /**
     * Compress an image with the given parameters.
     *
     * @param inputUri The URI of the image to compress.
     * @param quality Compression quality (1-100).
     * @param format Output format.
     * @param maxWidth Maximum width (0 = no resize).
     * @param maxHeight Maximum height (0 = no resize).
     * @param preserveExif Whether to preserve EXIF metadata.
     * @return CompressionResult with output URI and statistics.
     */
    suspend operator fun invoke(
        inputUri: Uri,
        quality: Int = 80,
        format: ImageFormat = ImageFormat.JPEG,
        maxWidth: Int = 0,
        maxHeight: Int = 0,
        preserveExif: Boolean = true,
        targetSizeBytes: Long = 0L,
        metadataOption: MetadataOption? = null
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        val effectiveMetaOption = metadataOption ?: if (preserveExif) MetadataOption.KEEP_METADATA else MetadataOption.REMOVE_ALL

        // 72. FEATURE GATING at UseCase level
        if (entitlementRepository != null) {
            if (targetSizeBytes > 0L) {
                val check = entitlementRepository.checkFeatureAccess(ProFeature.TARGET_FILE_SIZE)
                if (check is EntitlementResult.Restricted) {
                    return@withContext Result.failure(FeatureRestrictedException(ProFeature.TARGET_FILE_SIZE, check.message))
                }
            }
            if (effectiveMetaOption == MetadataOption.REMOVE_ALL) {
                val check = entitlementRepository.checkFeatureAccess(ProFeature.METADATA_REMOVAL)
                if (check is EntitlementResult.Restricted) {
                    return@withContext Result.failure(FeatureRestrictedException(ProFeature.METADATA_REMOVAL, check.message))
                }
            }
        }

        if (imagePipelineEngine != null) {
            val ops = mutableListOf<ImageOperation>()
            if (maxWidth > 0 || maxHeight > 0) {
                ops.add(ImageOperation.Resize(maxWidth, maxHeight, maintainAspectRatio = true))
            }
            if (quality > 0 && targetSizeBytes <= 0L) {
                ops.add(ImageOperation.Compress(quality))
            }
            ops.add(ImageOperation.Convert(format))
            ops.add(ImageOperation.Metadata(effectiveMetaOption))

            return@withContext imagePipelineEngine.execute(
                inputUri = inputUri,
                pipeline = ImagePipeline(ops),
                targetSizeBytes = targetSizeBytes,
                operationType = OperationType.COMPRESS
            )
        }

        try {
            val startTime = System.currentTimeMillis()

            // Get original image info
            val imageInfo = imageRepository.getImageInfo(inputUri)

            // Load bitmap (memory-efficient with target dimensions)
            var bitmap = imageRepository.loadBitmap(inputUri, maxWidth, maxHeight)

            // Resize if needed
            if (maxWidth > 0 && maxHeight > 0) {
                val resized = resizeEngine.resize(bitmap, maxWidth, maxHeight, maintainAspectRatio = true)
                if (resized !== bitmap) {
                    bitmap.recycle()
                    bitmap = resized
                }
            }

            // Determine effective quality (use target size binary search if requested)
            val effectiveQuality = if (targetSizeBytes > 0L) {
                val targetResult = compressionEngine.compressToTargetSize(
                    bitmap,
                    format,
                    targetSizeBytes
                )
                if (targetResult.finalBitmap !== bitmap) {
                    bitmap.recycle()
                    bitmap = targetResult.finalBitmap
                }
                targetResult.quality
            } else {
                quality
            }

            // Generate output filename
            val outputFileName = generateOutputFileName(imageInfo.fileName, format)

            // Save to file
            val outputUri = imageRepository.saveBitmap(bitmap, outputFileName, format, effectiveQuality)
            val outputSize = imageRepository.getFileSize(outputUri)

            // Capture dimensions before recycling
            val resultWidth = bitmap.width
            val resultHeight = bitmap.height

            // Clean up bitmap
            bitmap.recycle()

            val durationMs = System.currentTimeMillis() - startTime

            val result = CompressionResult(
                originalSize = imageInfo.fileSize,
                compressedSize = outputSize,
                outputUri = outputUri,
                outputFileName = outputFileName,
                width = resultWidth,
                height = resultHeight,
                format = format,
                quality = effectiveQuality,
                durationMs = durationMs,
                preservedExif = preserveExif
            )

            // Save to history
            historyRepository.addEntry(
                ProcessingHistory(
                    inputUri = inputUri.toString(),
                    inputFileName = imageInfo.fileName,
                    outputUri = outputUri.toString(),
                    outputFileName = outputFileName,
                    operation = OperationType.COMPRESS,
                    originalSize = imageInfo.fileSize,
                    resultSize = outputSize,
                    width = resultWidth,
                    height = resultHeight
                )
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateOutputFileName(originalName: String, format: ImageFormat): String {
        return com.scanflow.photocompressor.data.storage.FileNamingEngine().generateFileName(
            originalName = originalName,
            operationType = OperationType.COMPRESS,
            targetFormat = format
        )
    }
}

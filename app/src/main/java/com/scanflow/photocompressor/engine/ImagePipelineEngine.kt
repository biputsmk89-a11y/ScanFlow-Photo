package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import android.net.Uri
import com.scanflow.photocompressor.data.storage.ExifHandler
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.data.storage.FileNamingEngine
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * =========================================================================
 * IMAGE PIPELINE ENGINE
 * =========================================================================
 *
 * Ordered pipeline stages:
 *
 * Decode
 *   ↓
 * Normalize Orientation
 *   ↓
 * Crop
 *   ↓
 * Resize
 *   ↓
 * Metadata Policy
 *   ↓
 * Compression
 *   ↓
 * Format Encode
 *   ↓
 * Validation
 *   ↓
 * Export
 *
 * Rule: Operation yang tidak diperlukan tidak dijalankan (strictly skipped).
 * =========================================================================
 */
@Singleton
class ImagePipelineEngine @Inject constructor(
    private val compressionEngine: CompressionEngine,
    private val resizeEngine: ResizeEngine,
    private val cropEngine: CropEngine,
    private val rotateEngine: RotateEngine,
    private val watermarkEngine: WatermarkEngine,
    private val formatConverter: FormatConverter,
    private val imageRepository: ImageRepository,
    private val historyRepository: HistoryRepository,
    private val exifHandler: ExifHandler,
    private val metadataEngine: MetadataEngine? = null,
    private val fileManager: FileManager? = null,
    private val outputValidator: OutputValidator? = null,
    private val fileNamingEngine: FileNamingEngine = FileNamingEngine()
) {

    /**
     * Executes the pipeline according to the strict contract specification.
     * Operations not requested in [pipeline] are skipped entirely.
     */
    suspend fun execute(
        inputUri: Uri,
        pipeline: ImagePipeline,
        targetSizeBytes: Long = 0L,
        operationType: OperationType = OperationType.COMPRESS
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        try {
            val startTime = System.currentTimeMillis()
            val operations = pipeline.operations

            // Inspect metadata
            val imageInfo = imageRepository.getImageInfo(inputUri)

            // =================================================================
            // 1. STAGE: DECODE (Memory-safe with required dimensions)
            // =================================================================
            val resizeOp = operations.filterIsInstance<ImageOperation.Resize>().firstOrNull()
            var reqWidth = 0
            var reqHeight = 0
            if (resizeOp != null) {
                if (resizeOp.percentage != null && resizeOp.percentage > 0f) {
                    reqWidth = (imageInfo.width * resizeOp.percentage).toInt()
                    reqHeight = (imageInfo.height * resizeOp.percentage).toInt()
                } else {
                    reqWidth = resizeOp.width ?: 0
                    reqHeight = resizeOp.height ?: 0
                }
            }

            var currentBitmap = imageRepository.loadBitmap(inputUri, reqWidth, reqHeight)

            // =================================================================
            // 2. STAGE: NORMALIZE ORIENTATION
            // BitmapUtils already ensures orientation normalization on decode.
            // Any explicit rotation / flip operations are applied here.
            // =================================================================
            for (rotateOp in operations.filterIsInstance<ImageOperation.Rotate>()) {
                val rotated = rotateEngine.rotate(currentBitmap, rotateOp.degrees)
                if (rotated !== currentBitmap) {
                    currentBitmap.recycle()
                    currentBitmap = rotated
                }
            }
            for (flipOp in operations.filterIsInstance<ImageOperation.Flip>()) {
                val flipped = rotateEngine.flip(currentBitmap, flipOp.horizontal, flipOp.vertical)
                if (flipped !== currentBitmap) {
                    currentBitmap.recycle()
                    currentBitmap = flipped
                }
            }

            // =================================================================
            // 3. STAGE: CROP (Skipped if not in operations)
            // =================================================================
            val cropOp = operations.filterIsInstance<ImageOperation.Crop>().firstOrNull()
            if (cropOp != null) {
                val cropped = when {
                    cropOp.region != null -> {
                        cropEngine.crop(currentBitmap, cropOp.region, imageInfo.width, imageInfo.height)
                    }
                    cropOp.aspectRatio != null -> {
                        cropEngine.cropToAspectRatio(
                            currentBitmap,
                            cropOp.aspectRatio.ratioX,
                            cropOp.aspectRatio.ratioY
                        )
                    }
                    else -> currentBitmap
                }
                if (cropped !== currentBitmap) {
                    currentBitmap.recycle()
                    currentBitmap = cropped
                }
            }

            // =================================================================
            // 4. STAGE: RESIZE (Skipped if not in operations)
            // =================================================================
            if (resizeOp != null) {
                val resized = when {
                    resizeOp.preset != null && resizeOp.preset != com.scanflow.photocompressor.domain.model.ResizePreset.CUSTOM -> {
                        resizeEngine.resizeByPreset(currentBitmap, resizeOp.preset, resizeOp.maintainAspectRatio)
                    }
                    resizeOp.maxDimension != null && resizeOp.maxDimension > 0 -> {
                        resizeEngine.resizeByMaxDimension(currentBitmap, resizeOp.maxDimension, resizeOp.maintainAspectRatio)
                    }
                    resizeOp.percentage != null && resizeOp.percentage > 0f -> {
                        resizeEngine.resizeByPercentage(currentBitmap, resizeOp.percentage)
                    }
                    (resizeOp.width != null && resizeOp.width > 0) || (resizeOp.height != null && resizeOp.height > 0) -> {
                        resizeEngine.resize(
                            currentBitmap,
                            resizeOp.width ?: 0,
                            resizeOp.height ?: 0,
                            resizeOp.maintainAspectRatio
                        )
                    }
                    else -> currentBitmap
                }
                if (resized !== currentBitmap) {
                    currentBitmap.recycle()
                    currentBitmap = resized
                }
            }

            // Apply watermarking if requested
            for (watermarkOp in operations.filterIsInstance<ImageOperation.Watermark>()) {
                val watermarked = watermarkEngine.addTextWatermark(currentBitmap, watermarkOp.config)
                if (watermarked !== currentBitmap) {
                    currentBitmap.recycle()
                    currentBitmap = watermarked
                }
            }

            // =================================================================
            // 5. STAGE: METADATA POLICY (Skipped / preserved unless specified)
            // =================================================================
            val metadataOp = operations.filterIsInstance<ImageOperation.Metadata>().firstOrNull()
            val removeMetaOp = operations.filterIsInstance<ImageOperation.RemoveMetadata>().firstOrNull()

            val metadataOption = when {
                metadataOp != null -> metadataOp.option
                removeMetaOp != null -> removeMetaOp.option
                else -> MetadataOption.KEEP_METADATA
            }
            val preserveExif = (metadataOption != MetadataOption.REMOVE_ALL)

            // =================================================================
            // 6. STAGE: COMPRESSION
            // =================================================================
            val convertOp = operations.filterIsInstance<ImageOperation.Convert>().firstOrNull()
            val compressOp = operations.filterIsInstance<ImageOperation.Compress>().firstOrNull()
            val finalFormat = convertOp?.format ?: imageInfo.format

            // Flatten transparency if converting to JPEG
            if (finalFormat == ImageFormat.JPEG && currentBitmap.hasAlpha()) {
                val bgColor = convertOp?.backgroundColor ?: android.graphics.Color.WHITE
                val flattened = formatConverter.flattenAlpha(currentBitmap, bgColor)
                if (flattened !== currentBitmap) {
                    currentBitmap.recycle()
                    currentBitmap = flattened
                }
            }

            var targetEncodedBytes: ByteArray? = null
            val effectiveQuality = when {
                targetSizeBytes > 0L -> {
                    val targetResult = compressionEngine.compressToTargetSize(
                        currentBitmap,
                        finalFormat,
                        targetSizeBytes
                    )
                    if (targetResult.finalBitmap !== currentBitmap) {
                        currentBitmap.recycle()
                        currentBitmap = targetResult.finalBitmap
                    }
                    targetEncodedBytes = targetResult.bytes
                    targetResult.quality
                }
                compressOp != null -> compressOp.quality
                else -> 85 // Default high-quality compression
            }

            // =================================================================
            // 7. STAGE: FORMAT ENCODE
            // =================================================================
            val encodedBytes = targetEncodedBytes ?: compressionEngine.compress(currentBitmap, finalFormat, effectiveQuality)
            val resultWidth = currentBitmap.width
            val resultHeight = currentBitmap.height

            // Recycle working bitmap memory immediately after encode
            currentBitmap.recycle()

            if (encodedBytes.isEmpty()) {
                throw IllegalStateException("Validation error: Encoded output byte stream is empty")
            }
            if (resultWidth <= 0 || resultHeight <= 0) {
                throw IllegalStateException("Validation error: Invalid bitmap dimensions (${resultWidth}x${resultHeight})")
            }

            // =================================================================
            // 8. STAGE: STORAGE RULE & 5-STEP OUTPUT VALIDATION
            // Flow: Original -> Temp Output -> Validate -> Final Output
            // Flow 58: Input URI -> Temp file -> Encode -> Close -> Validate -> Move/Publish -> Final output URI
            // If failed: cleanup temp
            // =================================================================
            val outputFileName = fileNamingEngine.generateFileName(
                originalName = imageInfo.fileName,
                operationType = operationType,
                targetFormat = finalFormat
            )

            val tempFile = fileManager?.createTempFile("pipeline_temp_", finalFormat.extension)
                ?: File.createTempFile("pipeline_temp_", ".${finalFormat.extension}")

            val outputUri = try {
                tempFile.writeBytes(encodedBytes)

                // Apply metadata strictly to the temporary output file (original is untouched)
                if (metadataOption != MetadataOption.REMOVE_ALL && metadataEngine != null) {
                    metadataEngine.applyMetadataToFile(inputUri, tempFile, metadataOption)
                }

                // 5-step Output Validation:
                // 1. file exists -> 2. file > 0 bytes -> 3. MIME valid -> 4. decode output -> 5. dimensions valid
                if (outputValidator != null) {
                    val validation = outputValidator.validate(tempFile, finalFormat)
                    if (validation is ValidationResult.Failure) {
                        throw IllegalStateException("Output validation failed: ${validation.reason} (Step: ${validation.step})")
                    }
                }

                // =============================================================
                // 9. STAGE: EXPORT (Promote Validated Temp to Final Destination)
                // =============================================================
                val finalSavedUri = imageRepository.saveFromFile(tempFile, outputFileName, finalFormat)

                // Safety Rule: Never overwrite original source file
                if (finalSavedUri == inputUri) {
                    throw IllegalStateException("Storage safety violation: Output URI matches input URI, overwriting original is strictly prohibited")
                }

                finalSavedUri
            } finally {
                // Guaranteed purge of temporary file
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }

            val outputSize = imageRepository.getFileSize(outputUri)
            val durationMs = System.currentTimeMillis() - startTime

            val result = CompressionResult(
                originalSize = imageInfo.fileSize,
                compressedSize = outputSize,
                outputUri = outputUri,
                outputFileName = outputFileName,
                width = resultWidth,
                height = resultHeight,
                format = finalFormat,
                quality = effectiveQuality,
                durationMs = durationMs,
                preservedExif = preserveExif
            )

            // Centralized history tracking
            historyRepository.addEntry(
                ProcessingHistory(
                    inputUri = inputUri.toString(),
                    inputFileName = imageInfo.fileName,
                    outputUri = outputUri.toString(),
                    outputFileName = outputFileName,
                    operation = operationType,
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

    /**
     * Backward-compatible helper method mapping parameter lists into an [ImagePipeline].
     */
    suspend fun executePipeline(
        inputUri: Uri,
        operations: List<ImageOperation> = emptyList(),
        targetFormat: ImageFormat? = null,
        quality: Int = 80,
        targetSizeBytes: Long = 0L,
        preserveExif: Boolean = true,
        metadataOption: MetadataOption? = null,
        operationType: OperationType = OperationType.COMPRESS
    ): Result<CompressionResult> {
        val ops = operations.toMutableList()
        if (targetFormat != null && ops.none { it is ImageOperation.Convert }) {
            ops.add(ImageOperation.Convert(targetFormat))
        }
        if (quality > 0 && ops.none { it is ImageOperation.Compress } && targetSizeBytes <= 0L) {
            ops.add(ImageOperation.Compress(quality))
        }
        val effectiveMetaOption = metadataOption ?: if (!preserveExif) MetadataOption.REMOVE_ALL else null
        if (effectiveMetaOption != null && ops.none { it is ImageOperation.Metadata || it is ImageOperation.RemoveMetadata }) {
            ops.add(ImageOperation.Metadata(effectiveMetaOption))
        }

        return execute(
            inputUri = inputUri,
            pipeline = ImagePipeline(ops),
            targetSizeBytes = targetSizeBytes,
            operationType = operationType
        )
    }

    private fun generateOutputFileName(
        originalName: String,
        targetFormat: ImageFormat,
        operationType: OperationType
    ): String {
        return fileNamingEngine.generateFileName(originalName, operationType, targetFormat)
    }
}

package com.scanflow.photocompressor.engine

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for WhatsApp-ready photo optimization.
 * Presets:
 * - Small
 * - Balanced
 * - High Quality
 * - Custom
 *
 * Does not promise absolute file size numbers. Displays Before, After, Saved, Reduction.
 * Fully integrated with ImagePipelineEngine.
 */
@Singleton
class WhatsAppEngine @Inject constructor(
    private val imagePipelineEngine: ImagePipelineEngine
) {

    /**
     * Executes the WhatsApp optimization pipeline based on tier or custom config.
     */
    suspend fun processWhatsAppImage(
        sourceUri: Uri,
        config: WhatsAppConfig
    ): Result<CompressionResult> = withContext(Dispatchers.Default) {
        val (maxDim, quality, targetBytes) = when (config.tier) {
            WhatsAppTier.SMALL -> Triple(
                WhatsAppTier.SMALL.maxDimension,
                WhatsAppTier.SMALL.quality,
                WhatsAppTier.SMALL.targetSizeBytes
            )
            WhatsAppTier.BALANCED -> Triple(
                WhatsAppTier.BALANCED.maxDimension,
                WhatsAppTier.BALANCED.quality,
                WhatsAppTier.BALANCED.targetSizeBytes
            )
            WhatsAppTier.HIGH_QUALITY -> Triple(
                WhatsAppTier.HIGH_QUALITY.maxDimension,
                WhatsAppTier.HIGH_QUALITY.quality,
                WhatsAppTier.HIGH_QUALITY.targetSizeBytes
            )
            WhatsAppTier.CUSTOM -> Triple(
                config.customMaxDimension.coerceAtLeast(100),
                config.customQuality.coerceIn(1, 100),
                (config.customTargetSizeKB * 1024L).coerceAtLeast(0L)
            )
        }

        val operations = listOf(
            ImageOperation.Resize(
                maxDimension = maxDim,
                maintainAspectRatio = true
            ),
            ImageOperation.Compress(
                quality = quality
            ),
            ImageOperation.Convert(
                format = ImageFormat.JPEG
            )
        )

        val pipeline = ImagePipeline(operations)

        imagePipelineEngine.execute(
            inputUri = sourceUri,
            pipeline = pipeline,
            targetSizeBytes = targetBytes,
            operationType = OperationType.COMPRESS
        )
    }
}

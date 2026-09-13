package com.scanflow.photocompressor.engine

import android.net.Uri
import com.scanflow.photocompressor.domain.model.ImageAnalysis
import com.scanflow.photocompressor.domain.model.ImagePipeline
import com.scanflow.photocompressor.domain.model.ProcessingResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ImageEngine] conforming to the engine contract.
 * Combines zero-pixel [ImageAnalyzer] inspection and memory-safe [ImagePipelineEngine] execution.
 */
@Singleton
class ImageEngineImpl @Inject constructor(
    private val imageAnalyzer: ImageAnalyzer,
    private val pipelineEngine: ImagePipelineEngine
) : ImageEngine {

    override suspend fun analyze(source: Uri): ImageAnalysis {
        return imageAnalyzer.analyze(source).getOrThrow()
    }

    override suspend fun process(
        source: Uri,
        pipeline: ImagePipeline
    ): ProcessingResult {
        val result = pipelineEngine.execute(source, pipeline).getOrThrow()

        val reductionPercent = com.scanflow.photocompressor.util.ReductionCalculator.calculateReductionPercentFloat(
            originalBytes = result.originalSize,
            outputBytes = result.compressedSize
        )

        return ProcessingResult(
            outputUri = result.outputUri,
            outputMimeType = result.format.mimeType,
            outputBytes = result.compressedSize,
            width = result.width,
            height = result.height,
            originalBytes = result.originalSize,
            reductionPercent = reductionPercent,
            processingTimeMs = result.durationMs
        )
    }
}

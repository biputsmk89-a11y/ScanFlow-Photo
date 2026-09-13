package com.scanflow.photocompressor.engine

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ImageEngineTest {

    private lateinit var imageAnalyzer: ImageAnalyzer
    private lateinit var pipelineEngine: ImagePipelineEngine
    private lateinit var imageEngine: ImageEngine

    private val sourceUri = mockk<Uri>()
    private val outputUri = mockk<Uri>()

    @Before
    fun setup() {
        imageAnalyzer = mockk(relaxed = true)
        pipelineEngine = mockk(relaxed = true)
        imageEngine = ImageEngineImpl(imageAnalyzer, pipelineEngine)
    }

    @Test
    fun `analyze delegates directly to imageAnalyzer and returns ImageAnalysis`() = runTest {
        val expectedAnalysis = ImageAnalysis(
            width = 3840,
            height = 2160,
            mimeType = "image/jpeg",
            fileSizeBytes = 5_000_000L,
            orientation = 0,
            hasAlpha = false,
            estimatedMemoryBytes = 33_177_600L,
            exifAvailable = true
        )

        coEvery { imageAnalyzer.analyze(sourceUri) } returns Result.success(expectedAnalysis)

        val analysis = imageEngine.analyze(sourceUri)

        assertEquals(3840, analysis.width)
        assertEquals(2160, analysis.height)
        assertEquals("image/jpeg", analysis.mimeType)
        assertEquals(5_000_000L, analysis.fileSizeBytes)
        coVerify(exactly = 1) { imageAnalyzer.analyze(sourceUri) }
    }

    @Test
    fun `process executes pipeline and maps to ProcessingResult with correct reduction percent`() = runTest {
        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Compress(quality = 70)
            )
        )

        val compressionResult = CompressionResult(
            originalSize = 1_000_000L,
            compressedSize = 400_000L,
            outputUri = outputUri,
            outputFileName = "test_compressed.jpg",
            width = 1920,
            height = 1080,
            format = ImageFormat.JPEG,
            quality = 70,
            durationMs = 120L,
            preservedExif = true
        )

        coEvery { pipelineEngine.execute(sourceUri, pipeline) } returns Result.success(compressionResult)

        val result = imageEngine.process(sourceUri, pipeline)

        assertEquals(outputUri, result.outputUri)
        assertEquals("image/jpeg", result.outputMimeType)
        assertEquals(400_000L, result.outputBytes)
        assertEquals(1_000_000L, result.originalBytes)
        assertEquals(1920, result.width)
        assertEquals(1080, result.height)
        assertEquals(60.0f, result.reductionPercent, 0.01f)
        assertEquals(120L, result.processingTimeMs)

        coVerify(exactly = 1) { pipelineEngine.execute(sourceUri, pipeline) }
    }
}

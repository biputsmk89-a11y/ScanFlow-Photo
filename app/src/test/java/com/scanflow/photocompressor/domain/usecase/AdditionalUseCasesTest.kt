package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.CompressionPreset
import com.scanflow.photocompressor.domain.model.CompressionResult
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.repository.PresetRepository
import com.scanflow.photocompressor.engine.BatchProcessor
import com.scanflow.photocompressor.engine.ImageAnalyzer
import com.scanflow.photocompressor.util.ShareHelper
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AdditionalUseCasesTest {

    @Test
    fun `AnalyzeImageUseCase delegates to ImageAnalyzer`() = runTest {
        val analyzer = mockk<ImageAnalyzer>()
        val uri = mockk<Uri>()
        coEvery { analyzer.analyze(uri) } returns Result.success(mockk())

        val useCase = AnalyzeImageUseCase(analyzer)
        val result = useCase(uri)
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { analyzer.analyze(uri) }
    }

    @Test
    fun `CompressToTargetSizeUseCase delegates to CompressImageUseCase`() = runTest {
        val compressImageUseCase = mockk<CompressImageUseCase>()
        val uri = mockk<Uri>()
        val expectedResult = mockk<CompressionResult>()
        coEvery {
            compressImageUseCase(
                inputUri = uri,
                quality = any(),
                format = any(),
                maxWidth = any(),
                maxHeight = any(),
                targetSizeBytes = 500_000L,
                metadataOption = any()
            )
        } returns Result.success(expectedResult)

        val useCase = CompressToTargetSizeUseCase(compressImageUseCase)
        val result = useCase(uri, targetSizeBytes = 500_000L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `ConvertImageUseCase delegates to ConvertFormatUseCase`() = runTest {
        val convertFormatUseCase = mockk<ConvertFormatUseCase>()
        val uri = mockk<Uri>()
        coEvery {
            convertFormatUseCase(
                inputUri = uri,
                targetFormat = ImageFormat.WEBP,
                quality = 90,
                backgroundColor = any()
            )
        } returns Result.success(mockk())

        val useCase = ConvertImageUseCase(convertFormatUseCase)
        val result = useCase(uri, targetFormat = ImageFormat.WEBP)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `SavePresetUseCase delegates to PresetRepository`() = runTest {
        val repo = mockk<PresetRepository>()
        val preset = mockk<CompressionPreset>()
        coEvery { repo.savePreset(preset) } returns 42L

        val useCase = SavePresetUseCase(repo)
        val id = useCase(preset)
        assertEquals(42L, id)
        coVerify(exactly = 1) { repo.savePreset(preset) }
    }

    @Test
    fun `ProcessBatchUseCase delegates to BatchProcessor`() = runTest {
        val batchProcessor = mockk<BatchProcessor>()
        every { batchProcessor.process(any()) } returns flowOf(mockk())

        val useCase = ProcessBatchUseCase(batchProcessor)
        val flow = useCase(uris = listOf(mockk()), operation = mockk())
        assertNotNull(flow)
        verify(exactly = 1) { batchProcessor.process(any()) }
    }

    @Test
    fun `ShareOutputUseCase delegates to ShareHelper`() {
        val shareHelper = mockk<ShareHelper>(relaxed = true)
        val uri = mockk<Uri>()
        val useCase = ShareOutputUseCase(mockk(relaxed = true), shareHelper)

        useCase(uri)
        verify(exactly = 1) { shareHelper.shareImage(any(), uri, any()) }
    }
}

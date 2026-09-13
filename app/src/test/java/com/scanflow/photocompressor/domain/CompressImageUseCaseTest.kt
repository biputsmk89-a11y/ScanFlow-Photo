package com.scanflow.photocompressor.domain

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.CompressImageUseCase
import com.scanflow.photocompressor.engine.CompressionEngine
import com.scanflow.photocompressor.engine.ResizeEngine
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CompressImageUseCaseTest {

    private lateinit var useCase: CompressImageUseCase
    private lateinit var imageRepository: ImageRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var compressionEngine: CompressionEngine
    private lateinit var resizeEngine: ResizeEngine

    @Before
    fun setup() {
        imageRepository = mockk(relaxed = true)
        historyRepository = mockk(relaxed = true)
        compressionEngine = mockk(relaxed = true)
        resizeEngine = mockk(relaxed = true)

        useCase = CompressImageUseCase(
            imageRepository, historyRepository, compressionEngine, resizeEngine
        )
    }

    @Test
    fun `invoke returns failure when image loading fails`() = runTest {
        val uri = mockk<Uri>()
        coEvery { imageRepository.getImageInfo(uri) } throws RuntimeException("File not found")

        val result = useCase(uri)

        assertTrue("Should return failure", result.isFailure)
        assertTrue("Error message should contain reason", result.exceptionOrNull()?.message?.contains("File not found") == true)
    }

    @Test
    fun `invoke records history on success`() = runTest {
        val uri = mockk<Uri>()
        val outputUri = mockk<Uri>()
        val imageInfo = ImageInfo(
            uri = uri,
            fileName = "test.jpg",
            fileSize = 1_000_000,
            width = 1920,
            height = 1080,
            format = ImageFormat.JPEG,
            mimeType = "image/jpeg"
        )

        val bitmap = mockk<android.graphics.Bitmap>(relaxed = true) {
            every { width } returns 100
            every { height } returns 100
        }

        coEvery { imageRepository.getImageInfo(uri) } returns imageInfo
        coEvery { imageRepository.loadBitmap(uri, any(), any()) } returns bitmap
        coEvery { compressionEngine.compress(any(), any(), any()) } returns ByteArray(500_000)
        coEvery { imageRepository.saveBitmap(any(), any(), any(), any()) } returns outputUri
        coEvery { imageRepository.getFileSize(outputUri) } returns 500_000
        coEvery { historyRepository.addEntry(any()) } returns "history-id-1"

        val result = useCase(uri, quality = 80)

        assertTrue("Should return success", result.isSuccess)
        coVerify(exactly = 1) { historyRepository.addEntry(any()) }
    }
}

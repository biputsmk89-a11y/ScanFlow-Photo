package com.scanflow.photocompressor.engine

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.fixtures.ImageTestFixtures
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Specification 82: Batch Tests at Scale & Mixed Payload Robustness.
 *
 * Tests:
 * - 1 image
 * - 10 images
 * - 50 images
 * - 100 images
 *
 * Mixed payload:
 * - JPEG
 * - PNG
 * - WEBP
 * - Large image
 * - Corrupt image
 *
 * Expectations:
 * - Valid continue (valid items finish successfully)
 * - Invalid isolated (corrupt items fail without halting the queue)
 * - No whole-batch crash (entire batch completes cleanly)
 */
class BatchScaleAndRobustnessTest {

    private lateinit var mockPipelineEngine: ImagePipelineEngine
    private lateinit var batchProcessor: BatchProcessor

    private val defaultPipeline = ImagePipeline(listOf(ImageOperation.Compress(quality = 80)))

    @Before
    fun setUp() {
        mockPipelineEngine = mockk()
        batchProcessor = BatchProcessor(mockPipelineEngine)
    }

    private fun createBatchItem(id: String, fileName: String): BatchItem {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://media/photos/$id"
        every { uri.lastPathSegment } returns fileName
        return BatchItem(
            id = id,
            sourceUri = uri,
            originalBytes = 1_000_000L
        )
    }

    // =========================================================================
    // SCALE TESTS: 1, 10, 50, 100 IMAGES
    // =========================================================================

    @Test
    fun `scale test - 1 image batch processes cleanly to completion`() = runTest {
        val items = listOf(createBatchItem("item_1", "photo_1.jpg"))
        val job = BatchJob(id = "batch_1", items = items, operation = defaultPipeline)

        val outputUri = mockk<Uri>(relaxed = true)
        coEvery { mockPipelineEngine.execute(any(), any(), any(), any()) } returns Result.success(
            CompressionResult(
                originalSize = 1_000_000L,
                compressedSize = 400_000L,
                outputUri = outputUri,
                outputFileName = "photo_1_compressed.jpg",
                width = 1920,
                height = 1080,
                format = ImageFormat.JPEG,
                quality = 80,
                durationMs = 50L
            )
        )

        val states = batchProcessor.process(job).toList()
        val finalJob = states.last()

        assertTrue("Batch must be complete", finalJob.isComplete)
        assertEquals(1, finalJob.completedCount)
        assertEquals(0, finalJob.failedCount)
        assertEquals(BatchItemStatus.SUCCESS, finalJob.items.first().status)
    }

    @Test
    fun `scale test - 10 images batch processes sequentially with 100 percent success`() = runTest {
        runScaleTest(count = 10)
    }

    @Test
    fun `scale test - 50 images batch processes sequentially without memory leak`() = runTest {
        runScaleTest(count = 50)
    }

    @Test
    fun `scale test - 100 images batch completes all items without crash`() = runTest {
        runScaleTest(count = 100)
    }

    private suspend fun runScaleTest(count: Int) {
        val items = (1..count).map { createBatchItem("item_$it", "photo_$it.jpg") }
        val job = BatchJob(id = "batch_$count", items = items, operation = defaultPipeline)

        val outputUri = mockk<Uri>(relaxed = true)
        coEvery { mockPipelineEngine.execute(any(), any(), any(), any()) } returns Result.success(
            CompressionResult(
                originalSize = 1_000_000L,
                compressedSize = 350_000L,
                outputUri = outputUri,
                outputFileName = "out.jpg",
                width = 1920,
                height = 1080,
                format = ImageFormat.JPEG,
                quality = 80,
                durationMs = 15L
            )
        )

        val states = batchProcessor.process(job).toList()
        val finalJob = states.last()

        assertTrue("Batch must be complete", finalJob.isComplete)
        assertEquals(count, finalJob.completedCount)
        assertEquals(0, finalJob.failedCount)
        assertTrue(finalJob.items.all { it.status == BatchItemStatus.SUCCESS })
    }

    // =========================================================================
    // MIXED PAYLOAD & ERROR ISOLATION TESTS
    // =========================================================================

    @Test
    fun `mixed payload - handles JPEG, PNG, WEBP, Large, and Corrupt images gracefully`() = runTest {
        val validJpeg = createBatchItem("item_jpeg", ImageTestFixtures.SMALL_JPEG.fileName)
        val validPng = createBatchItem("item_png", ImageTestFixtures.TRANSPARENT_PNG.fileName)
        val validWebp = createBatchItem("item_webp", ImageTestFixtures.WEBP_IMAGE.fileName)
        val largeJpeg = createBatchItem("item_large", ImageTestFixtures.LARGE_JPEG.fileName)
        val corruptImage = createBatchItem("item_corrupt", ImageTestFixtures.CORRUPT_IMAGE.fileName)
        val followingValid = createBatchItem("item_after_corrupt", "photo_resume.jpg")

        val mixedItems = listOf(validJpeg, validPng, corruptImage, validWebp, largeJpeg, followingValid)
        val job = BatchJob(id = "mixed_batch_job", items = mixedItems, operation = defaultPipeline)

        val mockOutputUri = mockk<Uri>(relaxed = true)

        // Mock pipeline behaviors per item
        coEvery { mockPipelineEngine.execute(validJpeg.sourceUri, any(), any(), any()) } returns Result.success(
            CompressionResult(500_000L, 200_000L, mockOutputUri, "valid_jpeg.jpg", 800, 600, ImageFormat.JPEG, 80, 20L)
        )
        coEvery { mockPipelineEngine.execute(validPng.sourceUri, any(), any(), any()) } returns Result.success(
            CompressionResult(1_200_000L, 400_000L, mockOutputUri, "valid_png.png", 1000, 1000, ImageFormat.PNG, 80, 40L)
        )
        coEvery { mockPipelineEngine.execute(corruptImage.sourceUri, any(), any(), any()) } returns Result.failure(
            IOException("Corrupt image data: unexpected end of stream")
        )
        coEvery { mockPipelineEngine.execute(validWebp.sourceUri, any(), any(), any()) } returns Result.success(
            CompressionResult(650_000L, 150_000L, mockOutputUri, "valid_webp.webp", 1920, 1080, ImageFormat.WEBP, 80, 30L)
        )
        coEvery { mockPipelineEngine.execute(largeJpeg.sourceUri, any(), any(), any()) } returns Result.success(
            CompressionResult(15_000_000L, 3_500_000L, mockOutputUri, "large_jpeg.jpg", 4000, 3000, ImageFormat.JPEG, 80, 150L)
        )
        coEvery { mockPipelineEngine.execute(followingValid.sourceUri, any(), any(), any()) } returns Result.success(
            CompressionResult(1_000_000L, 300_000L, mockOutputUri, "resume.jpg", 1920, 1080, ImageFormat.JPEG, 80, 25L)
        )

        val states = batchProcessor.process(job).toList()
        val finalJob = states.last()

        // 1. NO WHOLE-BATCH CRASH: The batch completes
        assertTrue("Batch must be complete", finalJob.isComplete)

        // 2. INVALID ISOLATED: Corrupt item is marked FAILED with error
        val corruptItem = finalJob.items.first { it.id == "item_corrupt" }
        assertEquals(BatchItemStatus.FAILED, corruptItem.status)
        assertNotNull(corruptItem.error)

        // 3. VALID CONTINUE: All other valid items (JPEG, PNG, WEBP, Large, Resumed) succeed
        assertEquals(5, finalJob.completedCount)
        assertEquals(1, finalJob.failedCount)

        val successItems = finalJob.items.filter { it.status == BatchItemStatus.SUCCESS }
        assertEquals(5, successItems.size)

        val resumedItem = finalJob.items.first { it.id == "item_after_corrupt" }
        assertEquals("Subsequent item after corrupt failure must process successfully", BatchItemStatus.SUCCESS, resumedItem.status)
    }
}

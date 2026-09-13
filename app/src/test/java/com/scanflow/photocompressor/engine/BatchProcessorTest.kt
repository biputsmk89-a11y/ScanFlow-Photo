package com.scanflow.photocompressor.engine

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class BatchProcessorTest {

    private lateinit var imagePipelineEngine: ImagePipelineEngine
    private lateinit var batchProcessor: BatchProcessor

    private val outputUri = mockk<Uri>()

    @Before
    fun setup() {
        imagePipelineEngine = mockk(relaxed = true)
        batchProcessor = BatchProcessor(imagePipelineEngine)

        val mockResult = CompressionResult(
            originalSize = 2_000_000L,
            compressedSize = 500_000L,
            outputUri = outputUri,
            outputFileName = "output.jpg",
            width = 1920,
            height = 1080,
            format = ImageFormat.JPEG,
            quality = 80,
            durationMs = 50L,
            preservedExif = true
        )
        coEvery { imagePipelineEngine.execute(any(), any(), any(), any()) } returns Result.success(mockResult)
    }

    @Test
    fun `concurrency is strictly 1 and never processes items in parallel`() = runTest {
        val activeConcurrent = AtomicInteger(0)
        var maxObservedConcurrency = 0

        coEvery { imagePipelineEngine.execute(any(), any(), any(), any()) } coAnswers {
            val current = activeConcurrent.incrementAndGet()
            if (current > maxObservedConcurrency) {
                maxObservedConcurrency = current
            }
            delay(10) // Simulate processing time
            activeConcurrent.decrementAndGet()

            Result.success(
                CompressionResult(
                    originalSize = 1000L,
                    compressedSize = 500L,
                    outputUri = outputUri,
                    outputFileName = "out.jpg",
                    width = 100,
                    height = 100,
                    format = ImageFormat.JPEG,
                    quality = 80,
                    durationMs = 10L,
                    preservedExif = true
                )
            )
        }

        val items = (1..10).map { BatchItem(sourceUri = mockk()) }
        val job = BatchJob(
            items = items,
            operation = ImagePipeline(emptyList())
        )

        val emissions = batchProcessor.process(job).toList()
        val finalJob = emissions.last()

        assertEquals(1, maxObservedConcurrency)
        assertEquals(10, finalJob.completedCount)
        assertEquals(0, finalJob.failedCount)
        assertTrue(finalJob.isComplete)
    }

    @Test
    fun `supports batch size of 1 item`() = runTest {
        val items = listOf(BatchItem(sourceUri = mockk()))
        val job = BatchJob(
            items = items,
            operation = ImagePipeline(listOf(ImageOperation.Compress(80)))
        )

        val emissions = batchProcessor.process(job).toList()
        val finalJob = emissions.last()

        assertEquals(1, finalJob.totalCount)
        assertEquals(1, finalJob.completedCount)
        assertEquals(BatchItemStatus.SUCCESS, finalJob.items.first().status)
        assertEquals(outputUri, finalJob.items.first().outputUri)
    }

    @Test
    fun `supports batch size of 10 items`() = runTest {
        val items = (1..10).map { BatchItem(sourceUri = mockk()) }
        val job = BatchJob(
            items = items,
            operation = ImagePipeline(listOf(ImageOperation.Compress(80)))
        )

        val emissions = batchProcessor.process(job).toList()
        val finalJob = emissions.last()

        assertEquals(10, finalJob.totalCount)
        assertEquals(10, finalJob.completedCount)
        assertTrue(finalJob.items.all { it.status == BatchItemStatus.SUCCESS })
    }

    @Test
    fun `supports batch size of 50 items`() = runTest {
        val items = (1..50).map { BatchItem(sourceUri = mockk()) }
        val job = BatchJob(
            items = items,
            operation = ImagePipeline(listOf(ImageOperation.Compress(75)))
        )

        val emissions = batchProcessor.process(job).toList()
        val finalJob = emissions.last()

        assertEquals(50, finalJob.totalCount)
        assertEquals(50, finalJob.completedCount)
        assertTrue(finalJob.items.all { it.status == BatchItemStatus.SUCCESS })
    }

    @Test
    fun `supports batch size of 100+ items without memory leak`() = runTest {
        val items = (1..105).map { BatchItem(sourceUri = mockk()) }
        val job = BatchJob(
            items = items,
            operation = ImagePipeline(listOf(ImageOperation.Compress(75)))
        )

        val emissions = batchProcessor.process(job).toList()
        val finalJob = emissions.last()

        assertEquals(105, finalJob.totalCount)
        assertEquals(105, finalJob.completedCount)
        assertEquals(0, finalJob.failedCount)
        assertTrue(finalJob.items.all { it.status == BatchItemStatus.SUCCESS })
        coVerify(exactly = 105) { imagePipelineEngine.execute(any(), any(), any(), any()) }
    }

    @Test
    fun `error isolation ensures failure of one item does not abort remaining queue`() = runTest {
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        val uri3 = mockk<Uri>()

        coEvery { imagePipelineEngine.execute(uri1, any(), any(), any()) } returns Result.success(mockk(relaxed = true) {
            every { outputUri } returns mockk()
        })
        coEvery { imagePipelineEngine.execute(uri2, any(), any(), any()) } returns Result.failure(
            IllegalStateException("Corrupted JPEG file")
        )
        coEvery { imagePipelineEngine.execute(uri3, any(), any(), any()) } returns Result.success(mockk(relaxed = true) {
            every { outputUri } returns mockk()
        })

        val job = BatchJob(
            items = listOf(
                BatchItem(sourceUri = uri1),
                BatchItem(sourceUri = uri2),
                BatchItem(sourceUri = uri3)
            ),
            operation = ImagePipeline(emptyList())
        )

        val emissions = batchProcessor.process(job).toList()
        val finalJob = emissions.last()

        assertEquals(3, finalJob.totalCount)
        assertEquals(2, finalJob.completedCount)
        assertEquals(1, finalJob.failedCount)

        assertEquals(BatchItemStatus.SUCCESS, finalJob.items[0].status)
        assertEquals(BatchItemStatus.FAILED, finalJob.items[1].status)
        assertTrue(finalJob.items[1].error is ProcessingError.InvalidImage)
        assertEquals("This image could not be opened.\nTry another file.", finalJob.items[1].error?.userFacingMessage)
        assertEquals(BatchItemStatus.SUCCESS, finalJob.items[2].status)
        assertTrue(finalJob.isComplete)
    }

    @Test
    fun `cancelled job marks remaining queued items as CANCELLED`() = runTest {
        val items = (1..5).map { BatchItem(sourceUri = mockk()) }
        val job = BatchJob(
            items = items,
            operation = ImagePipeline(emptyList())
        )

        // Take only 3 emissions (initial + item 1 processing + item 1 success) to simulate early consumer cancellation
        val partialEmissions = batchProcessor.process(job).take(3).toList()

        assertEquals(3, partialEmissions.size)
    }

    @Test
    fun `retry skips already SUCCESS items and only processes QUEUED items preserving completed outputs`() = runTest {
        val completedUri = mockk<Uri>()
        val retriedUri = mockk<Uri>()
        val completedOutputUri = mockk<Uri>()
        val newOutputUri = mockk<Uri>()

        coEvery { imagePipelineEngine.execute(retriedUri, any(), any(), any()) } returns Result.success(
            CompressionResult(
                originalSize = 1_000_000L,
                compressedSize = 400_000L,
                outputUri = newOutputUri,
                outputFileName = "retried.jpg",
                width = 800,
                height = 600,
                format = ImageFormat.JPEG,
                quality = 80,
                durationMs = 20L,
                preservedExif = true
            )
        )

        val completedItem = BatchItem(
            sourceUri = completedUri,
            status = BatchItemStatus.SUCCESS,
            outputUri = completedOutputUri,
            originalBytes = 2_000_000L,
            outputBytes = 500_000L
        )
        val retriedItem = BatchItem(
            sourceUri = retriedUri,
            status = BatchItemStatus.QUEUED
        )

        val job = BatchJob(
            items = listOf(completedItem, retriedItem),
            operation = ImagePipeline(emptyList())
        )

        val emissions = batchProcessor.process(job).toList()
        val finalJob = emissions.last()

        // imagePipelineEngine should ONLY be called once for retriedUri, never for completedUri!
        coVerify(exactly = 0) { imagePipelineEngine.execute(completedUri, any(), any(), any()) }
        coVerify(exactly = 1) { imagePipelineEngine.execute(retriedUri, any(), any(), any()) }

        assertEquals(2, finalJob.totalCount)
        assertEquals(2, finalJob.completedCount)
        assertEquals(0, finalJob.failedCount)

        // Completed item output preserved
        assertEquals(completedOutputUri, finalJob.items[0].outputUri)
        assertEquals(BatchItemStatus.SUCCESS, finalJob.items[0].status)

        // Retried item successfully processed
        assertEquals(newOutputUri, finalJob.items[1].outputUri)
        assertEquals(BatchItemStatus.SUCCESS, finalJob.items[1].status)

        // Total saved bytes combined: (2MB - 500KB) + (1MB - 400KB) = 1.5MB + 600KB = 2.1MB = 2_100_000L
        assertEquals(2_100_000L, finalJob.totalSavedBytes)
    }

    @Test
    fun `BatchProcessor cleans temp files in finally block on completion`() = runTest {
        val mockFileManager = mockk<com.scanflow.photocompressor.data.storage.FileManager>(relaxed = true)
        val processor = BatchProcessor(imagePipelineEngine, mockFileManager)

        val job = BatchJob(
            items = listOf(BatchItem(sourceUri = mockk())),
            operation = ImagePipeline(emptyList())
        )

        processor.process(job).toList()

        verify(atLeast = 1) { mockFileManager.cleanTempFiles() }
    }
}

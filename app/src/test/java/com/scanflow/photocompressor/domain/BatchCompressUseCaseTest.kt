package com.scanflow.photocompressor.domain

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.usecase.BatchCompressUseCase
import com.scanflow.photocompressor.engine.BatchProcessor
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BatchCompressUseCaseTest {

    private lateinit var useCase: BatchCompressUseCase
    private lateinit var batchProcessor: BatchProcessor

    @Before
    fun setup() {
        batchProcessor = mockk()
        useCase = BatchCompressUseCase(batchProcessor)
    }

    @Test
    fun `invoke with images and preset converts preset to pipeline and delegates to processor`() = runTest {
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        val images = listOf(
            ImageInfo(
                uri = uri1,
                fileName = "img1.jpg",
                fileSize = 1000L,
                width = 100,
                height = 100,
                format = ImageFormat.JPEG,
                mimeType = "image/jpeg"
            ),
            ImageInfo(
                uri = uri2,
                fileName = "img2.jpg",
                fileSize = 2000L,
                width = 200,
                height = 200,
                format = ImageFormat.JPEG,
                mimeType = "image/jpeg"
            )
        )

        val capturedJob = slot<BatchJob>()
        val mockJobOutput = BatchJob(
            items = listOf(
                BatchItem(sourceUri = uri1, status = BatchItemStatus.SUCCESS),
                BatchItem(sourceUri = uri2, status = BatchItemStatus.SUCCESS)
            ),
            operation = ImagePipeline(listOf())
        )

        every { batchProcessor.process(capture(capturedJob)) } returns flowOf(mockJobOutput)

        val results = useCase(images, CompressionPreset.BALANCED).toList()

        assertEquals(1, results.size)
        assertEquals(2, capturedJob.captured.items.size)
        assertEquals(uri1, capturedJob.captured.items[0].sourceUri)
        assertEquals(uri2, capturedJob.captured.items[1].sourceUri)

        // Verify pipeline operations
        val ops = capturedJob.captured.operation.operations
        assertTrue("Contains Compress operation", ops.any { it is ImageOperation.Compress && it.quality == CompressionPreset.BALANCED.quality })
        assertTrue("Contains Convert operation", ops.any { it is ImageOperation.Convert && it.format == CompressionPreset.BALANCED.format })
    }

    @Test
    fun `invoke with uris and pipeline creates BatchJob and delegates`() = runTest {
        val uri = mockk<Uri>()
        val pipeline = ImagePipeline(listOf(ImageOperation.Compress(80)))

        val capturedJob = slot<BatchJob>()
        val mockJobOutput = BatchJob(
            items = listOf(BatchItem(sourceUri = uri, status = BatchItemStatus.SUCCESS)),
            operation = pipeline
        )

        every { batchProcessor.process(capture(capturedJob)) } returns flowOf(mockJobOutput)

        val results = useCase(listOf(uri), pipeline).toList()

        assertEquals(1, results.size)
        assertEquals(1, capturedJob.captured.items.size)
        assertEquals(uri, capturedJob.captured.items[0].sourceUri)
        assertEquals(pipeline, capturedJob.captured.operation)
    }

    @Test
    fun `invoke with BatchJob delegates directly to processor`() = runTest {
        val job = BatchJob(
            items = emptyList(),
            operation = ImagePipeline(emptyList())
        )

        every { batchProcessor.process(job) } returns flowOf(job)

        val results = useCase(job).toList()

        assertEquals(1, results.size)
        assertEquals(job, results.first())
        verify(exactly = 1) { batchProcessor.process(job) }
    }
}

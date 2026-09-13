package com.scanflow.photocompressor.engine

import android.net.Uri
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.model.*
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BatchMemoryPolicyTest {

    private val pipelineEngine: ImagePipelineEngine = mockk(relaxed = true)
    private val fileManager: FileManager = mockk(relaxed = true)

    private lateinit var batchProcessor: BatchProcessor

    @Before
    fun setup() {
        batchProcessor = BatchProcessor(
            imagePipelineEngine = pipelineEngine,
            fileManager = fileManager
        )
    }

    @Test
    fun `default concurrency is strictly 1 per policy 62`() {
        assertEquals(1, BatchProcessor.DEFAULT_CONCURRENCY)
    }

    @Test
    fun `batch processing executes sequentially item by item without pre-decoding all items`() = runTest {
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        val uri3 = mockk<Uri>()

        val job = BatchJob(
            items = listOf(
                BatchItem(id = "1", sourceUri = uri1),
                BatchItem(id = "2", sourceUri = uri2),
                BatchItem(id = "3", sourceUri = uri3)
            ),
            operation = ImagePipeline(emptyList())
        )

        val executionOrder = mutableListOf<String>()

        coEvery { pipelineEngine.execute(uri1, any(), any(), any()) } answers {
            executionOrder.add("item1_processed")
            Result.success(mockk(relaxed = true))
        }
        coEvery { pipelineEngine.execute(uri2, any(), any(), any()) } answers {
            executionOrder.add("item2_processed")
            Result.success(mockk(relaxed = true))
        }
        coEvery { pipelineEngine.execute(uri3, any(), any(), any()) } answers {
            executionOrder.add("item3_processed")
            Result.success(mockk(relaxed = true))
        }

        every { fileManager.cleanTempFiles() } answers {
            executionOrder.add("temp_released")
        }

        val emissions = batchProcessor.process(job).toList()

        val finalJob = emissions.last()
        assertTrue(finalJob.isComplete)
        assertEquals(3, finalJob.completedCount)

        // Verify the sequential lifecycle: read one -> decode -> process -> encode -> save -> release -> next
        coVerifyOrder {
            pipelineEngine.execute(uri1, any(), any(), any())
            fileManager.cleanTempFiles()
            pipelineEngine.execute(uri2, any(), any(), any())
            fileManager.cleanTempFiles()
            pipelineEngine.execute(uri3, any(), any(), any())
            fileManager.cleanTempFiles()
        }
    }
}

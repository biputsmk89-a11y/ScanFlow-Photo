package com.scanflow.photocompressor.presentation

import android.net.Uri
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.BatchCompressUseCase
import com.scanflow.photocompressor.domain.usecase.ManagePresetsUseCase
import com.scanflow.photocompressor.ui.batch.BatchViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BatchViewModelTest {

    private lateinit var viewModel: BatchViewModel
    private lateinit var batchCompressUseCase: BatchCompressUseCase
    private lateinit var imageRepository: ImageRepository
    private lateinit var managePresetsUseCase: ManagePresetsUseCase
    private lateinit var fileManager: FileManager
    private val testDispatcher = StandardTestDispatcher()

    private val samplePreset = CompressionPreset(
        id = 1L,
        name = "Balanced",
        quality = 80,
        format = ImageFormat.JPEG,
        preserveExif = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        batchCompressUseCase = mockk(relaxed = true)
        imageRepository = mockk(relaxed = true)
        managePresetsUseCase = mockk(relaxed = true)
        fileManager = mockk(relaxed = true)

        every { managePresetsUseCase.getAllPresets() } returns flowOf(listOf(samplePreset))

        viewModel = BatchViewModel(
            batchCompressUseCase = batchCompressUseCase,
            imageRepository = imageRepository,
            managePresetsUseCase = managePresetsUseCase,
            fileManager = fileManager
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cancelBatch stops pending and active items, preserves completed items, and triggers temp cleanup`() = runTest {
        val completedUri = mockk<Uri>()
        val processingUri = mockk<Uri>()
        val queuedUri = mockk<Uri>()

        val item1 = BatchItem(
            id = "1",
            sourceUri = completedUri,
            status = BatchItemStatus.SUCCESS,
            outputUri = mockk()
        )
        val item2 = BatchItem(
            id = "2",
            sourceUri = processingUri,
            status = BatchItemStatus.PROCESSING
        )
        val item3 = BatchItem(
            id = "3",
            sourceUri = queuedUri,
            status = BatchItemStatus.QUEUED
        )

        val activeJob = BatchJob(
            items = listOf(item1, item2, item3),
            operation = ImagePipeline(emptyList())
        )

        every { batchCompressUseCase(any<BatchJob>()) } returns flowOf(activeJob)

        // Set state with preset and images
        viewModel.selectPreset(samplePreset)

        coEvery { imageRepository.getImageInfo(any()) } returns ImageInfo(
            uri = mockk(),
            fileName = "img.jpg",
            fileSize = 1000L,
            width = 100,
            height = 100,
            format = ImageFormat.JPEG,
            mimeType = "image/jpeg"
        )
        viewModel.addImages(listOf(mockk(), mockk(), mockk()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startBatch()
        testDispatcher.scheduler.advanceUntilIdle()

        // Cancel batch
        viewModel.cancelBatch()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertNotNull(state.batchJob)

        val finalItems = state.batchJob!!.items
        // Item 1 (SUCCESS) preserved
        assertEquals(BatchItemStatus.SUCCESS, finalItems[0].status)
        // Item 2 and 3 became CANCELLED
        assertEquals(BatchItemStatus.CANCELLED, finalItems[1].status)
        assertEquals(BatchItemStatus.CANCELLED, finalItems[2].status)

        // Verify temp file cleanup was called
        verify(atLeast = 1) { fileManager.cleanTempFiles() }
    }

    @Test
    fun `retryBatch resets only failed or cancelled items to QUEUED, preserving completed items`() = runTest {
        val completedOutputUri = mockk<Uri>()
        val completedItem = BatchItem(
            id = "item-1",
            sourceUri = mockk(),
            status = BatchItemStatus.SUCCESS,
            outputUri = completedOutputUri,
            originalBytes = 1000L,
            outputBytes = 500L
        )
        val failedItem = BatchItem(
            id = "item-2",
            sourceUri = mockk(),
            status = BatchItemStatus.FAILED,
            error = ProcessingError.InvalidImage
        )
        val cancelledItem = BatchItem(
            id = "item-3",
            sourceUri = mockk(),
            status = BatchItemStatus.CANCELLED
        )

        val batchJobWithFailures = BatchJob(
            items = listOf(completedItem, failedItem, cancelledItem),
            operation = ImagePipeline(emptyList())
        )

        // Set up the job in state
        viewModel.selectPreset(samplePreset)
        coEvery { imageRepository.getImageInfo(any()) } returns ImageInfo(
            uri = mockk(),
            fileName = "img.jpg",
            fileSize = 1000L,
            width = 100,
            height = 100,
            format = ImageFormat.JPEG,
            mimeType = "image/jpeg"
        )
        viewModel.addImages(listOf(mockk()))
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate that a batch run resulted in batchJobWithFailures
        every { batchCompressUseCase(any<BatchJob>()) } returns flowOf(batchJobWithFailures)
        viewModel.startBatch()
        testDispatcher.scheduler.advanceUntilIdle()

        val capturedJobSlot = slot<BatchJob>()
        every { batchCompressUseCase(capture(capturedJobSlot)) } returns flowOf()

        // Now trigger retry
        viewModel.retryBatch()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the job submitted to use case:
        assertTrue(capturedJobSlot.isCaptured)
        val retriedJob = capturedJobSlot.captured

        // Item 1 (SUCCESS) preserved
        assertEquals(BatchItemStatus.SUCCESS, retriedJob.items[0].status)
        assertEquals(completedOutputUri, retriedJob.items[0].outputUri)

        // Item 2 (FAILED) reset to QUEUED, error cleared
        assertEquals(BatchItemStatus.QUEUED, retriedJob.items[1].status)
        assertNull(retriedJob.items[1].error)

        // Item 3 (CANCELLED) reset to QUEUED, error cleared
        assertEquals(BatchItemStatus.QUEUED, retriedJob.items[2].status)
        assertNull(retriedJob.items[2].error)
    }

    @Test
    fun `retryItem resets only the targeted item to QUEUED`() = runTest {
        val completedItem = BatchItem(
            id = "item-1",
            sourceUri = mockk(),
            status = BatchItemStatus.SUCCESS
        )
        val failedItem1 = BatchItem(
            id = "item-2",
            sourceUri = mockk(),
            status = BatchItemStatus.FAILED,
            error = ProcessingError.InvalidImage
        )
        val failedItem2 = BatchItem(
            id = "item-3",
            sourceUri = mockk(),
            status = BatchItemStatus.FAILED,
            error = ProcessingError.OutOfMemory
        )

        val batchJob = BatchJob(
            items = listOf(completedItem, failedItem1, failedItem2),
            operation = ImagePipeline(emptyList())
        )

        every { batchCompressUseCase(any<BatchJob>()) } returns flowOf(batchJob)
        viewModel.selectPreset(samplePreset)
        coEvery { imageRepository.getImageInfo(any()) } returns ImageInfo(
            uri = mockk(), fileName = "a.jpg", fileSize = 10L, width = 10, height = 10, format = ImageFormat.JPEG, mimeType = "image/jpeg"
        )
        viewModel.addImages(listOf(mockk()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startBatch()
        testDispatcher.scheduler.advanceUntilIdle()

        val capturedJobSlot = slot<BatchJob>()
        every { batchCompressUseCase(capture(capturedJobSlot)) } returns flowOf()

        // Retry ONLY item-2
        viewModel.retryItem("item-2")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(capturedJobSlot.isCaptured)
        val retriedJob = capturedJobSlot.captured

        // item-1 stays SUCCESS
        assertEquals(BatchItemStatus.SUCCESS, retriedJob.items[0].status)
        // item-2 reset to QUEUED
        assertEquals(BatchItemStatus.QUEUED, retriedJob.items[1].status)
        assertNull(retriedJob.items[1].error)
        // item-3 stays FAILED (not retried)
        assertEquals(BatchItemStatus.FAILED, retriedJob.items[2].status)
    }
}

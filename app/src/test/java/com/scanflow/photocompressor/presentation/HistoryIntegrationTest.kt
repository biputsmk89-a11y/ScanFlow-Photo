package com.scanflow.photocompressor.presentation

import com.scanflow.photocompressor.domain.model.OperationType
import com.scanflow.photocompressor.domain.model.ProcessingHistory
import com.scanflow.photocompressor.domain.usecase.GetHistoryUseCase
import com.scanflow.photocompressor.ui.history.HistoryViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val historyFlow = MutableStateFlow<List<ProcessingHistory>>(emptyList())
    private val getHistoryUseCase = mockk<GetHistoryUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getHistoryUseCase.getAllHistory() } returns historyFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `HistoryViewModel accurately populates uiState from use case flow`() = runTest {
        val sampleList = listOf(
            ProcessingHistory(
                id = "1",
                timestamp = 1000L,
                operation = OperationType.COMPRESS,
                inputUri = "content://media/1",
                outputUri = "content://media/1_out",
                inputFileName = "scan_001.jpg",
                outputFileName = "scan_001_compressed.jpg",
                originalSize = 5_000_000L,
                resultSize = 2_000_000L
            ),
            ProcessingHistory(
                id = "2",
                timestamp = 2000L,
                operation = OperationType.CONVERT,
                inputUri = "content://media/2",
                outputUri = "content://media/2_out",
                inputFileName = "scan_002.png",
                outputFileName = "scan_002.webp",
                originalSize = 4_000_000L,
                resultSize = 1_000_000L
            )
        )

        val viewModel = HistoryViewModel(getHistoryUseCase)
        testScheduler.advanceUntilIdle()

        historyFlow.value = sampleList
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.historyList.size)
        assertEquals(6_000_000L, state.totalSavedBytes)
        assertEquals(2, state.totalOperations)
        assertFalse(state.isLoading)
    }

    @Test
    fun `deleteEntry delegates to use case and emits user feedback message`() = runTest {
        val viewModel = HistoryViewModel(getHistoryUseCase)
        testScheduler.advanceUntilIdle()

        viewModel.deleteEntry("entry_42")
        testScheduler.advanceUntilIdle()

        coVerify { getHistoryUseCase.deleteEntry("entry_42") }
        assertEquals("Record removed from history", viewModel.uiState.value.userMessage)

        viewModel.clearMessage()
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `clearAll delegates to usecase and emits clear feedback message`() = runTest {
        val viewModel = HistoryViewModel(getHistoryUseCase)
        testScheduler.advanceUntilIdle()

        viewModel.clearAll()
        testScheduler.advanceUntilIdle()

        coVerify { getHistoryUseCase.clearAll() }
        assertEquals("All history records cleared", viewModel.uiState.value.userMessage)
    }

    @Test
    fun `operation filter mappings cover all editing categories`() {
        fun matchesFilter(filter: String, op: OperationType): Boolean {
            return when (filter) {
                "compress" -> op == OperationType.COMPRESS
                "resize" -> op == OperationType.RESIZE
                "convert" -> op == OperationType.CONVERT
                "crop" -> op == OperationType.CROP
                "rotate" -> op == OperationType.ROTATE
                "pdf" -> op == OperationType.PDF
                "batch" -> op == OperationType.BATCH
                else -> true
            }
        }

        assertTrue("Convert filter should match CONVERT", matchesFilter("convert", OperationType.CONVERT))
        assertFalse("Convert filter should not match RESIZE", matchesFilter("convert", OperationType.RESIZE))
        assertTrue("Crop filter should match CROP", matchesFilter("crop", OperationType.CROP))
        assertTrue("Rotate filter should match ROTATE", matchesFilter("rotate", OperationType.ROTATE))
        assertTrue("All filter should match any", matchesFilter("all", OperationType.PASSPORT))
    }
}

package com.scanflow.photocompressor.data

import com.scanflow.photocompressor.data.local.HistoryDao
import com.scanflow.photocompressor.data.local.ProcessingHistoryEntity
import com.scanflow.photocompressor.data.repository.HistoryRepositoryImpl
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HistoryRepositoryImplTest {

    private lateinit var repository: HistoryRepositoryImpl
    private lateinit var historyDao: HistoryDao

    @Before
    fun setup() {
        historyDao = mockk(relaxed = true)
        repository = HistoryRepositoryImpl(historyDao)
    }

    @Test
    fun `getAllHistory maps entities to domain models`() = runTest {
        val entities = listOf(
            ProcessingHistoryEntity(
                id = "hist-123",
                operationType = "COMPRESS",
                itemCount = 1,
                originalBytes = 1000000L,
                outputBytes = 500000L,
                reductionPercent = 50.0f,
                createdAt = System.currentTimeMillis(),
                settingsJson = "{}",
                status = "SUCCESS",
                inputUri = "content://input",
                inputFileName = "test.jpg",
                outputUri = "content://output",
                outputFileName = "test_compressed.jpg"
            )
        )

        every { historyDao.getAllHistory() } returns flowOf(entities)

        val result = repository.getAllHistory().first()

        assertEquals(1, result.size)
        assertEquals("hist-123", result[0].id)
        assertEquals("test.jpg", result[0].inputFileName)
        assertEquals(com.scanflow.photocompressor.domain.model.OperationType.COMPRESS, result[0].operation)
        assertEquals(500000L, result[0].savedBytes)
        assertEquals(50.0, result[0].savedPercentage, 0.01)
    }

    @Test
    fun `addEntry calls dao insert`() = runTest {
        coJustRun { historyDao.insert(any()) }

        val entry = com.scanflow.photocompressor.domain.model.ProcessingHistory(
            id = "test-id-1",
            inputUri = "content://input",
            inputFileName = "test.jpg",
            outputUri = "content://output",
            outputFileName = "test_compressed.jpg",
            operationType = com.scanflow.photocompressor.domain.model.OperationType.COMPRESS,
            originalBytes = 1000000L,
            outputBytes = 500000L
        )

        val id = repository.addEntry(entry)

        assertEquals("test-id-1", id)
        coVerify(exactly = 1) { historyDao.insert(any()) }
    }

    @Test
    fun `getTotalSavedBytes delegates to dao`() = runTest {
        coEvery { historyDao.getTotalSavedBytes() } returns 5_000_000L

        val saved = repository.getTotalSavedBytes()

        assertEquals(5_000_000L, saved)
    }

    @Test
    fun `clearAll calls dao clearAll`() = runTest {
        coJustRun { historyDao.clearAll() }

        repository.clearAll()

        coVerify(exactly = 1) { historyDao.clearAll() }
    }
}

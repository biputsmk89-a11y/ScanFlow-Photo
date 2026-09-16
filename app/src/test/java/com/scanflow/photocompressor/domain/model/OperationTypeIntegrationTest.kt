package com.scanflow.photocompressor.domain.model

import com.scanflow.photocompressor.data.local.ProcessingHistoryEntity
import com.scanflow.photocompressor.data.local.toDomain
import com.scanflow.photocompressor.data.local.toEntity
import org.junit.Assert.*
import org.junit.Test

class OperationTypeIntegrationTest {

    @Test
    fun `all operation types have valid non-empty display names`() {
        val types = OperationType.values()
        assertEquals(12, types.size)

        types.forEach { type ->
            assertTrue("Display name should not be blank for ${type.name}", type.displayName.isNotBlank())
        }

        assertTrue(types.contains(OperationType.COMPRESS))
        assertTrue(types.contains(OperationType.RESIZE))
        assertTrue(types.contains(OperationType.CROP))
        assertTrue(types.contains(OperationType.ROTATE))
        assertTrue(types.contains(OperationType.FLIP))
        assertTrue(types.contains(OperationType.WATERMARK))
        assertTrue(types.contains(OperationType.CONVERT))
        assertTrue(types.contains(OperationType.BATCH))
        assertTrue(types.contains(OperationType.PDF))
        assertTrue(types.contains(OperationType.PASSPORT))
        assertTrue(types.contains(OperationType.SOCIAL))
        assertTrue(types.contains(OperationType.WHATSAPP))
    }

    @Test
    fun `ProcessingHistory supports all new operation types and converts to and from entity`() {
        OperationType.values().forEach { type ->
            val history = ProcessingHistory(
                operationType = type,
                itemCount = 1,
                originalBytes = 2048L,
                outputBytes = 1024L,
                inputFileName = "test_input.jpg",
                outputFileName = "test_output.jpg"
            )

            assertEquals(type, history.operation)
            assertEquals(50.0, history.savedPercentage, 0.1)

            val entity = history.toEntity()
            assertEquals(type.name, entity.operationType)

            val roundTrip = entity.toDomain()
            assertEquals(type, roundTrip.operationType)
        }
    }

    @Test
    fun `ProcessingHistory toDomain falls back gracefully on unknown operation type string`() {
        val entity = ProcessingHistoryEntity(
            id = "test-id",
            operationType = "UNKNOWN_FUTURE_OP",
            itemCount = 1,
            originalBytes = 1000L,
            outputBytes = 500L,
            reductionPercent = 50.0f,
            createdAt = 12345L,
            settingsJson = null,
            status = "SUCCESS",
            inputUri = "file:///input.jpg",
            inputFileName = "input.jpg",
            outputUri = "file:///output.jpg",
            outputFileName = "output.jpg"
        )

        val domain = entity.toDomain()
        assertEquals(OperationType.COMPRESS, domain.operationType)
    }
}

package com.scanflow.photocompressor.domain.model

import org.junit.Assert.*
import org.junit.Test

class ProcessingErrorAndStateTest {

    @Test
    fun `ProcessingError provides safe user-facing message without stack traces`() {
        val invalidImage = ProcessingError.InvalidImage
        assertEquals("This image could not be opened.\nTry another file.", invalidImage.userFacingMessage)

        val oom = ProcessingError.OutOfMemory
        assertEquals("This image is too large to process safely on this device.\nTry reducing its dimensions first.", oom.userFacingMessage)

        val storage = ProcessingError.StorageFull
        assertEquals("There is not enough storage space to save the result.", storage.userFacingMessage)

        val unsupported = ProcessingError.UnsupportedFormat
        assertTrue(unsupported.userFacingMessage.contains("not supported"))

        val permission = ProcessingError.PermissionDenied
        assertTrue(permission.userFacingMessage.contains("Permission was denied"))

        val cancelled = ProcessingError.ProcessingCancelled
        assertEquals("Processing was cancelled.", cancelled.userFacingMessage)

        val unknown = ProcessingError.Unknown("Custom reason")
        assertEquals("Custom reason", unknown.userFacingMessage)
    }

    @Test
    fun `fromThrowable maps OutOfMemoryError to ProcessingError_OutOfMemory`() {
        val oom = OutOfMemoryError("Failed to allocate bitmap")
        val error = ProcessingError.fromThrowable(oom)
        assertTrue(error is ProcessingError.OutOfMemory)
    }

    @Test
    fun `fromThrowable maps SecurityException to ProcessingError_PermissionDenied`() {
        val sec = SecurityException("Permission Denial")
        val error = ProcessingError.fromThrowable(sec)
        assertTrue(error is ProcessingError.PermissionDenied)
    }

    @Test
    fun `fromThrowable maps CancellationException to ProcessingError_ProcessingCancelled`() {
        val cancel = kotlinx.coroutines.CancellationException("User cancelled")
        val error = ProcessingError.fromThrowable(cancel)
        assertTrue(error is ProcessingError.ProcessingCancelled)
    }

    @Test
    fun `ProcessingState instances transition correctly`() {
        val idle: ProcessingState = ProcessingState.Idle
        val analyzing: ProcessingState = ProcessingState.Analyzing
        val processing: ProcessingState = ProcessingState.Processing(0.45f)
        val cancelled: ProcessingState = ProcessingState.Cancelled
        val failed: ProcessingState = ProcessingState.Failed(ProcessingError.StorageFull)

        assertTrue(idle is ProcessingState.Idle)
        assertTrue(analyzing is ProcessingState.Analyzing)
        assertEquals(0.45f, (processing as ProcessingState.Processing).progress, 0.001f)
        assertTrue(cancelled is ProcessingState.Cancelled)
        assertEquals(ProcessingError.StorageFull, (failed as ProcessingState.Failed).error)
    }
}

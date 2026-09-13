package com.scanflow.photocompressor.failure

import android.net.Uri
import com.scanflow.photocompressor.domain.model.AppPreferences
import com.scanflow.photocompressor.domain.model.ProcessingError
import com.scanflow.photocompressor.domain.model.ProcessingState
import com.scanflow.photocompressor.domain.model.ThemeMode
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.CompressImageUseCase
import com.scanflow.photocompressor.engine.LargeImageRejectedException
import com.scanflow.photocompressor.ui.compress.CompressViewModel
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Specification 84: Failure and Resilience Tests.
 *
 * Simulates real-world mobile failure modes:
 * 1. Storage Full (ENOSPC)
 * 2. Permission Revoked (SecurityException)
 * 3. URI Unavailable (FileNotFoundException)
 * 4. Corrupt Image (Decode failure)
 * 5. Unsupported Format (Invalid MIME / raw DNG)
 * 6. Huge Image (Safe memory rejection > 50MB)
 * 7. Worker Cancellation (CancellationException mid-job)
 * 8. App Process Death (State restoration from defaults/preferences)
 * 9. Duplicate Submission (Rapid double-click debounce / atomic job cancel)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FailureAndResilienceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var compressUseCase: CompressImageUseCase
    private lateinit var imageRepository: ImageRepository
    private lateinit var viewModel: CompressViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        compressUseCase = mockk()
        imageRepository = mockk(relaxed = true)
        viewModel = CompressViewModel(compressUseCase, imageRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. STORAGE FULL
    // =========================================================================
    @Test
    fun `1 - simulate storage full error maps to StorageFull without raw trace`() = runTest {
        val mockUri = mockk<Uri>(relaxed = true)
        val storageException = IOException("ENOSPC: No space left on device")
        coEvery { compressUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.failure(storageException)

        viewModel.selectImages(listOf(mockUri))
        testScheduler.advanceUntilIdle()

        viewModel.compress()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State must be Failed", state.processingState is ProcessingState.Failed)
        assertEquals(ProcessingError.StorageFull, state.typedError)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("not enough storage space"))
        assertFalse("Never expose raw ENOSPC stack trace", state.error!!.contains("ENOSPC"))
    }

    // =========================================================================
    // 2. PERMISSION REVOKED
    // =========================================================================
    @Test
    fun `2 - simulate permission revoked maps to PermissionDenied`() = runTest {
        val mockUri = mockk<Uri>(relaxed = true)
        val permException = SecurityException("Permission denial: reading external storage requires READ_EXTERNAL_STORAGE")
        coEvery { compressUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.failure(permException)

        viewModel.selectImages(listOf(mockUri))
        testScheduler.advanceUntilIdle()

        viewModel.compress()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ProcessingError.PermissionDenied, state.typedError)
        assertTrue(state.error!!.contains("Permission was denied"))
    }

    // =========================================================================
    // 3. URI UNAVAILABLE
    // =========================================================================
    @Test
    fun `3 - simulate URI unavailable maps to InvalidImage`() = runTest {
        val mockUri = mockk<Uri>(relaxed = true)
        val notFoundException = FileNotFoundException("Image file does not exist or was deleted externally")
        coEvery { compressUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.failure(notFoundException)

        viewModel.selectImages(listOf(mockUri))
        testScheduler.advanceUntilIdle()

        viewModel.compress()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ProcessingError.InvalidImage, state.typedError)
        assertTrue(state.error!!.contains("could not be opened"))
    }

    // =========================================================================
    // 4. CORRUPT IMAGE
    // =========================================================================
    @Test
    fun `4 - simulate corrupt image data maps to InvalidImage cleanly`() = runTest {
        val mockUri = mockk<Uri>(relaxed = true)
        val corruptException = IOException("Corrupt image: bitmap could not be decoded from stream")
        coEvery { compressUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.failure(corruptException)

        viewModel.selectImages(listOf(mockUri))
        testScheduler.advanceUntilIdle()

        viewModel.compress()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ProcessingError.InvalidImage, state.typedError)
        assertTrue(state.error!!.contains("could not be opened"))
    }

    // =========================================================================
    // 5. UNSUPPORTED FORMAT
    // =========================================================================
    @Test
    fun `5 - simulate unsupported format maps to UnsupportedFormat`() {
        val unsupportedException = IllegalArgumentException("unsupported format: raw dng is not supported")
        val typedError = ProcessingError.fromThrowable(unsupportedException)

        assertEquals(ProcessingError.UnsupportedFormat, typedError)
        assertTrue(typedError.userFacingMessage.contains("format is not supported"))
    }

    // =========================================================================
    // 6. HUGE IMAGE (> 50 MB)
    // =========================================================================
    @Test
    fun `6 - simulate huge image rejection maps to FileTooLarge with memory safeguard`() = runTest {
        val mockUri = mockk<Uri>(relaxed = true)
        val hugeException = LargeImageRejectedException(
            fileSizeMB = 72.5,
            rawWidth = 10000,
            rawHeight = 8000,
            message = "Image safely rejected: 72.5 MB exceeds device memory limit"
        )
        coEvery { compressUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.failure(hugeException)

        viewModel.selectImages(listOf(mockUri))
        testScheduler.advanceUntilIdle()

        viewModel.compress()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Typed error must be FileTooLarge", state.typedError is ProcessingError.FileTooLarge)
        val fileTooLarge = state.typedError as ProcessingError.FileTooLarge
        assertEquals(72.5, fileTooLarge.fileSizeMB, 0.01)
        assertTrue(state.error!!.contains("too large for device memory"))
    }

    // =========================================================================
    // 7. WORKER CANCELLATION
    // =========================================================================
    @Test
    fun `7 - simulate worker cancellation terminates processing and transitions to Cancelled`() = runTest {
        val mockUri = mockk<Uri>(relaxed = true)
        coEvery { compressUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            delay(10_000)
            throw CancellationException("Worker was cancelled by user")
        }

        viewModel.selectImages(listOf(mockUri))
        testScheduler.advanceUntilIdle()

        viewModel.compress()
        testScheduler.advanceTimeBy(100)
        assertTrue(viewModel.uiState.value.isProcessing)

        // Cancel mid-processing
        viewModel.cancelCompression()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertEquals(ProcessingState.Cancelled, state.processingState)
        assertEquals(ProcessingError.ProcessingCancelled, state.typedError)
    }

    // =========================================================================
    // 8. APP PROCESS DEATH & STATE RESTORATION
    // =========================================================================
    @Test
    fun `8 - simulate process death recovers safely with initial state and preferences`() {
        val freshPrefs = AppPreferences(theme = ThemeMode.SYSTEM, defaultQuality = 80)
        assertEquals(ThemeMode.SYSTEM, freshPrefs.theme)
        assertEquals(80, freshPrefs.defaultQuality)
        assertTrue(freshPrefs.behavior.preserveExif)
        assertTrue(freshPrefs.behavior.keepAspectRatio)

        // New ViewModel instantiation simulates fresh process launch
        val restoredViewModel = CompressViewModel(compressUseCase, imageRepository)
        val state = restoredViewModel.uiState.value

        assertEquals(ProcessingState.Idle, state.processingState)
        assertNull(state.selectedImageUri)
        assertFalse(state.isProcessing)
    }

    // =========================================================================
    // 9. DUPLICATE SUBMISSION
    // =========================================================================
    @Test
    fun `9 - simulate duplicate submission cancels existing job and avoids corrupted concurrency`() = runTest {
        val mockUri = mockk<Uri>(relaxed = true)
        var executeCount = 0
        coEvery { compressUseCase(any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            executeCount++
            delay(500)
            Result.success(mockk(relaxed = true))
        }

        viewModel.selectImages(listOf(mockUri))
        testScheduler.advanceUntilIdle()

        // Double submission in rapid succession
        viewModel.compress()
        viewModel.compress() // Should cancel the first job and launch second cleanly
        testScheduler.advanceUntilIdle()

        // Verify second job finished cleanly without corrupting the state
        val state = viewModel.uiState.value
        assertFalse("Processing should finish", state.isProcessing)
        assertTrue(state.processingState is ProcessingState.Completed)
    }
}

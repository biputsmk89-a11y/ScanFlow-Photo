package com.scanflow.photocompressor.presentation

import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.CompressImageUseCase
import com.scanflow.photocompressor.ui.compress.CompressUiState
import com.scanflow.photocompressor.ui.compress.CompressViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompressViewModelTest {

    private lateinit var viewModel: CompressViewModel
    private lateinit var compressImageUseCase: CompressImageUseCase
    private lateinit var imageRepository: ImageRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        compressImageUseCase = mockk(relaxed = true)
        imageRepository = mockk(relaxed = true)
        viewModel = CompressViewModel(compressImageUseCase, imageRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state defaults to Quick mode with Balanced preset`() {
        val state = viewModel.uiState.value

        assertNull(state.selectedImageUri)
        assertNull(state.imageInfo)
        assertEquals(CompressionMode.QUICK, state.mode)
        assertEquals(QuickPreset.BALANCED, state.quickPreset)
        assertEquals(75, state.quality)
        assertEquals(ImageFormat.JPEG, state.format)
        assertFalse(state.isProcessing)
        assertNull(state.result)
        assertNull(state.error)
    }

    @Test
    fun `setQuickPreset updates preset and quality`() {
        viewModel.setQuickPreset(QuickPreset.SMALL)
        val stateSmall = viewModel.uiState.value
        assertEquals(QuickPreset.SMALL, stateSmall.quickPreset)
        assertEquals(50, stateSmall.quality)
        assertEquals(1280, stateSmall.maxWidth)

        viewModel.setQuickPreset(QuickPreset.HIGH_QUALITY)
        val stateHigh = viewModel.uiState.value
        assertEquals(QuickPreset.HIGH_QUALITY, stateHigh.quickPreset)
        assertEquals(90, stateHigh.quality)
        assertEquals(0, stateHigh.maxWidth)
    }

    @Test
    fun `setQuality updates state and switches mode to Quality`() {
        viewModel.setQuality(60)
        val state = viewModel.uiState.value
        assertEquals(60, state.quality)
        assertEquals(CompressionMode.QUALITY, state.mode)
    }

    @Test
    fun `setTargetSizePreset updates target size and mode`() {
        viewModel.setTargetSizePreset(TargetSizePreset.SIZE_100_KB)
        val state = viewModel.uiState.value
        assertEquals(CompressionMode.TARGET_SIZE, state.mode)
        assertEquals(TargetSizePreset.SIZE_100_KB, state.targetSizePreset)
        assertEquals(100L * 1024L, state.targetSizePreset.bytes)
    }

    @Test
    fun `setCustomTargetSizeKB updates custom size and preset to CUSTOM`() {
        viewModel.setCustomTargetSizeKB("250")
        val state = viewModel.uiState.value
        assertEquals(CompressionMode.TARGET_SIZE, state.mode)
        assertEquals(TargetSizePreset.CUSTOM, state.targetSizePreset)
        assertEquals("250", state.customTargetSizeKB)
    }

    @Test
    fun `setFormat updates state`() {
        viewModel.setFormat(ImageFormat.WEBP)
        assertEquals(ImageFormat.WEBP, viewModel.uiState.value.format)
    }

    @Test
    fun `reset clears all state back to default Balanced`() {
        viewModel.setQuality(50)
        viewModel.setFormat(ImageFormat.PNG)
        viewModel.reset()

        val state = viewModel.uiState.value
        assertEquals(CompressionMode.QUICK, state.mode)
        assertEquals(QuickPreset.BALANCED, state.quickPreset)
        assertEquals(75, state.quality)
        assertEquals(ImageFormat.JPEG, state.format)
        assertNull(state.selectedImageUri)
        assertEquals(ProcessingState.Idle, state.processingState)
    }

    @Test
    fun `Rule 55 - ViewModel state holds selections, config, states and NO large Bitmaps`() {
        val state = viewModel.uiState.value

        // Verifies state class fields contain only metadata, URIs, primitives, configs, and sealed states
        val fields = CompressUiState::class.java.declaredFields
        for (field in fields) {
            assertNotEquals(
                "Rule 55 violation: ViewModel UI state must never hold large android.graphics.Bitmap",
                "android.graphics.Bitmap",
                field.type.name
            )
        }

        // Verify initial processingState is Idle
        assertEquals(ProcessingState.Idle, state.processingState)
    }

    @Test
    fun `cancelCompression sets ProcessingState Cancelled`() {
        viewModel.cancelCompression()
        val state = viewModel.uiState.value
        assertEquals(ProcessingState.Cancelled, state.processingState)
        assertEquals(ProcessingError.ProcessingCancelled, state.typedError)
    }
}

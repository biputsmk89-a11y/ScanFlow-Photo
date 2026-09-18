package com.scanflow.photocompressor.presentation

import android.graphics.Bitmap
import android.net.Uri
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.model.PassportBackground
import com.scanflow.photocompressor.domain.model.PassportSpec
import com.scanflow.photocompressor.engine.BitmapUtils
import com.scanflow.photocompressor.engine.PassportEngine
import com.scanflow.photocompressor.engine.PortraitSegmentationEngine
import com.scanflow.photocompressor.ui.passport.PassportViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class PassportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val passportEngine: PassportEngine = mockk(relaxed = true)
    private val portraitSegmentationEngine: PortraitSegmentationEngine = mockk(relaxed = true)
    private val bitmapUtils: BitmapUtils = mockk(relaxed = true)
    private val fileManager: FileManager = mockk(relaxed = true)

    private lateinit var viewModel: PassportViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers {
            val uriString = firstArg<String>()
            val uriMock = mockk<Uri>()
            every { uriMock.toString() } returns uriString
            uriMock
        }
        every { Uri.fromFile(any()) } answers {
            val file = firstArg<File>()
            val uriMock = mockk<Uri>()
            every { uriMock.toString() } returns "file://${file.path}"
            uriMock
        }

        viewModel = PassportViewModel(
            passportEngine = passportEngine,
            portraitSegmentationEngine = portraitSegmentationEngine,
            bitmapUtils = bitmapUtils,
            fileManager = fileManager
        ).apply {
            ioDispatcher = testDispatcher
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state has no selected image and default config`() {
        val state = viewModel.uiState.value
        assertNull(state.selectedImageUri)
        assertNull(state.cutoutUri)
        assertFalse(state.isSegmenting)
        assertEquals(PassportSpec.PRESET_3X4, state.config.spec)
        assertEquals(PassportBackground.ORIGINAL, state.config.background)
    }

    @Test
    fun `updating background changes config reactively`() {
        viewModel.updateBackground(PassportBackground.RED)
        assertEquals(PassportBackground.RED, viewModel.uiState.value.config.background)

        viewModel.updateBackground(PassportBackground.BLUE)
        assertEquals(PassportBackground.BLUE, viewModel.uiState.value.config.background)

        viewModel.updateBackground(PassportBackground.ORIGINAL)
        assertEquals(PassportBackground.ORIGINAL, viewModel.uiState.value.config.background)
    }

    @Test
    fun `updating spec and zoom modifies state properly`() {
        viewModel.updateSpec(PassportSpec.PRESET_2X3)
        assertEquals(PassportSpec.PRESET_2X3, viewModel.uiState.value.config.spec)

        viewModel.updateZoom(1.5f)
        assertEquals(1.5f, viewModel.uiState.value.config.zoom)

        viewModel.updatePan(10f, -20f)
        assertEquals(10f, viewModel.uiState.value.config.panX)
        assertEquals(-20f, viewModel.uiState.value.config.panY)

        viewModel.autoCenter()
        assertEquals(0f, viewModel.uiState.value.config.panX)
        assertEquals(0f, viewModel.uiState.value.config.panY)
    }

    @Test
    fun `selectImage triggers portrait segmentation and saves cutout`() = runTest(testDispatcher) {
        val dummyUri = mockk<Uri>()
        val dummyBitmap = mockk<Bitmap>(relaxed = true)
        val dummyCutout = mockk<Bitmap>(relaxed = true)
        val tempFile = File.createTempFile("test_cutout", ".png").apply { deleteOnExit() }

        every { bitmapUtils.decodeBitmap(dummyUri, 1200, 1200) } returns dummyBitmap
        coEvery { portraitSegmentationEngine.removeBackground(dummyBitmap) } returns dummyCutout
        every { fileManager.createTempFile("passport_cutout_", "png") } returns tempFile
        every { dummyCutout.compress(Bitmap.CompressFormat.PNG, 100, any<FileOutputStream>()) } returns true

        viewModel.selectImage(dummyUri)

        // Verify image uri is set immediately
        assertEquals(dummyUri, viewModel.uiState.value.selectedImageUri)
        assertTrue(viewModel.uiState.value.isSegmenting)

        // Advance coroutines
        advanceUntilIdle()

        // Verify segmentation finished and cutout is set
        assertFalse(viewModel.uiState.value.isSegmenting)
        assertNotNull(viewModel.uiState.value.cutoutUri)
    }
}

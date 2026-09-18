package com.scanflow.photocompressor.presentation

import android.net.Uri
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.usecase.AddWatermarkUseCase
import com.scanflow.photocompressor.domain.usecase.RotateImageUseCase
import com.scanflow.photocompressor.engine.PdfEngine
import com.scanflow.photocompressor.engine.SocialMediaEngine
import com.scanflow.photocompressor.engine.WhatsAppEngine
import com.scanflow.photocompressor.ui.pdf.PdfViewModel
import com.scanflow.photocompressor.ui.rotate.RotateViewModel
import com.scanflow.photocompressor.ui.social.SocialViewModel
import com.scanflow.photocompressor.ui.watermark.WatermarkViewModel
import com.scanflow.photocompressor.ui.whatsapp.WhatsAppViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class MoreToolsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // =========================================================================
    // 1. PDF DOCUMENT MAKER (PdfViewModel)
    // =========================================================================
    @Test
    fun `PdfViewModel manages page list reordering and configuration updates`() = runTest {
        val pdfEngine = mockk<PdfEngine>(relaxed = true)
        val imageRepo = mockk<ImageRepository>(relaxed = true)
        val historyRepo = mockk<HistoryRepository>(relaxed = true)
        val fileManager = mockk<FileManager>(relaxed = true)

        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        coEvery { imageRepo.getImageInfo(uri1) } returns ImageInfo(uri1, "page1.jpg", 1000L, 100, 100, ImageFormat.JPEG, "image/jpeg")
        coEvery { imageRepo.getImageInfo(uri2) } returns ImageInfo(uri2, "page2.jpg", 2000L, 200, 200, ImageFormat.JPEG, "image/jpeg")

        val viewModel = PdfViewModel(pdfEngine, imageRepo, historyRepo, fileManager)

        viewModel.addImages(listOf(uri1, uri2))
        testScheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(2, state.selectedImages.size)
        assertEquals("page1.jpg", state.selectedImages[0].fileName)
        assertEquals("page2.jpg", state.selectedImages[1].fileName)

        // Move page 2 up
        viewModel.moveImageUp(1)
        state = viewModel.uiState.value
        assertEquals("page2.jpg", state.selectedImages[0].fileName)
        assertEquals("page1.jpg", state.selectedImages[1].fileName)

        // Update configurations
        viewModel.updateTitle("Report_2026")
        viewModel.updatePageSize(PdfPageSize.LETTER)
        viewModel.updateMargin(PdfMargin.SMALL)
        viewModel.updateOrientation(PdfOrientation.LANDSCAPE)
        viewModel.updateQuality(PdfQuality.HIGH)

        val updatedConfig = viewModel.uiState.value.config
        assertEquals("Report_2026", updatedConfig.title)
        assertEquals(PdfPageSize.LETTER, updatedConfig.pageSize)
        assertEquals(PdfMargin.SMALL, updatedConfig.margin)
        assertEquals(PdfOrientation.LANDSCAPE, updatedConfig.orientation)
        assertEquals(PdfQuality.HIGH, updatedConfig.quality)

        // Remove page
        viewModel.removeImage(0)
        assertEquals(1, viewModel.uiState.value.selectedImages.size)
    }

    // =========================================================================
    // 2. SOCIAL MEDIA SIZER (SocialViewModel)
    // =========================================================================
    @Test
    fun `SocialViewModel updates platform and type dynamically with accurate presets`() = runTest {
        val socialEngine = mockk<SocialMediaEngine>(relaxed = true)
        val viewModel = SocialViewModel(socialEngine)

        val mockUri = mockk<Uri>()
        viewModel.selectImage(mockUri)
        assertEquals(mockUri, viewModel.uiState.value.selectedImageUri)

        // Default platform is Instagram
        assertEquals(SocialPlatform.INSTAGRAM, viewModel.uiState.value.selectedPlatform)

        // Switch to TikTok
        viewModel.selectPlatform(SocialPlatform.TIKTOK)
        assertEquals(SocialPlatform.TIKTOK, viewModel.uiState.value.selectedPlatform)

        // Switch content type to STORY
        viewModel.selectType(SocialContentType.STORY)
        assertEquals(SocialContentType.STORY, viewModel.uiState.value.selectedType)

        // Current preset should reflect 9:16 vertical
        val preset = viewModel.uiState.value.currentPreset
        assertEquals(9, preset.ratioX)
        assertEquals(16, preset.ratioY)

        // Quality update
        viewModel.updateQuality(92)
        assertEquals(92, viewModel.uiState.value.quality)
    }

    // =========================================================================
    // 3. WHATSAPP OPTIMIZER (WhatsAppViewModel)
    // =========================================================================
    @Test
    fun `WhatsAppViewModel manages tiers and custom target parameters correctly`() = runTest {
        val whatsAppEngine = mockk<WhatsAppEngine>(relaxed = true)
        val viewModel = WhatsAppViewModel(whatsAppEngine)

        val mockUri = mockk<Uri>()
        viewModel.selectImage(mockUri)
        assertEquals(mockUri, viewModel.uiState.value.selectedImageUri)

        // Default tier is Balanced
        assertEquals(WhatsAppTier.BALANCED, viewModel.uiState.value.config.tier)

        // Switch to High Quality
        viewModel.selectTier(WhatsAppTier.HIGH_QUALITY)
        assertEquals(WhatsAppTier.HIGH_QUALITY, viewModel.uiState.value.config.tier)

        // Switch to Custom and set KB limit
        viewModel.selectTier(WhatsAppTier.CUSTOM)
        viewModel.updateCustomTargetSize(500)
        viewModel.updateCustomQuality(80)
        viewModel.updateCustomMaxDimension(1600)

        val config = viewModel.uiState.value.config
        assertEquals(WhatsAppTier.CUSTOM, config.tier)
        assertEquals(500, config.customTargetSizeKB)
        assertEquals(80, config.customQuality)
        assertEquals(1600, config.customMaxDimension)
    }

    // =========================================================================
    // 4. ROTATE & FLIP (RotateViewModel)
    // =========================================================================
    @Test
    fun `RotateViewModel handles 90 degree increments and flip toggles`() = runTest {
        val rotateUseCase = mockk<RotateImageUseCase>(relaxed = true)
        val imageRepo = mockk<ImageRepository>(relaxed = true)
        val viewModel = RotateViewModel(rotateUseCase, imageRepo)

        // 90 CW rotation increments
        viewModel.rotate90CW()
        assertEquals(90f, viewModel.uiState.value.rotation, 0.01f)

        viewModel.rotate90CW()
        assertEquals(180f, viewModel.uiState.value.rotation, 0.01f)

        // 90 CCW rotation
        viewModel.rotate90CCW()
        assertEquals(90f, viewModel.uiState.value.rotation, 0.01f)

        // Custom angle slider
        viewModel.setRotation(45f)
        assertEquals(45f, viewModel.uiState.value.rotation, 0.01f)

        // Flip toggles
        assertFalse(viewModel.uiState.value.flipH)
        viewModel.toggleFlipH()
        assertTrue(viewModel.uiState.value.flipH)

        assertFalse(viewModel.uiState.value.flipV)
        viewModel.toggleFlipV()
        assertTrue(viewModel.uiState.value.flipV)

        // Reset
        viewModel.reset()
        assertEquals(0f, viewModel.uiState.value.rotation, 0.01f)
        assertFalse(viewModel.uiState.value.flipH)
        assertFalse(viewModel.uiState.value.flipV)
    }

    // =========================================================================
    // 5. WATERMARK (WatermarkViewModel)
    // =========================================================================
    @Test
    fun `WatermarkViewModel updates text position and parameters correctly`() = runTest {
        val watermarkUseCase = mockk<AddWatermarkUseCase>(relaxed = true)
        val imageRepo = mockk<ImageRepository>(relaxed = true)
        val viewModel = WatermarkViewModel(watermarkUseCase, imageRepo)

        val mockUri = mockk<Uri>()
        coEvery { imageRepo.getImageInfo(mockUri) } returns ImageInfo(
            mockUri, "photo.jpg", 5000L, 1920, 1080, ImageFormat.JPEG, "image/jpeg"
        )

        viewModel.selectImage(mockUri)
        testScheduler.advanceUntilIdle()

        assertEquals(mockUri, viewModel.uiState.value.selectedImageUri)
        assertEquals("photo.jpg", viewModel.uiState.value.imageInfo?.fileName)

        // Update properties
        viewModel.setText("Confidential Document")
        assertEquals("Confidential Document", viewModel.uiState.value.text)

        viewModel.setPosition(WatermarkPosition.CENTER)
        assertEquals(WatermarkPosition.CENTER, viewModel.uiState.value.position)

        viewModel.setOpacity(0.75f)
        assertEquals(0.75f, viewModel.uiState.value.opacity, 0.01f)

        viewModel.setFontSize(32f)
        assertEquals(32f, viewModel.uiState.value.fontSize, 0.01f)

        // Validation on blank text
        viewModel.setText("   ")
        viewModel.apply()
        assertEquals("Text cannot be empty", viewModel.uiState.value.error)
    }
}

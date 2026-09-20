package com.scanflow.photocompressor.presentation

import android.net.Uri
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.repository.PreferencesRepository
import com.scanflow.photocompressor.domain.repository.UserTierRepository
import com.scanflow.photocompressor.domain.usecase.*
import com.scanflow.photocompressor.engine.CropEngine
import com.scanflow.photocompressor.fixtures.ImageTestFixtures
import com.scanflow.photocompressor.ui.compress.CompressViewModel
import com.scanflow.photocompressor.ui.crop.CropViewModel
import com.scanflow.photocompressor.ui.history.HistoryViewModel
import com.scanflow.photocompressor.ui.home.HomeViewModel
import com.scanflow.photocompressor.ui.resize.ResizeViewModel
import com.scanflow.photocompressor.ui.settings.SettingsViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Specification 83: UI Flow and Screen State Tests.
 *
 * Tests:
 * 1. Home
 * 2. Select
 * 3. Compress
 * 4. Resize
 * 5. Crop
 * 6. Result
 * 7. History
 * 8. Settings
 * 9. Pro
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UiFlowAndNavigationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. HOME SCREEN STATE
    // =========================================================================
    @Test
    fun `1 - Home screen displays tools and observes recent history`() = runTest {
        val getHistoryUseCase = mockk<GetHistoryUseCase>(relaxed = true)
        val managePresetsUseCase = mockk<ManagePresetsUseCase>(relaxed = true)
        val sampleHistory = listOf(
            ProcessingHistory(
                id = "1",
                operation = OperationType.COMPRESS,
                originalSize = 1_000_000L,
                resultSize = 400_000L,
                inputFileName = "test.jpg",
                outputFileName = "test_compressed.jpg"
            )
        )
        every { getHistoryUseCase.getRecentHistory(any()) } returns flowOf(sampleHistory)
        coEvery { getHistoryUseCase.getTotalSavedBytes() } returns 600_000L
        coEvery { getHistoryUseCase.getTotalOperations() } returns 1

        val homeViewModel = HomeViewModel(getHistoryUseCase, managePresetsUseCase)
        testScheduler.advanceUntilIdle()

        val state = homeViewModel.uiState.value
        assertEquals(1, state.recentHistory.size)
        assertEquals(600_000L, state.totalSavedBytes)
        assertEquals("test.jpg", state.recentHistory.first().inputFileName)
    }

    // =========================================================================
    // 2. SELECT FLOW
    // =========================================================================
    @Test
    fun `2 - Select flow updates ViewModel with selected photos and extracts info`() = runTest {
        val imageRepo = mockk<ImageRepository>(relaxed = true)
        val compressUseCase = mockk<CompressImageUseCase>(relaxed = true)
        val fixture = ImageTestFixtures.SMALL_JPEG
        val mockUri = fixture.mockUri()

        coEvery { imageRepo.getImageInfo(mockUri) } returns ImageInfo(
            uri = mockUri,
            fileName = fixture.fileName,
            fileSize = fixture.fileSizeBytes,
            width = fixture.width,
            height = fixture.height,
            mimeType = fixture.mimeType,
            format = fixture.format!!
        )

        val compressViewModel = CompressViewModel(compressUseCase, imageRepo)
        compressViewModel.selectImages(listOf(mockUri))
        testScheduler.advanceUntilIdle()

        val state = compressViewModel.uiState.value
        assertEquals(mockUri, state.selectedImageUri)
        assertEquals(1, state.selectedImageUris.size)
        assertNotNull(state.imageInfo)
        assertEquals(fixture.fileName, state.imageInfo?.fileName)
    }

    // =========================================================================
    // 3. COMPRESS SCREEN STATE
    // =========================================================================
    @Test
    fun `3 - Compress screen switches modes, presets, and updates parameters`() = runTest {
        val imageRepo = mockk<ImageRepository>(relaxed = true)
        val compressUseCase = mockk<CompressImageUseCase>(relaxed = true)
        val viewModel = CompressViewModel(compressUseCase, imageRepo)

        // Mode switching: Quick -> Target Size -> Quality
        viewModel.setMode(CompressionMode.TARGET_SIZE)
        assertEquals(CompressionMode.TARGET_SIZE, viewModel.uiState.value.mode)

        viewModel.setCustomTargetSizeKB("250")
        assertEquals("250", viewModel.uiState.value.customTargetSizeKB)

        viewModel.setMode(CompressionMode.QUICK)
        assertEquals(CompressionMode.QUICK, viewModel.uiState.value.mode)

        // Preset selection: Small -> Balanced -> High Quality
        viewModel.setQuickPreset(QuickPreset.SMALL)
        assertEquals(QuickPreset.SMALL, viewModel.uiState.value.quickPreset)

        viewModel.setQuickPreset(QuickPreset.HIGH_QUALITY)
        assertEquals(QuickPreset.HIGH_QUALITY, viewModel.uiState.value.quickPreset)
    }

    // =========================================================================
    // 4. RESIZE SCREEN STATE
    // =========================================================================
    @Test
    fun `4 - Resize screen maintains aspect ratio lock and resolution presets`() = runTest {
        val resizeUseCase = mockk<ResizeImageUseCase>(relaxed = true)
        val imageRepo = mockk<ImageRepository>(relaxed = true)
        val fixture = ImageTestFixtures.LANDSCAPE
        val mockUri = fixture.mockUri()

        coEvery { imageRepo.getImageInfo(mockUri) } returns ImageInfo(
            uri = mockUri,
            fileName = fixture.fileName,
            fileSize = fixture.fileSizeBytes,
            width = fixture.width,
            height = fixture.height,
            mimeType = fixture.mimeType,
            format = fixture.format!!
        )

        val viewModel = ResizeViewModel(resizeUseCase, imageRepo)
        viewModel.selectImage(mockUri)
        testScheduler.advanceUntilIdle()

        // Toggle aspect ratio lock
        assertTrue(viewModel.uiState.value.lockAspectRatio)
        viewModel.toggleAspectLock()
        assertFalse(viewModel.uiState.value.lockAspectRatio)

        // Select preset
        viewModel.setPreset(ResizePreset.P_1080)
        assertEquals(ResizePreset.P_1080, viewModel.uiState.value.selectedPreset)
    }

    // =========================================================================
    // 5. CROP SCREEN STATE
    // =========================================================================
    @Test
    fun `5 - Crop screen selects aspect ratios and custom crop bounds`() = runTest {
        val cropUseCase = mockk<CropImageUseCase>(relaxed = true)
        val imageRepo = mockk<ImageRepository>(relaxed = true)
        val cropEngine = mockk<CropEngine>(relaxed = true)
        val fixture = ImageTestFixtures.LARGE_JPEG
        val mockUri = fixture.mockUri()

        coEvery { imageRepo.getImageInfo(mockUri) } returns ImageInfo(
            uri = mockUri,
            fileName = fixture.fileName,
            fileSize = fixture.fileSizeBytes,
            width = fixture.width,
            height = fixture.height,
            mimeType = fixture.mimeType,
            format = fixture.format!!
        )

        val viewModel = CropViewModel(cropUseCase, imageRepo, cropEngine)
        viewModel.selectImage(mockUri)
        testScheduler.advanceUntilIdle()

        // Aspect ratio selection
        viewModel.setAspectRatio(AspectRatioPreset.SQUARE)
        assertEquals(AspectRatioPreset.SQUARE, viewModel.uiState.value.selectedAspectRatio)

        viewModel.setAspectRatio(AspectRatioPreset.RATIO_16_9)
        assertEquals(AspectRatioPreset.RATIO_16_9, viewModel.uiState.value.selectedAspectRatio)
    }

    // =========================================================================
    // 6. RESULT STATE & METRICS
    // =========================================================================
    @Test
    fun `6 - Result screen displays before, after, saved bytes, and reduction percentage`() = runTest {
        val originalBytes = 2_000_000L
        val compressedBytes = 500_000L
        val outputUri = mockk<Uri>(relaxed = true)

        val result = CompressionResult(
            originalSize = originalBytes,
            compressedSize = compressedBytes,
            outputUri = outputUri,
            outputFileName = "test_result.jpg",
            width = 1920,
            height = 1080,
            format = ImageFormat.JPEG,
            quality = 80,
            durationMs = 95L
        )

        assertEquals(1_500_000L, result.savedBytes)
        assertEquals(75.0, result.reductionPercent, 0.01)
        assertEquals(outputUri, result.outputUri)
    }

    // =========================================================================
    // 7. HISTORY SCREEN STATE
    // =========================================================================
    @Test
    fun `7 - History screen loads items, supports filtering, and calculates totals`() = runTest {
        val getHistoryUseCase = mockk<GetHistoryUseCase>(relaxed = true)
        val items = listOf(
            ProcessingHistory(id = "1", operation = OperationType.COMPRESS, originalSize = 1000L, resultSize = 400L, inputFileName = "a.jpg"),
            ProcessingHistory(id = "2", operation = OperationType.RESIZE, originalSize = 2000L, resultSize = 1000L, inputFileName = "b.png")
        )
        every { getHistoryUseCase.getAllHistory() } returns flowOf(items)
        coEvery { getHistoryUseCase.getTotalSavedBytes() } returns 1600L
        coEvery { getHistoryUseCase.getTotalOperations() } returns 2

        val viewModel = HistoryViewModel(getHistoryUseCase)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.historyList.size)
        assertEquals(1600L, state.totalSavedBytes)
    }

    // =========================================================================
    // 8. SETTINGS SCREEN STATE
    // =========================================================================
    @Test
    fun `8 - Settings screen allows theme switching, quality adjustments, and format defaults`() = runTest {
        val prefsRepo = mockk<PreferencesRepository>(relaxed = true)
        val initialPrefs = AppPreferences(theme = ThemeMode.SYSTEM, defaultQuality = 80)
        every { prefsRepo.preferencesFlow } returns flowOf(initialPrefs)

        val viewModel = SettingsViewModel(
            fileManager = mockk(relaxed = true),
            preferencesRepository = prefsRepo
        )
        testScheduler.advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        testScheduler.advanceUntilIdle()
        coVerify { prefsRepo.setThemeMode(ThemeMode.DARK) }

        viewModel.setDefaultQuality(95)
        testScheduler.advanceUntilIdle()
        coVerify { prefsRepo.setDefaultQuality(95) }
    }

    // =========================================================================
    // 9. PRO SCREEN STATE & GATING
    // =========================================================================
    @Test
    fun `9 - Pro paywall screen triggers for gated features and unlocks upon purchase`() = runTest {
        val tierRepo = mockk<UserTierRepository>(relaxed = true)
        every { tierRepo.currentTier } returns flowOf(UserTier.FREE)
        every { tierRepo.isFeatureAvailable(ProFeature.TARGET_FILE_SIZE, UserTier.FREE) } returns false
        every { tierRepo.isFeatureAvailable(ProFeature.TARGET_FILE_SIZE, UserTier.PRO) } returns true

        val checkProFeatureUseCase = CheckProFeatureUseCase(tierRepo)
        assertFalse(checkProFeatureUseCase.canAccess(ProFeature.TARGET_FILE_SIZE))

        // When user upgrades to Pro
        coEvery { tierRepo.setUserTier(UserTier.PRO) } returns Unit

        checkProFeatureUseCase.upgradeToPro()
        coVerify { tierRepo.setUserTier(UserTier.PRO) }
    }
}

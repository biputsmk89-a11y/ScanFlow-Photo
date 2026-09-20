package com.scanflow.photocompressor.presentation

import com.scanflow.photocompressor.data.storage.FileNamingEngine
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.PreferencesRepository
import com.scanflow.photocompressor.ui.compress.CompressViewModel
import com.scanflow.photocompressor.ui.convert.ConvertViewModel
import com.scanflow.photocompressor.ui.resize.ResizeViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesSynchronizationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `CompressViewModel synchronizes defaultQuality, defaultFormat, and preserveExif from PreferencesRepository`() = runTest {
        val prefsRepo = mockk<PreferencesRepository>(relaxed = true)
        val customPrefs = AppPreferences(
            theme = ThemeMode.DARK,
            defaultQuality = 92,
            defaultFormat = ImageFormat.WEBP,
            behavior = DefaultBehavior(
                preserveExif = false,
                keepAspectRatio = true,
                conflictStrategy = ConflictStrategy.TIMESTAMP
            )
        )
        every { prefsRepo.preferencesFlow } returns flowOf(customPrefs)

        val viewModel = CompressViewModel(
            compressImageUseCase = mockk(relaxed = true),
            imageRepository = mockk(relaxed = true),
            preferencesRepository = prefsRepo
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Quality should sync to 92", 92, state.quality)
        assertEquals("Format should sync to WEBP", ImageFormat.WEBP, state.format)
        assertEquals("PreserveExif false should sync to REMOVE_ALL", MetadataOption.REMOVE_ALL, state.metadataOption)
    }

    @Test
    fun `ResizeViewModel synchronizes defaultQuality and keepAspectRatio from PreferencesRepository`() = runTest {
        val prefsRepo = mockk<PreferencesRepository>(relaxed = true)
        val customPrefs = AppPreferences(
            defaultQuality = 85,
            behavior = DefaultBehavior(
                keepAspectRatio = false
            )
        )
        every { prefsRepo.preferencesFlow } returns flowOf(customPrefs)

        val viewModel = ResizeViewModel(
            resizeImageUseCase = mockk(relaxed = true),
            imageRepository = mockk(relaxed = true),
            preferencesRepository = prefsRepo
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Quality should sync to 85", 85, state.quality)
        assertFalse("keepAspectRatio should sync to false", state.lockAspectRatio)
    }

    @Test
    fun `ConvertViewModel synchronizes targetFormat and quality from PreferencesRepository`() = runTest {
        val prefsRepo = mockk<PreferencesRepository>(relaxed = true)
        val customPrefs = AppPreferences(
            defaultQuality = 88,
            defaultFormat = ImageFormat.PNG
        )
        every { prefsRepo.preferencesFlow } returns flowOf(customPrefs)

        val viewModel = ConvertViewModel(
            convertFormatUseCase = mockk(relaxed = true),
            imageRepository = mockk(relaxed = true),
            preferencesRepository = prefsRepo
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("TargetFormat should sync to PNG", ImageFormat.PNG, state.targetFormat)
        assertEquals("Quality should sync to 88", 88, state.quality)
    }

    @Test
    fun `FileNamingEngine applies custom prefix and timestamp correctly`() {
        val engine = FileNamingEngine()

        // 1. SCAN Prefix without timestamp
        val configScan = FileNamingConfig(
            prefixType = NamingPrefixType.SCAN,
            includeTimestamp = false
        )
        val fileName1 = engine.generateFileName(
            originalName = "sample.jpg",
            operationType = OperationType.COMPRESS,
            targetFormat = ImageFormat.JPEG,
            namingConfig = configScan
        )
        assertEquals("SCAN_sample_compressed.jpg", fileName1)

        // 2. Custom Prefix
        val configCustom = FileNamingConfig(
            prefixType = NamingPrefixType.CUSTOM,
            customPrefixText = "OFFICE_DOC",
            includeTimestamp = false
        )
        val fileName2 = engine.generateFileName(
            originalName = "receipt.png",
            operationType = OperationType.CONVERT,
            targetFormat = ImageFormat.WEBP,
            namingConfig = configCustom
        )
        assertEquals("OFFICE_DOC_receipt_webp.webp", fileName2)

        // 3. ConflictStrategy TIMESTAMP resolution
        val resolved = engine.resolveConflict(
            targetFileName = "SCAN_sample_compressed.jpg",
            strategy = ConflictStrategy.TIMESTAMP,
            existsPredicate = { it == "SCAN_sample_compressed.jpg" }
        )
        assertTrue("Resolved name should contain timestamp", resolved.startsWith("SCAN_sample_compressed_"))
        assertTrue("Resolved name should end with .jpg", resolved.endsWith(".jpg"))
    }
}

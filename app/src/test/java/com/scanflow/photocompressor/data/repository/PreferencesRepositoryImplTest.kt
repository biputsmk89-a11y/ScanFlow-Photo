package com.scanflow.photocompressor.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.scanflow.photocompressor.domain.model.ConflictStrategy
import com.scanflow.photocompressor.domain.model.DefaultBehavior
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PreferencesRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var repository: PreferencesRepositoryImpl

    @Before
    fun setup() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val testFile = File(tempFolder.root, "test_preferences_${System.nanoTime()}.preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { testFile }
        )
        repository = PreferencesRepositoryImpl(testDataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `initial preferences returns expected default values`() = runTest {
        val prefs = repository.preferencesFlow.first()
        assertEquals(ThemeMode.SYSTEM, prefs.theme)
        assertEquals(80, prefs.defaultQuality)
        assertEquals(ImageFormat.JPEG, prefs.defaultFormat)
        assertTrue(prefs.behavior.preserveExif)
        assertTrue(prefs.behavior.keepAspectRatio)
        assertTrue(prefs.behavior.autoCleanTemp)
        assertEquals(ConflictStrategy.INCREMENT, prefs.behavior.conflictStrategy)
    }

    @Test
    fun `setThemeMode updates theme preference`() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        val prefs = repository.preferencesFlow.first()
        assertEquals(ThemeMode.DARK, prefs.theme)
    }

    @Test
    fun `setDefaultQuality updates quality within valid bounds`() = runTest {
        repository.setDefaultQuality(95)
        val prefs = repository.preferencesFlow.first()
        assertEquals(95, prefs.defaultQuality)
    }

    @Test
    fun `setDefaultQuality out of bounds is clamped`() = runTest {
        repository.setDefaultQuality(150)
        val prefs = repository.preferencesFlow.first()
        assertEquals(100, prefs.defaultQuality)
    }

    @Test
    fun `setDefaultFormat updates format preference`() = runTest {
        repository.setDefaultFormat(ImageFormat.WEBP)
        val prefs = repository.preferencesFlow.first()
        assertEquals(ImageFormat.WEBP, prefs.defaultFormat)
    }

    @Test
    fun `setDefaultBehavior updates multiple behavior flags`() = runTest {
        repository.setDefaultBehavior(
            DefaultBehavior(
                preserveExif = false,
                keepAspectRatio = false,
                autoCleanTemp = true,
                conflictStrategy = ConflictStrategy.TIMESTAMP
            )
        )
        val prefs = repository.preferencesFlow.first()
        assertFalse(prefs.behavior.preserveExif)
        assertFalse(prefs.behavior.keepAspectRatio)
        assertEquals(ConflictStrategy.TIMESTAMP, prefs.behavior.conflictStrategy)
    }

    @Test
    fun `resetToDefaults resets to initial default preferences`() = runTest {
        repository.resetToDefaults()
        val prefs = repository.preferencesFlow.first()
        assertEquals(ThemeMode.SYSTEM, prefs.theme)
        assertEquals(80, prefs.defaultQuality)
        assertEquals(ImageFormat.JPEG, prefs.defaultFormat)
    }
}

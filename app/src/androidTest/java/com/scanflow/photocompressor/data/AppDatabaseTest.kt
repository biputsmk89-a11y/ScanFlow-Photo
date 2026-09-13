package com.scanflow.photocompressor.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scanflow.photocompressor.data.local.AppDatabase
import com.scanflow.photocompressor.data.local.HistoryDao
import com.scanflow.photocompressor.data.local.HistoryEntity
import com.scanflow.photocompressor.data.local.PresetDao
import com.scanflow.photocompressor.data.local.PresetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var historyDao: HistoryDao
    private lateinit var presetDao: PresetDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        historyDao = database.historyDao()
        presetDao = database.presetDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveHistoryEntry() = runTest {
        val entity = HistoryEntity(
            inputUri = "content://test/input",
            inputFileName = "photo.jpg",
            outputUri = "content://test/output",
            outputFileName = "photo_compressed.jpg",
            operation = "COMPRESS",
            originalSize = 1000000,
            resultSize = 500000,
            width = 1920,
            height = 1080
        )

        val id = historyDao.insert(entity)
        val result = historyDao.getById(id)

        assertNotNull(result)
        assertEquals("photo.jpg", result?.inputFileName)
        assertEquals(1000000L, result?.originalSize)
    }

    @Test
    fun getTotalSavedBytes() = runTest {
        historyDao.insert(HistoryEntity(inputUri = "a", inputFileName = "a", outputUri = "b", outputFileName = "b", operation = "COMPRESS", originalSize = 1000, resultSize = 500, width = 100, height = 100))
        historyDao.insert(HistoryEntity(inputUri = "c", inputFileName = "c", outputUri = "d", outputFileName = "d", operation = "RESIZE", originalSize = 2000, resultSize = 800, width = 100, height = 100))

        val totalSaved = historyDao.getTotalSavedBytes()
        assertEquals(1700L, totalSaved) // (1000-500) + (2000-800)
    }

    @Test
    fun clearAllHistory() = runTest {
        historyDao.insert(HistoryEntity(inputUri = "a", inputFileName = "a", outputUri = "b", outputFileName = "b", operation = "COMPRESS", originalSize = 1000, resultSize = 500, width = 100, height = 100))
        historyDao.clearAll()

        val count = historyDao.getTotalOperations()
        assertEquals(0, count)
    }

    @Test
    fun insertAndRetrievePreset() = runTest {
        val preset = PresetEntity(name = "Test", quality = 80, maxWidth = 1920, maxHeight = 1080, format = "JPEG", preserveExif = true, isDefault = false)

        val id = presetDao.insert(preset)
        val result = presetDao.getById(id)

        assertNotNull(result)
        assertEquals("Test", result?.name)
        assertEquals(80, result?.quality)
    }

    @Test
    fun presetCount() = runTest {
        presetDao.insert(PresetEntity(name = "A", quality = 80, maxWidth = 0, maxHeight = 0, format = "JPEG", preserveExif = true, isDefault = true))
        presetDao.insert(PresetEntity(name = "B", quality = 50, maxWidth = 1280, maxHeight = 1280, format = "WEBP", preserveExif = false, isDefault = false))

        val count = presetDao.getCount()
        assertEquals(2, count)
    }
}

package com.scanflow.photocompressor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for the Photo Compressor application.
 */
@Database(
    entities = [
        ProcessingHistoryEntity::class,
        PresetEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun presetDao(): PresetDao

    companion object {
        const val DATABASE_NAME = "photo_compressor_db"
    }
}

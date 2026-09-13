package com.scanflow.photocompressor.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Safe, non-destructive Room database migrations.
 * Never uses destructive migration in production.
 */
object Migrations {

    /**
     * Migration from Version 1 (legacy HistoryEntity) to Version 2 (ProcessingHistoryEntity).
     * Preserves all legacy user processing history records without data loss.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create new table matching ProcessingHistoryEntity schema
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `processing_history_new` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `operationType` TEXT NOT NULL,
                    `itemCount` INTEGER NOT NULL,
                    `originalBytes` INTEGER,
                    `outputBytes` INTEGER,
                    `reductionPercent` REAL,
                    `createdAt` INTEGER NOT NULL,
                    `settingsJson` TEXT,
                    `status` TEXT NOT NULL,
                    `inputUri` TEXT,
                    `inputFileName` TEXT,
                    `outputUri` TEXT,
                    `outputFileName` TEXT
                )
                """.trimIndent()
            )

            // 2. Safely copy data from legacy table if it exists
            db.execSQL(
                """
                INSERT INTO `processing_history_new` (
                    `id`, `operationType`, `itemCount`, `originalBytes`, `outputBytes`, 
                    `reductionPercent`, `createdAt`, `settingsJson`, `status`, 
                    `inputUri`, `inputFileName`, `outputUri`, `outputFileName`
                )
                SELECT 
                    CAST(id AS TEXT),
                    operation,
                    1,
                    originalSize,
                    resultSize,
                    CASE 
                        WHEN originalSize > 0 THEN CAST((originalSize - resultSize) * 100.0 / originalSize AS REAL)
                        ELSE 0.0 
                    END,
                    timestamp,
                    NULL,
                    'SUCCESS',
                    inputUri,
                    inputFileName,
                    outputUri,
                    outputFileName
                FROM `processing_history`
                """.trimIndent()
            )

            // 3. Drop legacy table and rename new table
            db.execSQL("DROP TABLE `processing_history`")
            db.execSQL("ALTER TABLE `processing_history_new` RENAME TO `processing_history`")
        }
    }
}

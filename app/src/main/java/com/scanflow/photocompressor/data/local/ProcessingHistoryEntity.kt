package com.scanflow.photocompressor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing processing history records.
 * Adheres to Rule 41:
 * - date (createdAt)
 * - tool (operationType)
 * - item count (itemCount)
 * - input size (originalBytes)
 * - output size (outputBytes)
 * - reduction (reductionPercent)
 * - settings (settingsJson)
 * - status (status)
 *
 * Notice: Large Bitmaps are NEVER stored in Room.
 */
@Entity(tableName = "processing_history")
data class ProcessingHistoryEntity(
    @PrimaryKey
    val id: String,
    val operationType: String,
    val itemCount: Int = 1,
    val originalBytes: Long? = null,
    val outputBytes: Long? = null,
    val reductionPercent: Float? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val settingsJson: String? = null,
    val status: String = "SUCCESS",
    val inputUri: String? = null,
    val inputFileName: String? = null,
    val outputUri: String? = null,
    val outputFileName: String? = null
)

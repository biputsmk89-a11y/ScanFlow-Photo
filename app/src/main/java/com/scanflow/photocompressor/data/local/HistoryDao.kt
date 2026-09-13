package com.scanflow.photocompressor.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for processing history operations using [ProcessingHistoryEntity].
 */
@Dao
interface HistoryDao {

    @Query("SELECT * FROM processing_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<ProcessingHistoryEntity>>

    @Query("SELECT * FROM processing_history ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<ProcessingHistoryEntity>>

    @Query("SELECT * FROM processing_history WHERE id = :id")
    suspend fun getById(id: String): ProcessingHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProcessingHistoryEntity)

    @Delete
    suspend fun delete(entity: ProcessingHistoryEntity)

    @Query("DELETE FROM processing_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM processing_history")
    suspend fun clearAll()

    @Query("SELECT COALESCE(SUM(originalBytes - outputBytes), 0) FROM processing_history WHERE originalBytes > outputBytes AND status = 'SUCCESS'")
    suspend fun getTotalSavedBytes(): Long

    @Query("SELECT COUNT(*) FROM processing_history WHERE status = 'SUCCESS'")
    suspend fun getTotalOperations(): Int
}

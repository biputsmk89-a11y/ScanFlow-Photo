package com.scanflow.photocompressor.domain.repository

import com.scanflow.photocompressor.domain.model.ProcessingHistory
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for processing history management.
 */
interface HistoryRepository {
    /**
     * Get all history entries as a Flow, ordered by timestamp descending.
     */
    fun getAllHistory(): Flow<List<ProcessingHistory>>

    /**
     * Get recent history entries (limited count).
     */
    fun getRecentHistory(limit: Int = 10): Flow<List<ProcessingHistory>>

    /**
     * Add a new history entry.
     */
    suspend fun addEntry(entry: ProcessingHistory): String

    /**
     * Delete a history entry by ID.
     */
    suspend fun deleteEntry(id: String)

    /**
     * Clear all history.
     */
    suspend fun clearAll()

    /**
     * Get total bytes saved across all operations.
     */
    suspend fun getTotalSavedBytes(): Long

    /**
     * Get total number of operations performed.
     */
    suspend fun getTotalOperations(): Int
}

package com.scanflow.photocompressor.domain.usecase

import com.scanflow.photocompressor.domain.model.ProcessingHistory
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving processing history.
 */
class GetHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    /**
     * Get all processing history.
     */
    fun getAllHistory(): Flow<List<ProcessingHistory>> = historyRepository.getAllHistory()

    /**
     * Get recent history entries.
     */
    fun getRecentHistory(limit: Int = 10): Flow<List<ProcessingHistory>> =
        historyRepository.getRecentHistory(limit)

    /**
     * Get total bytes saved.
     */
    suspend fun getTotalSavedBytes(): Long = historyRepository.getTotalSavedBytes()

    /**
     * Get total operations count.
     */
    suspend fun getTotalOperations(): Int = historyRepository.getTotalOperations()

    /**
     * Delete a history entry.
     */
    suspend fun deleteEntry(id: String) = historyRepository.deleteEntry(id)

    /**
     * Clear all history.
     */
    suspend fun clearAll() = historyRepository.clearAll()
}

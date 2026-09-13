package com.scanflow.photocompressor.data.repository

import com.scanflow.photocompressor.data.local.HistoryDao
import com.scanflow.photocompressor.data.local.toDomain
import com.scanflow.photocompressor.data.local.toEntity
import com.scanflow.photocompressor.domain.model.ProcessingHistory
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of HistoryRepository.
 */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<ProcessingHistory>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentHistory(limit: Int): Flow<List<ProcessingHistory>> {
        return historyDao.getRecentHistory(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addEntry(entry: ProcessingHistory): String {
        historyDao.insert(entry.toEntity())
        return entry.id
    }

    override suspend fun deleteEntry(id: String) {
        historyDao.deleteById(id)
    }

    override suspend fun clearAll() {
        historyDao.clearAll()
    }

    override suspend fun getTotalSavedBytes(): Long {
        return historyDao.getTotalSavedBytes()
    }

    override suspend fun getTotalOperations(): Int {
        return historyDao.getTotalOperations()
    }
}

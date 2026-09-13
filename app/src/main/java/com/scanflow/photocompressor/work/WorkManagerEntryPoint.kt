package com.scanflow.photocompressor.work

import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.engine.BatchProcessor
import com.scanflow.photocompressor.engine.ImageEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint to inject dependencies into WorkManager workers.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkManagerEntryPoint {
    fun batchProcessor(): BatchProcessor
    fun imageEngine(): ImageEngine
    fun imageRepository(): ImageRepository
    fun fileManager(): FileManager
    fun historyRepository(): HistoryRepository
}

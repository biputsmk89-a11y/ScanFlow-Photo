package com.scanflow.photocompressor.data.repository

import com.scanflow.photocompressor.data.local.PresetDao
import com.scanflow.photocompressor.data.local.toDomain
import com.scanflow.photocompressor.data.local.toEntity
import com.scanflow.photocompressor.domain.model.CompressionPreset
import com.scanflow.photocompressor.domain.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of PresetRepository.
 */
@Singleton
class PresetRepositoryImpl @Inject constructor(
    private val presetDao: PresetDao
) : PresetRepository {

    override fun getAllPresets(): Flow<List<CompressionPreset>> {
        return presetDao.getAllPresets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPresetById(id: Long): CompressionPreset? {
        return presetDao.getById(id)?.toDomain()
    }

    override suspend fun savePreset(preset: CompressionPreset): Long {
        return presetDao.insert(preset.toEntity())
    }

    override suspend fun updatePreset(preset: CompressionPreset) {
        presetDao.update(preset.toEntity())
    }

    override suspend fun deletePreset(id: Long) {
        presetDao.deleteById(id)
    }

    override suspend fun seedDefaults() {
        val count = presetDao.getCount()
        if (count == 0) {
            val defaultEntities = CompressionPreset.defaults.map { it.toEntity() }
            presetDao.insertAll(defaultEntities)
        }
    }
}

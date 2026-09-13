package com.scanflow.photocompressor.domain.usecase

import com.scanflow.photocompressor.domain.model.CompressionPreset
import com.scanflow.photocompressor.domain.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing compression presets (CRUD).
 */
class ManagePresetsUseCase @Inject constructor(
    private val presetRepository: PresetRepository
) {
    fun getAllPresets(): Flow<List<CompressionPreset>> = presetRepository.getAllPresets()

    suspend fun getPresetById(id: Long): CompressionPreset? = presetRepository.getPresetById(id)

    suspend fun savePreset(preset: CompressionPreset): Long = presetRepository.savePreset(preset)

    suspend fun updatePreset(preset: CompressionPreset) = presetRepository.updatePreset(preset)

    suspend fun deletePreset(id: Long) = presetRepository.deletePreset(id)

    suspend fun seedDefaults() = presetRepository.seedDefaults()
}

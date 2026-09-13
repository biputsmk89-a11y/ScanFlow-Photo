package com.scanflow.photocompressor.domain.usecase

import com.scanflow.photocompressor.domain.model.CompressionPreset
import com.scanflow.photocompressor.domain.repository.PresetRepository
import javax.inject.Inject

/**
 * 56. USE CASE: SavePresetUseCase
 * Persists custom user compression presets to Room database.
 */
class SavePresetUseCase @Inject constructor(
    private val presetRepository: PresetRepository
) {
    suspend operator fun invoke(preset: CompressionPreset): Long {
        return presetRepository.savePreset(preset)
    }
}

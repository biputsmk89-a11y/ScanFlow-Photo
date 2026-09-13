package com.scanflow.photocompressor.domain.repository

import com.scanflow.photocompressor.domain.model.CompressionPreset
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for compression preset management.
 */
interface PresetRepository {
    /**
     * Get all presets as a Flow.
     */
    fun getAllPresets(): Flow<List<CompressionPreset>>

    /**
     * Get a preset by ID.
     */
    suspend fun getPresetById(id: Long): CompressionPreset?

    /**
     * Save a new preset.
     * @return The ID of the saved preset.
     */
    suspend fun savePreset(preset: CompressionPreset): Long

    /**
     * Update an existing preset.
     */
    suspend fun updatePreset(preset: CompressionPreset)

    /**
     * Delete a preset by ID.
     */
    suspend fun deletePreset(id: Long)

    /**
     * Seed default presets if the database is empty.
     */
    suspend fun seedDefaults()
}

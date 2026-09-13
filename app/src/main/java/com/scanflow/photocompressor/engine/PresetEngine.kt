package com.scanflow.photocompressor.engine

import com.scanflow.photocompressor.domain.model.CompressionPreset
import com.scanflow.photocompressor.domain.model.ImageInfo
import com.scanflow.photocompressor.domain.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Preset Engine for managing, applying, and recommending compression presets.
 * Supports requested system presets: WhatsApp, Email, Website, School Upload, Government Upload, Social Media, Custom.
 */
interface PresetEngine {
    /**
     * Get built-in system presets.
     */
    fun getSystemPresets(): List<CompressionPreset>

    /**
     * Get all available presets (system + custom).
     */
    fun getAllPresets(): Flow<List<CompressionPreset>>

    /**
     * Find a preset by its name.
     */
    suspend fun getPresetByName(name: String): CompressionPreset?

    /**
     * Find a preset by its ID.
     */
    suspend fun getPresetById(id: String): CompressionPreset?

    /**
     * Intelligently recommend a preset based on image metadata and optional use case.
     */
    fun recommendPreset(info: ImageInfo, targetUseCase: String? = null): CompressionPreset

    /**
     * Calculate target dimensions constrained by preset, preserving original aspect ratio.
     */
    fun calculateTargetDimensions(
        originalWidth: Int,
        originalHeight: Int,
        preset: CompressionPreset
    ): Pair<Int, Int>

    /**
     * Persist a custom user preset.
     */
    suspend fun savePreset(preset: CompressionPreset): Long

    /**
     * Delete a custom preset by Long ID.
     */
    suspend fun deletePreset(id: Long)

    /**
     * Delete a custom preset by String ID.
     */
    suspend fun deletePreset(id: String)
}

@Singleton
class PresetEngineImpl @Inject constructor(
    private val presetRepository: PresetRepository
) : PresetEngine {

    override fun getSystemPresets(): List<CompressionPreset> = CompressionPreset.defaults

    override fun getAllPresets(): Flow<List<CompressionPreset>> = presetRepository.getAllPresets()

    override suspend fun getPresetByName(name: String): CompressionPreset? {
        val systemMatch = CompressionPreset.defaults.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (systemMatch != null) return systemMatch
        return presetRepository.getPresetById(name.hashCode().toLong())
    }

    override suspend fun getPresetById(id: String): CompressionPreset? {
        val systemMatch = CompressionPreset.defaults.firstOrNull { it.id.equals(id, ignoreCase = true) }
        if (systemMatch != null) return systemMatch
        return id.toLongOrNull()?.let { presetRepository.getPresetById(it) }
    }

    override fun recommendPreset(info: ImageInfo, targetUseCase: String?): CompressionPreset {
        val useCase = targetUseCase?.lowercase()
        return when {
            useCase?.contains("whatsapp") == true -> CompressionPreset.WHATSAPP
            useCase?.contains("email") == true || useCase?.contains("mail") == true -> CompressionPreset.EMAIL
            useCase?.contains("web") == true || useCase?.contains("site") == true -> CompressionPreset.WEBSITE
            useCase?.contains("school") == true || useCase?.contains("student") == true || useCase?.contains("sekolah") == true || useCase?.contains("kampus") == true -> CompressionPreset.SCHOOL_UPLOAD
            useCase?.contains("gov") == true || useCase?.contains("pemerintah") == true || useCase?.contains("cpns") == true || useCase?.contains("passport") == true -> CompressionPreset.GOVERNMENT_UPLOAD
            useCase?.contains("social") == true || useCase?.contains("instagram") == true || useCase?.contains("facebook") == true || useCase?.contains("story") == true -> CompressionPreset.SOCIAL_MEDIA
            useCase?.contains("custom") == true -> CompressionPreset.CUSTOM
            info.fileSizeMB > 10.0 -> CompressionPreset.EMAIL
            info.fileSizeMB < 1.0 -> CompressionPreset.WEBSITE
            else -> CompressionPreset.SOCIAL_MEDIA
        }
    }

    override fun calculateTargetDimensions(
        originalWidth: Int,
        originalHeight: Int,
        preset: CompressionPreset
    ): Pair<Int, Int> {
        val maxDim = preset.maxDimension ?: if (preset.maxWidth > 0 || preset.maxHeight > 0) maxOf(preset.maxWidth, preset.maxHeight) else null
        if (maxDim == null || maxDim <= 0) {
            return Pair(originalWidth, originalHeight)
        }
        if (originalWidth <= 0 || originalHeight <= 0) {
            return Pair(originalWidth, originalHeight)
        }

        val longestEdge = maxOf(originalWidth, originalHeight)
        if (longestEdge <= maxDim) {
            return Pair(originalWidth, originalHeight)
        }

        val scale = maxDim.toFloat() / longestEdge.toFloat()
        return Pair(
            (originalWidth * scale).toInt().coerceAtLeast(1),
            (originalHeight * scale).toInt().coerceAtLeast(1)
        )
    }

    override suspend fun savePreset(preset: CompressionPreset): Long = presetRepository.savePreset(preset)

    override suspend fun deletePreset(id: Long) = presetRepository.deletePreset(id)

    override suspend fun deletePreset(id: String) {
        id.toLongOrNull()?.let { deletePreset(it) }
    }
}

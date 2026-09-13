package com.scanflow.photocompressor.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for compression preset operations.
 */
@Dao
interface PresetDao {

    @Query("SELECT * FROM compression_presets ORDER BY isDefault DESC, createdAt ASC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM compression_presets WHERE id = :id")
    suspend fun getById(id: Long): PresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PresetEntity): Long

    @Update
    suspend fun update(entity: PresetEntity)

    @Query("DELETE FROM compression_presets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM compression_presets")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PresetEntity>)
}

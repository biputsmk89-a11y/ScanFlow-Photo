package com.scanflow.photocompressor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing compression presets.
 */
@Entity(tableName = "compression_presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quality: Int,
    val maxWidth: Int,
    val maxHeight: Int,
    val format: String, // Stored as enum name string
    val preserveExif: Boolean,
    val isDefault: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

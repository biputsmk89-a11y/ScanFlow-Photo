package com.scanflow.photocompressor.data.local

import com.scanflow.photocompressor.domain.model.CompressionPreset
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.OperationType
import com.scanflow.photocompressor.domain.model.ProcessingHistory

/**
 * Mapper functions between domain models and Room entities.
 */

// History mappers
fun ProcessingHistoryEntity.toDomain(): ProcessingHistory {
    return ProcessingHistory(
        id = id,
        operationType = try {
            OperationType.valueOf(operationType)
        } catch (e: Exception) {
            OperationType.COMPRESS
        },
        itemCount = itemCount,
        originalBytes = originalBytes,
        outputBytes = outputBytes,
        reductionPercent = reductionPercent,
        createdAt = createdAt,
        settingsJson = settingsJson,
        status = status,
        inputUri = inputUri,
        inputFileName = inputFileName ?: "",
        outputUri = outputUri,
        outputFileName = outputFileName ?: ""
    )
}

fun ProcessingHistory.toEntity(): ProcessingHistoryEntity {
    return ProcessingHistoryEntity(
        id = id,
        operationType = operationType.name,
        itemCount = itemCount,
        originalBytes = originalBytes ?: originalSize,
        outputBytes = outputBytes ?: resultSize,
        reductionPercent = reductionPercent ?: savedPercentage.toFloat(),
        createdAt = createdAt,
        settingsJson = settingsJson,
        status = status,
        inputUri = inputUri,
        inputFileName = inputFileName,
        outputUri = outputUri,
        outputFileName = outputFileName
    )
}

// Preset mappers
fun PresetEntity.toDomain(): CompressionPreset {
    return CompressionPreset(
        id = if (isDefault) name.lowercase().replace(" ", "_") else id.toString(),
        name = name,
        quality = quality,
        maxDimension = if (maxWidth > 0) maxWidth else (if (maxHeight > 0) maxHeight else null),
        targetBytes = null,
        format = try {
            ImageFormat.valueOf(format)
        } catch (e: Exception) {
            null
        },
        removeGps = !preserveExif
    )
}

fun CompressionPreset.toEntity(): PresetEntity {
    return PresetEntity(
        id = id.toLongOrNull() ?: 0L,
        name = name,
        quality = quality ?: 80,
        maxWidth = maxDimension ?: maxWidth,
        maxHeight = maxDimension ?: maxHeight,
        format = format?.name ?: ImageFormat.JPEG.name,
        preserveExif = !removeGps,
        isDefault = isDefault,
        createdAt = createdAt
    )
}

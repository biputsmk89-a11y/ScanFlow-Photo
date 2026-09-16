package com.scanflow.photocompressor.domain.model

import java.util.UUID

/**
 * Domain model for processing history entry matching Rule 41.
 */
data class ProcessingHistory(
    val id: String = UUID.randomUUID().toString(),
    val operationType: OperationType = OperationType.COMPRESS,
    val itemCount: Int = 1,
    val originalBytes: Long? = null,
    val outputBytes: Long? = null,
    val reductionPercent: Float? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val settingsJson: String? = null,
    val status: String = "SUCCESS",
    val inputUri: String? = null,
    val inputFileName: String = "",
    val outputUri: String? = null,
    val outputFileName: String = "",
    val width: Int = 0,
    val height: Int = 0
) {
    constructor(
        id: String = UUID.randomUUID().toString(),
        operation: OperationType,
        originalSize: Long,
        resultSize: Long,
        inputUri: String? = null,
        inputFileName: String = "",
        outputUri: String? = null,
        outputFileName: String = "",
        width: Int = 0,
        height: Int = 0,
        timestamp: Long = System.currentTimeMillis(),
        itemCount: Int = 1,
        settingsJson: String? = null,
        status: String = "SUCCESS"
    ) : this(
        id = id,
        operationType = operation,
        itemCount = itemCount,
        originalBytes = originalSize,
        outputBytes = resultSize,
        reductionPercent = com.scanflow.photocompressor.util.ReductionCalculator.calculateReductionPercentFloat(originalSize, resultSize),
        createdAt = timestamp,
        settingsJson = settingsJson,
        status = status,
        inputUri = inputUri,
        inputFileName = inputFileName,
        outputUri = outputUri,
        outputFileName = outputFileName,
        width = width,
        height = height
    )

    // Backwards-compatible computed properties for seamless integration:
    val operation: OperationType get() = operationType
    val originalSize: Long get() = originalBytes ?: 0L
    val resultSize: Long get() = outputBytes ?: 0L
    val savedBytes: Long get() = com.scanflow.photocompressor.util.ReductionCalculator.calculateSavedBytes(originalBytes ?: 0L, outputBytes ?: 0L)
    val savedPercentage: Double get() = reductionPercent?.toDouble() ?: com.scanflow.photocompressor.util.ReductionCalculator.calculateReductionPercent(originalBytes ?: 0L, outputBytes ?: 0L)
    val timestamp: Long get() = createdAt
}

enum class OperationType(val displayName: String) {
    COMPRESS("Compress"),
    RESIZE("Resize"),
    CROP("Crop"),
    ROTATE("Rotate"),
    FLIP("Flip"),
    WATERMARK("Watermark"),
    CONVERT("Convert"),
    BATCH("Batch"),
    PDF("PDF"),
    PASSPORT("Passport"),
    SOCIAL("Social"),
    WHATSAPP("WhatsApp")
}

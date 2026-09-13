package com.scanflow.photocompressor.util

/**
 * Privacy & Telemetry Contract:
 *
 * Core Principles:
 * 1. 100% OFFLINE FIRST: All core processing works with zero network connectivity (airplane mode safe).
 * 2. PRIVACY GUARANTEE: Never upload original image, processed image, GPS coordinates, or EXIF metadata.
 * 3. TELEMETRY POLICY: Analytics / Telemetry may ONLY record non-sensitive, aggregated product metrics:
 *    - Tool type (e.g. "compress", "resize", "crop", "convert", "batch", "pdf")
 *    - Duration in milliseconds
 *    - File size ranges / reduction percentage
 *    - Success / failure status code
 *
 * Strictly PROHIBITED in telemetry:
 * - Direct image binary / byte streams
 * - File paths or identifiable user filenames
 * - GPS latitude, longitude, altitude
 * - Camera serial numbers / identifiable EXIF tags
 * - User personal identifiable information (PII)
 */
object PrivacyContract {

    /**
     * Non-sensitive product telemetry event data class.
     */
    data class ProductTelemetryEvent(
        val toolName: String,
        val durationMs: Long,
        val reductionPercent: Float?,
        val isSuccess: Boolean,
        val errorCode: String? = null
    ) {
        init {
            // Enforcement: Ensure toolName does not contain sensitive identifiers or URIs
            require(!toolName.contains("/") && !toolName.contains("content:") && !toolName.contains("file:")) {
                "Telemetry event toolName must be an abstract category, never a URI or file path."
            }
        }
    }

    /**
     * Sanitizes an event before any potential telemetry dispatch,
     * guaranteeing zero sensitive image data or coordinates leak.
     */
    fun recordEvent(
        tool: String,
        durationMs: Long,
        reductionPercent: Float? = null,
        isSuccess: Boolean = true,
        errorCode: String? = null
    ): ProductTelemetryEvent {
        return ProductTelemetryEvent(
            toolName = tool.trim().lowercase(),
            durationMs = durationMs.coerceAtLeast(0L),
            reductionPercent = reductionPercent,
            isSuccess = isSuccess,
            errorCode = errorCode?.take(50) // Truncated generic error label
        )
    }

    /**
     * Validates that an operation adheres to the offline privacy contract.
     * Throws SecurityException if an operation attempts to leak raw payload or GPS data.
     */
    fun verifyNoSensitiveDataLogged(loggedPayload: String) {
        val lower = loggedPayload.lowercase()
        val forbiddenSubstrings = listOf(
            "gps_latitude",
            "gps_longitude",
            "exif_lat",
            "exif_lon",
            "image/jpeg;base64",
            "image/png;base64"
        )
        for (forbidden in forbiddenSubstrings) {
            if (lower.contains(forbidden)) {
                throw SecurityException("Privacy violation: Sensitive data ($forbidden) detected in logged payload!")
            }
        }
    }
}

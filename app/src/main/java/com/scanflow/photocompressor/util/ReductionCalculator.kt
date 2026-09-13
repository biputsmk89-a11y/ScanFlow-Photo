package com.scanflow.photocompressor.util

/**
 * Standard utility for image reduction calculations:
 *
 * savedBytes = originalBytes - outputBytes
 * reductionPercent = ((originalBytes - outputBytes) / originalBytes) * 100
 *
 * Safely handles originalBytes <= 0 by returning 0 (no division by zero).
 */
object ReductionCalculator {

    /**
     * Calculates the raw saved bytes:
     * savedBytes = originalBytes - outputBytes
     */
    fun calculateSavedBytes(originalBytes: Long, outputBytes: Long): Long {
        return originalBytes - outputBytes
    }

    /**
     * Calculates the reduction percentage as Double:
     * reductionPercent = ((originalBytes - outputBytes) / originalBytes) * 100
     *
     * Handles originalBytes <= 0 safely by returning 0.0.
     */
    fun calculateReductionPercent(originalBytes: Long, outputBytes: Long): Double {
        if (originalBytes <= 0L) {
            return 0.0
        }
        val saved = (originalBytes - outputBytes).toDouble()
        return (saved / originalBytes.toDouble()) * 100.0
    }

    /**
     * Calculates the reduction percentage as Float:
     * reductionPercent = ((originalBytes - outputBytes) / originalBytes) * 100
     *
     * Handles originalBytes <= 0 safely by returning 0.0f.
     */
    fun calculateReductionPercentFloat(originalBytes: Long, outputBytes: Long): Float {
        if (originalBytes <= 0L) {
            return 0f
        }
        val saved = (originalBytes - outputBytes).toFloat()
        return (saved / originalBytes.toFloat()) * 100f
    }

    /**
     * Formats bytes into human-readable representation (e.g. 450 KB, 2.4 MB).
     */
    fun formatBytes(bytes: Long): String {
        val b = if (bytes < 0) -bytes else bytes
        val formatted = when {
            b < 1024 -> "$b B"
            b < 1024 * 1024 -> String.format("%.1f KB", b / 1024.0)
            b < 1024 * 1024 * 1024 -> String.format("%.1f MB", b / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", b / (1024.0 * 1024.0 * 1024.0))
        }
        return if (bytes < 0) "-$formatted" else formatted
    }
}

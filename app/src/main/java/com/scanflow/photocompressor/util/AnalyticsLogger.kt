package com.scanflow.photocompressor.util

/**
 * 51. ANALYTICS SPECIFICATION
 *
 * Allowed events:
 * - tool_opened
 * - image_selected
 * - processing_started
 * - processing_completed
 * - processing_failed
 * - batch_started
 * - batch_completed
 * - export_clicked
 * - share_clicked
 * - pro_screen_viewed
 * - subscription_started
 *
 * Strictly PROHIBITED:
 * - image pixels
 * - GPS
 * - EXIF content
 * - image content
 * - sensitive filenames
 */
enum class AnalyticsEvent(val eventName: String) {
    TOOL_OPENED("tool_opened"),
    IMAGE_SELECTED("image_selected"),
    PROCESSING_STARTED("processing_started"),
    PROCESSING_COMPLETED("processing_completed"),
    PROCESSING_FAILED("processing_failed"),
    BATCH_STARTED("batch_started"),
    BATCH_COMPLETED("batch_completed"),
    EXPORT_CLICKED("export_clicked"),
    SHARE_CLICKED("share_clicked"),
    PRO_SCREEN_VIEWED("pro_screen_viewed"),
    SUBSCRIPTION_STARTED("subscription_started");

    companion object {
        private val ALLOWED_NAMES = entries.map { it.eventName }.toSet()

        fun isAllowed(eventName: String): Boolean = eventName in ALLOWED_NAMES
    }
}

/**
 * Analytics dispatcher enforcing strict privacy:
 * Strips any sensitive properties and ensures only permitted non-sensitive events are recorded.
 */
object AnalyticsLogger {

    private val FORBIDDEN_KEYS = setOf(
        "image", "pixels", "pixel", "gps", "lat", "latitude", "lon", "longitude",
        "exif", "filename", "file_name", "path", "uri", "location"
    )

    data class EventRecord(
        val event: AnalyticsEvent,
        val params: Map<String, Any> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis()
    )

    private val recordedEvents = mutableListOf<EventRecord>()

    /**
     * Logs an analytics event safely.
     * Rejects forbidden parameters (GPS, EXIF, raw pixels, file paths, etc.).
     */
    fun logEvent(
        event: AnalyticsEvent,
        params: Map<String, Any> = emptyMap()
    ): EventRecord {
        // Filter out any sensitive parameters
        val sanitizedParams = params.filterKeys { key ->
            val lower = key.lowercase()
            FORBIDDEN_KEYS.none { lower.contains(it) }
        }.mapValues { (_, value) ->
            when (value) {
                is String -> {
                    // Strip path / URI prefixes if mistakenly passed
                    if (value.startsWith("/") || value.startsWith("content:") || value.startsWith("file:")) {
                        "sanitized_identifier"
                    } else {
                        value.take(64)
                    }
                }
                is Number, is Boolean -> value
                else -> value.toString().take(64)
            }
        }

        val record = EventRecord(
            event = event,
            params = sanitizedParams
        )
        synchronized(recordedEvents) {
            recordedEvents.add(record)
        }
        return record
    }

    fun getRecordedEvents(): List<EventRecord> = synchronized(recordedEvents) { recordedEvents.toList() }

    fun clear() = synchronized(recordedEvents) { recordedEvents.clear() }
}

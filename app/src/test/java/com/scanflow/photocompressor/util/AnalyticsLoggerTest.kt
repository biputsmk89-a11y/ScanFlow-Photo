package com.scanflow.photocompressor.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AnalyticsLoggerTest {

    @Before
    fun setup() {
        AnalyticsLogger.clear()
    }

    @Test
    fun `all allowed events are validated and recognized`() {
        val allowed = listOf(
            "tool_opened",
            "image_selected",
            "processing_started",
            "processing_completed",
            "processing_failed",
            "batch_started",
            "batch_completed",
            "export_clicked",
            "share_clicked",
            "pro_screen_viewed",
            "subscription_started"
        )
        for (name in allowed) {
            assertTrue("Event $name must be allowed", AnalyticsEvent.isAllowed(name))
        }
        assertFalse(AnalyticsEvent.isAllowed("unauthorized_event"))
    }

    @Test
    fun `logEvent strips sensitive parameters like GPS, EXIF, pixels and filenames`() {
        AnalyticsLogger.logEvent(
            AnalyticsEvent.PROCESSING_STARTED,
            mapOf(
                "tool" to "compress",
                "gps_latitude" to 45.123,
                "exif_iso" to 400,
                "image_pixels" to "raw_buffer",
                "filename" to "my_secret_passport.jpg",
                "duration_ms" to 250
            )
        )

        val recorded = AnalyticsLogger.getRecordedEvents()
        assertEquals(1, recorded.size)

        val params = recorded.first().params
        assertEquals("compress", params["tool"])
        assertEquals(250, params["duration_ms"])

        // Sensitive keys must be purged completely
        assertFalse(params.containsKey("gps_latitude"))
        assertFalse(params.containsKey("exif_iso"))
        assertFalse(params.containsKey("image_pixels"))
        assertFalse(params.containsKey("filename"))
    }
}

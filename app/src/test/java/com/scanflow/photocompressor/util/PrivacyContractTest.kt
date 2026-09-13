package com.scanflow.photocompressor.util

import org.junit.Assert.*
import org.junit.Test

class PrivacyContractTest {

    @Test
    fun `recordEvent creates sanitized non-sensitive telemetry event`() {
        val event = PrivacyContract.recordEvent(
            tool = "COMPRESS",
            durationMs = 120L,
            reductionPercent = 75.4f,
            isSuccess = true
        )

        assertEquals("compress", event.toolName)
        assertEquals(120L, event.durationMs)
        assertEquals(75.4f, event.reductionPercent)
        assertTrue(event.isSuccess)
        assertNull(event.errorCode)
    }

    @Test
    fun `recordEvent enforces abstract tool name without file paths`() {
        try {
            PrivacyContract.recordEvent(
                tool = "content://media/external/images/media/123",
                durationMs = 100L
            )
            fail("Expected IllegalArgumentException when tool name contains URI/path")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("must be an abstract category"))
        }
    }

    @Test
    fun `verifyNoSensitiveDataLogged passes for clean telemetry string`() {
        PrivacyContract.verifyNoSensitiveDataLogged("tool=compress,duration=150,status=ok")
    }

    @Test
    fun `verifyNoSensitiveDataLogged throws SecurityException when GPS or image payload is detected`() {
        assertThrows(SecurityException::class.java) {
            PrivacyContract.verifyNoSensitiveDataLogged("payload: GPS_LATITUDE=-6.2088")
        }

        assertThrows(SecurityException::class.java) {
            PrivacyContract.verifyNoSensitiveDataLogged("data: image/jpeg;base64,/9j/4AAQSkZJRg==")
        }
    }
}

package com.scanflow.photocompressor.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class ShareIntentHandlerTest {

    @Test
    fun `extractUris returns empty list when intent is null`() {
        val uris = ShareIntentHandler.extractUris(null)
        assertTrue(uris.isEmpty())
    }

    @Test
    fun `extractUris returns empty list when action is MAIN`() {
        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns Intent.ACTION_MAIN
            every { type } returns null
            every { hasExtra(Intent.EXTRA_STREAM) } returns false
        }
        val uris = ShareIntentHandler.extractUris(intent)
        assertTrue(uris.isEmpty())
    }

    @Test
    fun `extractUris extracts single URI from ACTION_SEND`() {
        val mockUri = mockk<Uri>()
        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns Intent.ACTION_SEND
            every { type } returns "image/jpeg"
            every { hasExtra(Intent.EXTRA_STREAM) } returns true
            every { getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns mockUri
        }

        val uris = ShareIntentHandler.extractUris(intent)
        assertEquals(1, uris.size)
        assertEquals(mockUri, uris[0])
    }

    @Test
    fun `extractUris extracts multiple URIs from ACTION_SEND_MULTIPLE`() {
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns Intent.ACTION_SEND_MULTIPLE
            every { type } returns "image/*"
            every { hasExtra(Intent.EXTRA_STREAM) } returns true
            every { getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) } returns arrayListOf(uri1, uri2)
        }

        val uris = ShareIntentHandler.extractUris(intent)
        assertEquals(2, uris.size)
        assertTrue(uris.contains(uri1))
        assertTrue(uris.contains(uri2))
    }

    @Test
    fun `extractUris falls back to ClipData when EXTRA_STREAM is null`() {
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        val clipItem1 = mockk<ClipData.Item> { every { uri } returns uri1 }
        val clipItem2 = mockk<ClipData.Item> { every { uri } returns uri2 }
        val clipData = mockk<ClipData> {
            every { itemCount } returns 2
            every { getItemAt(0) } returns clipItem1
            every { getItemAt(1) } returns clipItem2
        }

        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns Intent.ACTION_SEND_MULTIPLE
            every { type } returns "image/png"
            every { hasExtra(Intent.EXTRA_STREAM) } returns false
            every { getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) } returns null
            every { this@mockk.clipData } returns clipData
        }

        val uris = ShareIntentHandler.extractUris(intent)
        assertEquals(2, uris.size)
        assertEquals(uri1, uris[0])
        assertEquals(uri2, uris[1])
    }
}

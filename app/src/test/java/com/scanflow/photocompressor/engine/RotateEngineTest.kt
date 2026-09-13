package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import com.scanflow.photocompressor.domain.model.MetadataOption
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RotateEngineTest {

    private lateinit var rotateEngine: RotateEngine

    @Before
    fun setup() {
        rotateEngine = RotateEngine()
    }

    @Test
    fun `rotate with 0 or 360 degrees returns original bitmap`() {
        val mockBitmap = mockk<Bitmap>()
        val result0 = rotateEngine.rotate(mockBitmap, 0f)
        val result360 = rotateEngine.rotate(mockBitmap, 360f)

        assertSame(mockBitmap, result0)
        assertSame(mockBitmap, result360)
    }

    @Test
    fun `flip with false flags returns original bitmap`() {
        val mockBitmap = mockk<Bitmap>()
        val result = rotateEngine.flip(mockBitmap, horizontal = false, vertical = false)

        assertSame(mockBitmap, result)
    }

    @Test
    fun `MetadataEngine normalizes TAG_ORIENTATION to ORIENTATION_NORMAL on output`() {
        val metadataEngine = MetadataEngine(mockk(relaxed = true))
        val rawAttributes = mapOf(
            ExifInterface.TAG_ORIENTATION to ExifInterface.ORIENTATION_ROTATE_90.toString(), // 6
            ExifInterface.TAG_MAKE to "Google",
            ExifInterface.TAG_MODEL to "Pixel"
        )

        val filtered = metadataEngine.filterAttributes(
            attributes = rawAttributes,
            option = MetadataOption.KEEP_METADATA,
            normalizeOrientation = true
        )

        // Rule 64: Because pixels were normalized during decode, output orientation tag
        // must be set to ORIENTATION_NORMAL (1) to display correctly without double rotation
        assertEquals(
            ExifInterface.ORIENTATION_NORMAL.toString(),
            filtered[ExifInterface.TAG_ORIENTATION]
        )
        assertEquals("Google", filtered[ExifInterface.TAG_MAKE])
        assertEquals("Pixel", filtered[ExifInterface.TAG_MODEL])
    }

    @Test
    fun `AdaptiveConcurrencyPolicy strictly enforces stability over speed`() {
        // Default concurrency must always be 1
        assertEquals(1, AdaptiveConcurrencyPolicy.DEFAULT_CONCURRENCY)
        assertEquals(1, AdaptiveConcurrencyPolicy.resolveConcurrency(null, allowAdaptive = false))

        // On low memory device, concurrency must be 1
        val lowRamContext = mockk<android.content.Context>(relaxed = true)
        val activityManager = mockk<android.app.ActivityManager>()
        every { lowRamContext.getSystemService(android.content.Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.isLowRamDevice } returns true

        val lowRamConcurrency = AdaptiveConcurrencyPolicy.resolveConcurrency(lowRamContext, allowAdaptive = true)
        assertEquals(1, lowRamConcurrency)

        // Max concurrency can never exceed 2
        assertTrue(AdaptiveConcurrencyPolicy.MAX_CONCURRENCY <= 2)
    }
}

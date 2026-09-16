package com.scanflow.photocompressor.data.storage

import com.scanflow.photocompressor.domain.model.ConflictStrategy
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.OperationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileNamingEngineTest {

    private lateinit var fileNamingEngine: FileNamingEngine

    @Before
    fun setup() {
        fileNamingEngine = FileNamingEngine()
    }

    @Test
    fun `sanitizeBaseName strips invalid characters and control chars`() {
        val unsafeName = "photo:with*invalid?chars<and>slashes/and\\bars|.jpg"
        val sanitized = fileNamingEngine.sanitizeBaseName(unsafeName)

        assertEquals("photo_with_invalid_chars_and_slashes_and_bars", sanitized)
        assertFalse(sanitized.contains(":"))
        assertFalse(sanitized.contains("*"))
        assertFalse(sanitized.contains("?"))
        assertFalse(sanitized.contains("<"))
        assertFalse(sanitized.contains(">"))
        assertFalse(sanitized.contains("/"))
        assertFalse(sanitized.contains("\\"))
        assertFalse(sanitized.contains("|"))
    }

    @Test
    fun `sanitizeBaseName handles null bytes and control chars`() {
        val controlCharsName = "image\u0000test\u001Fname.png"
        val sanitized = fileNamingEngine.sanitizeBaseName(controlCharsName)
        assertEquals("image_test_name", sanitized)
    }

    @Test
    fun `sanitizeBaseName falls back to image when completely empty or invalid`() {
        val allInvalid = ":::***???///\\\\\\"
        val sanitized = fileNamingEngine.sanitizeBaseName(allInvalid)
        assertEquals("image", sanitized)
    }

    @Test
    fun `generateFileName produces standard compressed naming`() {
        val fileName = fileNamingEngine.generateFileName(
            originalName = "IMG_1234.jpg",
            operationType = OperationType.COMPRESS,
            targetFormat = ImageFormat.JPEG
        )
        assertEquals("IMG_1234_compressed.jpg", fileName)
    }

    @Test
    fun `generateFileName produces standard resized naming`() {
        val fileName = fileNamingEngine.generateFileName(
            originalName = "IMG_1234.jpg",
            operationType = OperationType.RESIZE,
            targetFormat = ImageFormat.JPEG
        )
        assertEquals("IMG_1234_resized.jpg", fileName)
    }

    @Test
    fun `generateFileName produces standard format conversion naming`() {
        val fileName = fileNamingEngine.generateFileName(
            originalName = "IMG_1234.jpg",
            operationType = OperationType.CONVERT,
            targetFormat = ImageFormat.WEBP
        )
        assertEquals("IMG_1234_webp.webp", fileName)
    }

    @Test
    fun `generateFileName produces standard naming for all new operation types`() {
        assertEquals(
            "IMG_1234_document.pdf",
            fileNamingEngine.generateFileName("IMG_1234.jpg", OperationType.PDF)
        )
        assertEquals(
            "IMG_1234_passport.jpg",
            fileNamingEngine.generateFileName("IMG_1234.jpg", OperationType.PASSPORT, ImageFormat.JPEG)
        )
        assertEquals(
            "IMG_1234_social.jpg",
            fileNamingEngine.generateFileName("IMG_1234.jpg", OperationType.SOCIAL, ImageFormat.JPEG)
        )
        assertEquals(
            "IMG_1234_whatsapp.jpg",
            fileNamingEngine.generateFileName("IMG_1234.jpg", OperationType.WHATSAPP, ImageFormat.JPEG)
        )
    }

    @Test
    fun `resolveConflict returns original name if no file collision`() {
        val resolved = fileNamingEngine.resolveConflict(
            targetFileName = "IMG_1234_compressed.jpg",
            strategy = ConflictStrategy.INCREMENT,
            existsPredicate = { false }
        )
        assertEquals("IMG_1234_compressed.jpg", resolved)
    }

    @Test
    fun `resolveConflict increments suffix on collision`() {
        val existingFiles = setOf(
            "IMG_1234_compressed.jpg",
            "IMG_1234_compressed_1.jpg",
            "IMG_1234_compressed_2.jpg"
        )

        val resolved = fileNamingEngine.resolveConflict(
            targetFileName = "IMG_1234_compressed.jpg",
            strategy = ConflictStrategy.INCREMENT,
            existsPredicate = { it in existingFiles }
        )

        assertEquals("IMG_1234_compressed_3.jpg", resolved)
    }

    @Test
    fun `resolveConflict timestamp strategy appends timestamp on collision`() {
        val existingFiles = setOf("IMG_1234_compressed.jpg")

        val resolved = fileNamingEngine.resolveConflict(
            targetFileName = "IMG_1234_compressed.jpg",
            strategy = ConflictStrategy.TIMESTAMP,
            existsPredicate = { it in existingFiles }
        )

        assertTrue(resolved.startsWith("IMG_1234_compressed_"))
        assertTrue(resolved.endsWith(".jpg"))
        assertTrue(resolved != "IMG_1234_compressed.jpg")
    }

    @Test
    fun `resolveConflict overwrite strategy preserves exact name`() {
        val existingFiles = setOf("IMG_1234_compressed.jpg")

        val resolved = fileNamingEngine.resolveConflict(
            targetFileName = "IMG_1234_compressed.jpg",
            strategy = ConflictStrategy.OVERWRITE,
            existsPredicate = { it in existingFiles }
        )

        assertEquals("IMG_1234_compressed.jpg", resolved)
    }
}

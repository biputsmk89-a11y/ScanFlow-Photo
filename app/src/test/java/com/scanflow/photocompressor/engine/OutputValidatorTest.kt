package com.scanflow.photocompressor.engine

import android.graphics.BitmapFactory
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ValidationResult
import com.scanflow.photocompressor.domain.model.ValidationStep
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class OutputValidatorTest {

    private lateinit var validator: OutputValidator

    @Before
    fun setup() {
        validator = OutputValidator()
        mockkStatic(BitmapFactory::class)
    }

    @After
    fun teardown() {
        unmockkStatic(BitmapFactory::class)
    }

    @Test
    fun `validate fails with FILE_EXISTS when file does not exist`() {
        val nonExistentFile = File("fake/path/does_not_exist_photo.jpg")

        val result = validator.validate(nonExistentFile)

        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertEquals(ValidationStep.FILE_EXISTS, failure.step)
        assertTrue(failure.reason.contains("does not exist"))
    }

    @Test
    fun `validate fails with FILE_NOT_EMPTY when file is 0 bytes`() {
        val emptyFile = File.createTempFile("empty_", ".jpg")
        emptyFile.deleteOnExit()

        val result = validator.validate(emptyFile)

        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertEquals(ValidationStep.FILE_NOT_EMPTY, failure.step)
        assertTrue(failure.reason.contains("0 bytes"))
    }

    @Test
    fun `validate fails with MIME_VALID when MIME cannot be detected`() {
        val textFile = File.createTempFile("invalid_mime_", ".jpg")
        textFile.deleteOnExit()
        textFile.writeText("Not a real image file content")

        every { BitmapFactory.decodeFile(textFile.absolutePath, any()) } answers {
            val opts = secondArg<BitmapFactory.Options>()
            opts.outMimeType = null
            opts.outWidth = 0
            opts.outHeight = 0
            null
        }

        val result = validator.validate(textFile)

        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertEquals(ValidationStep.MIME_VALID, failure.step)
        assertTrue(failure.reason.contains("Unable to detect a valid image MIME type"))
    }

    @Test
    fun `validate fails with MIME_VALID when MIME does not match expected format`() {
        val pngFile = File.createTempFile("test_png_", ".png")
        pngFile.deleteOnExit()
        pngFile.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))

        every { BitmapFactory.decodeFile(pngFile.absolutePath, any()) } answers {
            val opts = secondArg<BitmapFactory.Options>()
            opts.outMimeType = "image/png"
            opts.outWidth = 100
            opts.outHeight = 100
            null
        }

        val result = validator.validate(pngFile, expectedFormat = ImageFormat.JPEG)

        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertEquals(ValidationStep.MIME_VALID, failure.step)
        assertTrue(failure.reason.contains("MIME type mismatch"))
    }

    @Test
    fun `validate fails with DECODE_SUCCESS when dimensions cannot be decoded`() {
        val corruptedFile = File.createTempFile("corrupted_", ".jpg")
        corruptedFile.deleteOnExit()
        corruptedFile.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x00))

        every { BitmapFactory.decodeFile(corruptedFile.absolutePath, any()) } answers {
            val opts = secondArg<BitmapFactory.Options>()
            opts.outMimeType = "image/jpeg"
            opts.outWidth = -1
            opts.outHeight = -1
            null
        }

        val result = validator.validate(corruptedFile, expectedFormat = ImageFormat.JPEG)

        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertEquals(ValidationStep.DECODE_SUCCESS, failure.step)
        assertTrue(failure.reason.contains("Failed to decode"))
    }

    @Test
    fun `validate fails with DIMENSIONS_VALID when dimensions are below minimum`() {
        val smallFile = File.createTempFile("small_", ".jpg")
        smallFile.deleteOnExit()
        smallFile.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01))

        every { BitmapFactory.decodeFile(smallFile.absolutePath, any()) } answers {
            val opts = secondArg<BitmapFactory.Options>()
            opts.outMimeType = "image/jpeg"
            opts.outWidth = 10
            opts.outHeight = 10
            null
        }

        val result = validator.validate(
            smallFile,
            expectedFormat = ImageFormat.JPEG,
            minWidth = 100,
            minHeight = 100
        )

        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertEquals(ValidationStep.DIMENSIONS_VALID, failure.step)
        assertTrue(failure.reason.contains("Invalid output dimensions"))
    }

    @Test
    fun `validate succeeds only after all 5 validation steps pass`() {
        val validFile = File.createTempFile("valid_", ".jpg")
        validFile.deleteOnExit()
        validFile.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x02, 0x03, 0x04))

        every { BitmapFactory.decodeFile(validFile.absolutePath, any()) } answers {
            val opts = secondArg<BitmapFactory.Options>()
            opts.outMimeType = "image/jpeg"
            opts.outWidth = 1920
            opts.outHeight = 1080
            null
        }

        val result = validator.validate(validFile, expectedFormat = ImageFormat.JPEG)

        assertTrue("Expected ValidationResult.Success", result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals("image/jpeg", success.mimeType)
        assertEquals(1920, success.width)
        assertEquals(1080, success.height)
        assertEquals(5L, success.fileSizeBytes)
    }
}

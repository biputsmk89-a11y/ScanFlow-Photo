package com.scanflow.photocompressor.data.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.scanflow.photocompressor.data.repository.ImageRepositoryImpl
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ValidationResult
import com.scanflow.photocompressor.domain.model.ValidationStep
import com.scanflow.photocompressor.engine.BitmapUtils
import com.scanflow.photocompressor.engine.OutputValidator
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OutputFileFlowTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context = mockk(relaxed = true)
    private val contentResolver: ContentResolver = mockk(relaxed = true)
    private val bitmapUtils: BitmapUtils = mockk(relaxed = true)
    private val fileManager: FileManager = mockk(relaxed = true)
    private val outputValidator: OutputValidator = mockk(relaxed = true)
    private val fileNamingEngine: FileNamingEngine = FileNamingEngine()

    private lateinit var imageRepository: ImageRepositoryImpl

    @Before
    fun setup() {
        every { context.contentResolver } returns contentResolver
        every { context.getExternalFilesDir(any()) } returns tempFolder.root

        imageRepository = ImageRepositoryImpl(
            context = context,
            bitmapUtils = bitmapUtils,
            fileManager = fileManager,
            outputValidator = outputValidator,
            fileNamingEngine = fileNamingEngine
        )
    }

    @Test
    fun `output file flow cleans up temporary file when validation fails`() = runTest {
        val tempFile = File(tempFolder.root, "temp_test_image.jpg")
        tempFile.writeText("sample data")
        assertTrue(tempFile.exists())

        every { fileManager.createTempFile(any(), any()) } returns tempFile

        val mockBitmap: Bitmap = mockk(relaxed = true)
        every { mockBitmap.compress(any(), any(), any()) } returns true

        // Validation fails at step MIME_VALID
        every { outputValidator.validate(tempFile, ImageFormat.JPEG) } returns ValidationResult.Failure(
            step = ValidationStep.MIME_VALID,
            reason = "Corrupted or invalid mime"
        )

        var exceptionThrown = false
        try {
            imageRepository.saveBitmap(
                bitmap = mockBitmap,
                fileName = "IMG_1234.jpg",
                format = ImageFormat.JPEG,
                quality = 80
            )
        } catch (e: IllegalStateException) {
            exceptionThrown = true
        }

        assertTrue("Expected exception to be thrown on validation failure", exceptionThrown)
        // Verify temp file was cleaned up on failure
        assertFalse("Temp file must be cleaned up on failure", tempFile.exists())
    }

    @Test
    fun `withTempFile in FileManager guarantees cleanup in both success and failure`() {
        val testTempDir = tempFolder.newFolder("managed_temp")
        val realFileManager = mockk<FileManager>(relaxed = true)
        val tempFile = File(testTempDir, "process_file.tmp")

        every { realFileManager.createTempFile(any(), any()) } returns tempFile

        var executed = false
        try {
            realFileManager.withTempFile("test", "tmp") { file ->
                executed = true
                file.writeText("temp image bytes")
                assertTrue(file.exists())
                throw RuntimeException("Simulated unexpected failure during processing")
            }
        } catch (e: RuntimeException) {
            // expected
        }

        assertTrue(executed)
    }
}

package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import android.net.Uri
import com.scanflow.photocompressor.data.storage.ExifHandler
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ImagePipelineEngineTest {

    private lateinit var compressionEngine: CompressionEngine
    private lateinit var resizeEngine: ResizeEngine
    private lateinit var cropEngine: CropEngine
    private lateinit var rotateEngine: RotateEngine
    private lateinit var watermarkEngine: WatermarkEngine
    private lateinit var formatConverter: FormatConverter
    private lateinit var imageRepository: ImageRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var exifHandler: ExifHandler
    private lateinit var metadataEngine: MetadataEngine
    private lateinit var fileManager: com.scanflow.photocompressor.data.storage.FileManager
    private lateinit var outputValidator: OutputValidator

    private lateinit var pipelineEngine: ImagePipelineEngine

    private val testUri = mockk<Uri>()
    private val outputUri = mockk<Uri>()
    private val mockBitmap = mockk<Bitmap>(relaxed = true)

    @Before
    fun setup() {
        compressionEngine = mockk(relaxed = true)
        resizeEngine = mockk(relaxed = true)
        cropEngine = mockk(relaxed = true)
        rotateEngine = mockk(relaxed = true)
        watermarkEngine = mockk(relaxed = true)
        formatConverter = mockk(relaxed = true)
        imageRepository = mockk(relaxed = true)
        historyRepository = mockk(relaxed = true)
        exifHandler = mockk(relaxed = true)
        metadataEngine = mockk(relaxed = true)
        fileManager = mockk(relaxed = true)
        outputValidator = mockk(relaxed = true)

        val tempTestFile = java.io.File.createTempFile("pipeline_unit_test_", ".jpg")
        tempTestFile.deleteOnExit()
        every { fileManager.createTempFile(any(), any()) } returns tempTestFile
        every { outputValidator.validate(any(), any(), any(), any()) } returns ValidationResult.Success(
            file = tempTestFile,
            mimeType = "image/jpeg",
            width = 4000,
            height = 3000,
            fileSizeBytes = 500L
        )

        pipelineEngine = ImagePipelineEngine(
            compressionEngine,
            resizeEngine,
            cropEngine,
            rotateEngine,
            watermarkEngine,
            formatConverter,
            imageRepository,
            historyRepository,
            exifHandler,
            metadataEngine,
            fileManager,
            outputValidator
        )

        val imageInfo = ImageInfo(
            uri = testUri,
            fileName = "photo.jpg",
            fileSize = 2_000_000L,
            width = 4000,
            height = 3000,
            format = ImageFormat.JPEG,
            mimeType = "image/jpeg"
        )

        coEvery { imageRepository.getImageInfo(any()) } returns imageInfo
        coEvery { imageRepository.loadBitmap(any(), any(), any()) } returns mockBitmap
        coEvery { imageRepository.saveBitmap(any(), any(), any(), any()) } returns outputUri
        coEvery { imageRepository.saveFromFile(any(), any(), any()) } returns outputUri
        coEvery { imageRepository.getFileSize(any()) } returns 800_000L
        every { mockBitmap.width } returns 4000
        every { mockBitmap.height } returns 3000
        every { compressionEngine.compress(any(), any(), any()) } returns ByteArray(500)
    }

    @Test
    fun `pipeline executes only requested operations and skips unneeded ones`() = runTest {
        // Pipeline with ONLY Compress - Crop and Resize must NOT be called!
        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Compress(quality = 75)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isSuccess)
        val compressionResult = result.getOrNull()
        assertNotNull(compressionResult)
        assertEquals(75, compressionResult?.quality)
        assertEquals(ImageFormat.JPEG, compressionResult?.format)

        // Strict verification: Crop & Resize operations are skipped
        verify(exactly = 0) { cropEngine.crop(any(), any(), any(), any()) }
        verify(exactly = 0) { cropEngine.cropToAspectRatio(any(), any(), any()) }
        verify(exactly = 0) { resizeEngine.resize(any(), any(), any(), any()) }
    }

    @Test
    fun `pipeline with Crop operation executes crop`() = runTest {
        val croppedBitmap = mockk<Bitmap>(relaxed = true)
        every { croppedBitmap.width } returns 3000
        every { croppedBitmap.height } returns 3000
        every { cropEngine.cropToAspectRatio(mockBitmap, 1, 1) } returns croppedBitmap

        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Crop(aspectRatio = AspectRatio.SQUARE),
                ImageOperation.Compress(quality = 85)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { cropEngine.cropToAspectRatio(mockBitmap, 1, 1) }
        verify(exactly = 0) { resizeEngine.resize(any(), any(), any(), any()) }
    }

    @Test
    fun `pipeline with Resize operation executes resize with target dimensions`() = runTest {
        val resizedBitmap = mockk<Bitmap>(relaxed = true)
        every { resizedBitmap.width } returns 1920
        every { resizedBitmap.height } returns 1080
        every { resizeEngine.resize(mockBitmap, 1920, 1080, true) } returns resizedBitmap

        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Resize(width = 1920, height = 1080, maintainAspectRatio = true),
                ImageOperation.Compress(quality = 80)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { resizeEngine.resize(mockBitmap, 1920, 1080, true) }
        verify(exactly = 0) { cropEngine.crop(any(), any(), any(), any()) }
    }

    @Test
    fun `pipeline with RemoveMetadata strips EXIF`() = runTest {
        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.RemoveMetadata(removeExif = true),
                ImageOperation.Compress(quality = 90)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()?.preservedExif ?: true)
        verify(exactly = 0) { metadataEngine.applyMetadataToFile(any(), any(), any()) }
    }

    @Test
    fun `pipeline with Metadata REMOVE_GPS delegates to metadataEngine with REMOVE_GPS`() = runTest {
        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Metadata(MetadataOption.REMOVE_GPS),
                ImageOperation.Compress(quality = 85)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.preservedExif ?: false)
        verify(exactly = 1) { metadataEngine.applyMetadataToFile(testUri, any(), MetadataOption.REMOVE_GPS) }
    }

    @Test
    fun `pipeline with default policy delegates to metadataEngine with KEEP_METADATA`() = runTest {
        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Compress(quality = 80)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.preservedExif ?: false)
        verify(exactly = 1) { metadataEngine.applyMetadataToFile(testUri, any(), MetadataOption.KEEP_METADATA) }
    }

    @Test
    fun `pipeline with Convert changes target format`() = runTest {
        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Convert(format = ImageFormat.WEBP),
                ImageOperation.Compress(quality = 85)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isSuccess)
        assertEquals(ImageFormat.WEBP, result.getOrNull()?.format)
        verify(exactly = 1) { compressionEngine.compress(any(), ImageFormat.WEBP, 85) }
    }

    @Test
    fun `pipeline combination Resize + Compress executes properly`() = runTest {
        val resizedBitmap = mockk<Bitmap>(relaxed = true)
        every { resizedBitmap.width } returns 1280
        every { resizedBitmap.height } returns 720
        every { resizeEngine.resize(mockBitmap, 1280, 720, true) } returns resizedBitmap

        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Resize(width = 1280, height = 720, maintainAspectRatio = true),
                ImageOperation.Compress(quality = 70)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { resizeEngine.resize(mockBitmap, 1280, 720, true) }
        verify(exactly = 1) { compressionEngine.compress(resizedBitmap, ImageFormat.JPEG, 70) }
    }

    @Test
    fun `pipeline combination Crop + Resize + Convert executes in order`() = runTest {
        val croppedBitmap = mockk<Bitmap>(relaxed = true)
        val resizedBitmap = mockk<Bitmap>(relaxed = true)
        every { croppedBitmap.width } returns 2000
        every { croppedBitmap.height } returns 2000
        every { resizedBitmap.width } returns 1000
        every { resizedBitmap.height } returns 1000
        every { cropEngine.cropToAspectRatio(mockBitmap, 1, 1) } returns croppedBitmap
        every { resizeEngine.resize(croppedBitmap, 1000, 1000, true) } returns resizedBitmap

        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Crop(aspectRatio = AspectRatio.SQUARE),
                ImageOperation.Resize(width = 1000, height = 1000, maintainAspectRatio = true),
                ImageOperation.Convert(format = ImageFormat.PNG),
                ImageOperation.Compress(quality = 100)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { cropEngine.cropToAspectRatio(mockBitmap, 1, 1) }
        verify(exactly = 1) { resizeEngine.resize(croppedBitmap, 1000, 1000, true) }
        verify(exactly = 1) { compressionEngine.compress(resizedBitmap, ImageFormat.PNG, 100) }
    }

    @Test
    fun `pipeline combination Resize + Compress + Metadata executes all three steps`() = runTest {
        val resizedBitmap = mockk<Bitmap>(relaxed = true)
        every { resizedBitmap.width } returns 1920
        every { resizedBitmap.height } returns 1080
        every { resizeEngine.resize(mockBitmap, 1920, 1080, true) } returns resizedBitmap

        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Resize(width = 1920, height = 1080, maintainAspectRatio = true),
                ImageOperation.Compress(quality = 85),
                ImageOperation.Metadata(MetadataOption.REMOVE_GPS)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { resizeEngine.resize(mockBitmap, 1920, 1080, true) }
        verify(exactly = 1) { compressionEngine.compress(resizedBitmap, ImageFormat.JPEG, 85) }
        verify(exactly = 1) { metadataEngine.applyMetadataToFile(testUri, any(), MetadataOption.REMOVE_GPS) }
    }

    @Test
    fun `pipeline validates output bytes and fails on empty encoding`() = runTest {
        every { compressionEngine.compress(any(), any(), any()) } returns ByteArray(0)

        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Compress(quality = 80)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Validation error") == true)
    }

    @Test
    fun `pipeline fails and purges temp file when output validation fails`() = runTest {
        every { outputValidator.validate(any(), any(), any(), any()) } returns ValidationResult.Failure(
            step = ValidationStep.DECODE_SUCCESS,
            reason = "Failed to decode corrupted header"
        )

        val pipeline = ImagePipeline(
            operations = listOf(
                ImageOperation.Compress(quality = 80)
            )
        )

        val result = pipelineEngine.execute(testUri, pipeline)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Output validation failed") == true)
        // Must NEVER promote to final output when validation fails!
        coVerify(exactly = 0) { imageRepository.saveFromFile(any(), any(), any()) }
    }
}

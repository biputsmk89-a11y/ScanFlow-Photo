package com.scanflow.photocompressor.engine

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.scanflow.photocompressor.domain.model.MetadataOption
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class MetadataEngineTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var metadataEngine: MetadataEngine

    private val sampleAttributes = mapOf(
        // Camera
        ExifInterface.TAG_MAKE to "Sony",
        ExifInterface.TAG_MODEL to "ILCE-7M4",
        // Lens
        ExifInterface.TAG_LENS_MAKE to "Sony",
        ExifInterface.TAG_LENS_MODEL to "FE 24-70mm F2.8 GM II",
        ExifInterface.TAG_FOCAL_LENGTH to "50/1",
        ExifInterface.TAG_F_NUMBER to "2.8",
        ExifInterface.TAG_ISO_SPEED_RATINGS to "400",
        // Date
        ExifInterface.TAG_DATETIME to "2026:09:12 12:00:00",
        ExifInterface.TAG_DATETIME_ORIGINAL to "2026:09:12 11:30:00",
        // Software
        ExifInterface.TAG_SOFTWARE to "ScanFlow Pro v1.0",
        // Orientation
        ExifInterface.TAG_ORIENTATION to "6", // 90 deg rotation
        // EXIF general
        ExifInterface.TAG_COLOR_SPACE to "1",
        ExifInterface.TAG_FLASH to "0",
        ExifInterface.TAG_EXPOSURE_TIME to "1/250",
        // GPS
        ExifInterface.TAG_GPS_LATITUDE to "37/1,46/1,29/1",
        ExifInterface.TAG_GPS_LATITUDE_REF to "N",
        ExifInterface.TAG_GPS_LONGITUDE to "122/1,25/1,10/1",
        ExifInterface.TAG_GPS_LONGITUDE_REF to "W",
        ExifInterface.TAG_GPS_ALTITUDE to "15/1",
        ExifInterface.TAG_GPS_ALTITUDE_REF to "0",
        ExifInterface.TAG_GPS_TIMESTAMP to "11:30:00",
        ExifInterface.TAG_GPS_DATESTAMP to "2026:09:12"
    )

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        metadataEngine = MetadataEngine(context)
    }

    @Test
    fun `filterAttributes with KEEP_METADATA retains all 7 categories`() {
        val filtered = metadataEngine.filterAttributes(sampleAttributes, MetadataOption.KEEP_METADATA)

        assertEquals(sampleAttributes.size, filtered.size)
        // Camera
        assertEquals("Sony", filtered[ExifInterface.TAG_MAKE])
        assertEquals("ILCE-7M4", filtered[ExifInterface.TAG_MODEL])
        // Lens
        assertEquals("FE 24-70mm F2.8 GM II", filtered[ExifInterface.TAG_LENS_MODEL])
        assertEquals("2.8", filtered[ExifInterface.TAG_F_NUMBER])
        // Date
        assertEquals("2026:09:12 11:30:00", filtered[ExifInterface.TAG_DATETIME_ORIGINAL])
        // Software
        assertEquals("ScanFlow Pro v1.0", filtered[ExifInterface.TAG_SOFTWARE])
        // Orientation
        assertEquals("6", filtered[ExifInterface.TAG_ORIENTATION])
        // General EXIF
        assertEquals("1/250", filtered[ExifInterface.TAG_EXPOSURE_TIME])
        // GPS
        assertEquals("37/1,46/1,29/1", filtered[ExifInterface.TAG_GPS_LATITUDE])
        assertEquals("122/1,25/1,10/1", filtered[ExifInterface.TAG_GPS_LONGITUDE])
    }

    @Test
    fun `filterAttributes with REMOVE_GPS strips location but preserves camera, lens, date, software, orientation, and EXIF`() {
        val filtered = metadataEngine.filterAttributes(sampleAttributes, MetadataOption.REMOVE_GPS)

        // GPS tags must be completely stripped
        assertNull(filtered[ExifInterface.TAG_GPS_LATITUDE])
        assertNull(filtered[ExifInterface.TAG_GPS_LATITUDE_REF])
        assertNull(filtered[ExifInterface.TAG_GPS_LONGITUDE])
        assertNull(filtered[ExifInterface.TAG_GPS_LONGITUDE_REF])
        assertNull(filtered[ExifInterface.TAG_GPS_ALTITUDE])
        assertNull(filtered[ExifInterface.TAG_GPS_TIMESTAMP])
        assertNull(filtered[ExifInterface.TAG_GPS_DATESTAMP])
        assertFalse(filtered.keys.any { it in MetadataEngine.GPS_TAGS })

        // Camera, Lens, Date, Software, Orientation, and EXIF must all be retained
        assertEquals("Sony", filtered[ExifInterface.TAG_MAKE])
        assertEquals("ILCE-7M4", filtered[ExifInterface.TAG_MODEL])
        assertEquals("FE 24-70mm F2.8 GM II", filtered[ExifInterface.TAG_LENS_MODEL])
        assertEquals("2026:09:12 11:30:00", filtered[ExifInterface.TAG_DATETIME_ORIGINAL])
        assertEquals("ScanFlow Pro v1.0", filtered[ExifInterface.TAG_SOFTWARE])
        assertEquals("6", filtered[ExifInterface.TAG_ORIENTATION])
        assertEquals("1/250", filtered[ExifInterface.TAG_EXPOSURE_TIME])
    }

    @Test
    fun `filterAttributes with REMOVE_ALL removes all metadata tags`() {
        val filtered = metadataEngine.filterAttributes(sampleAttributes, MetadataOption.REMOVE_ALL)

        assertTrue("Expected empty map for REMOVE_ALL", filtered.isEmpty())
    }

    @Test
    fun `source file is NEVER modified during metadata operations`() {
        // Create an original dummy source file
        val sourceFile = File.createTempFile("source_photo_", ".jpg")
        sourceFile.deleteOnExit()
        val originalBytes = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55)
        sourceFile.writeBytes(originalBytes)
        val originalLength = sourceFile.length()
        val sourceUri = mockk<Uri>()

        // Mock reading from source
        every { contentResolver.openInputStream(sourceUri) } answers {
            ByteArrayInputStream(sourceFile.readBytes())
        }

        val destinationFile = File.createTempFile("dest_photo_", ".jpg")
        destinationFile.deleteOnExit()

        // Call applyMetadataToFile with REMOVE_ALL
        metadataEngine.applyMetadataToFile(sourceUri, destinationFile, MetadataOption.REMOVE_ALL)

        // STRICT VERIFICATION: Source file must remain 100% unchanged!
        assertEquals(originalLength, sourceFile.length())
        assertArrayEquals(originalBytes, sourceFile.readBytes())

        // Ensure ContentResolver never opened an output stream on the source URI!
        verify(exactly = 0) { contentResolver.openOutputStream(sourceUri, any()) }
        verify(exactly = 0) { contentResolver.openOutputStream(sourceUri) }
    }

    @Test
    fun `extractMetadata populates structured models accurately`() {
        val testUri = mockk<Uri>()
        // Spy engine to return sampleAttributes for readRawAttributes
        val engineSpy = spyk(metadataEngine)
        every { engineSpy.readRawAttributes(testUri) } returns sampleAttributes

        val metadata = engineSpy.extractMetadata(testUri)

        // Camera
        assertEquals("Sony", metadata.camera.make)
        assertEquals("ILCE-7M4", metadata.camera.model)
        assertEquals("Sony ILCE-7M4", metadata.camera.displayName)

        // Lens
        assertEquals("FE 24-70mm F2.8 GM II", metadata.lens.lensModel)
        assertEquals("2.8", metadata.lens.fNumber)
        assertEquals("400", metadata.lens.iso)

        // Date
        assertEquals("2026:09:12 11:30:00", metadata.date.primaryDate)

        // Software
        assertEquals("ScanFlow Pro v1.0", metadata.software)

        // Orientation
        assertEquals(6, metadata.orientation.tagValue)
        assertEquals(90, metadata.orientation.rotationDegrees)

        // General EXIF
        assertEquals("1/250", metadata.exif.exposureTime)
        assertEquals("0", metadata.exif.flash)

        // Flags
        assertTrue(metadata.hasExif)
    }
}

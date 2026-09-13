package com.scanflow.photocompressor.data.storage

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles EXIF metadata reading, writing, and copying between image files.
 */
@Singleton
class ExifHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Key EXIF tags to preserve during processing
    private val preservableTags = listOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_ISO_SPEED_RATINGS,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT
    )

    /**
     * Read EXIF data from a URI.
     * @return Map of tag names to values.
     */
    fun readExif(uri: Uri): Map<String, String> {
        val exifData = mutableMapOf<String, String>()
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return exifData
            val exif = ExifInterface(inputStream)
            inputStream.close()

            for (tag in preservableTags) {
                exif.getAttribute(tag)?.let { value ->
                    exifData[tag] = value
                }
            }
        } catch (e: Exception) {
            // EXIF reading is optional, don't crash
        }
        return exifData
    }

    /**
     * Write EXIF data to a file.
     */
    fun writeExif(file: File, exifData: Map<String, String>) {
        try {
            val exif = ExifInterface(file)
            for ((tag, value) in exifData) {
                exif.setAttribute(tag, value)
            }
            exif.saveAttributes()
        } catch (e: Exception) {
            // EXIF writing is optional
        }
    }

    /**
     * Copy EXIF data from source URI to destination file.
     */
    fun copyExif(sourceUri: Uri, destFile: File) {
        val exifData = readExif(sourceUri)
        if (exifData.isNotEmpty()) {
            writeExif(destFile, exifData)
        }
    }

    /**
     * Get a human-readable summary of EXIF data.
     */
    fun getExifSummary(uri: Uri): String {
        val data = readExif(uri)
        if (data.isEmpty()) return "No EXIF data"

        val parts = mutableListOf<String>()
        data[ExifInterface.TAG_MAKE]?.let { parts.add("Camera: $it") }
        data[ExifInterface.TAG_MODEL]?.let { parts.add(it) }
        data[ExifInterface.TAG_DATETIME_ORIGINAL]?.let { parts.add("Date: $it") }
        data[ExifInterface.TAG_FOCAL_LENGTH]?.let { parts.add("Focal: ${it}mm") }
        data[ExifInterface.TAG_F_NUMBER]?.let { parts.add("f/$it") }
        data[ExifInterface.TAG_ISO_SPEED_RATINGS]?.let { parts.add("ISO $it") }

        return parts.joinToString(" • ")
    }
}

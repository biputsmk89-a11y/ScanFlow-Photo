package com.scanflow.photocompressor.engine

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.scanflow.photocompressor.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine responsible for inspecting, extracting, filtering, and applying image metadata (EXIF).
 *
 * Supported Metadata Categories:
 * 1. EXIF (Flash, White Balance, Exposure Time, Exposure Program, Color Space, etc.)
 * 2. GPS (Latitude, Longitude, Altitude, Timestamp, Datestamp, Speed, Direction, etc.)
 * 3. Camera (Make, Model)
 * 4. Lens (Lens Make, Lens Model, Focal Length, F-Number, ISO, 35mm equivalent)
 * 5. Date (DateTime, DateTimeOriginal, DateTimeDigitized, SubSecTime, OffsetTime)
 * 6. Software (Software tag)
 * 7. Orientation (Orientation tag)
 *
 * Supported Policies:
 * - [MetadataOption.KEEP_METADATA]: Retains all supported tags.
 * - [MetadataOption.REMOVE_GPS]: Retains Camera, Lens, Date, Software, Orientation, and EXIF, but strips all GPS tags.
 * - [MetadataOption.REMOVE_ALL]: Strips all metadata tags from the output.
 *
 * CRITICAL SAFETY RULE:
 * The original source file / URI is NEVER modified under any circumstance.
 * All source operations are strictly read-only.
 */
@Singleton
class MetadataEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val GPS_TAGS = setOf(
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_SPEED,
            ExifInterface.TAG_GPS_SPEED_REF,
            ExifInterface.TAG_GPS_TRACK,
            ExifInterface.TAG_GPS_TRACK_REF,
            ExifInterface.TAG_GPS_IMG_DIRECTION,
            ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
            ExifInterface.TAG_GPS_MAP_DATUM,
            ExifInterface.TAG_GPS_DEST_LATITUDE,
            ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
            ExifInterface.TAG_GPS_DEST_LONGITUDE,
            ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
            ExifInterface.TAG_GPS_STATUS,
            ExifInterface.TAG_GPS_MEASURE_MODE,
            ExifInterface.TAG_GPS_DOP,
            ExifInterface.TAG_GPS_DIFFERENTIAL
        )

        val CAMERA_TAGS = setOf(
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL
        )

        val LENS_TAGS = setOf(
            ExifInterface.TAG_LENS_MAKE,
            ExifInterface.TAG_LENS_MODEL,
            ExifInterface.TAG_LENS_SPECIFICATION,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
            ExifInterface.TAG_ISO_SPEED_RATINGS,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY
        )

        val DATE_TAGS = setOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_SUBSEC_TIME,
            ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
            ExifInterface.TAG_OFFSET_TIME,
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL
        )

        val SOFTWARE_TAGS = setOf(
            ExifInterface.TAG_SOFTWARE
        )

        val ORIENTATION_TAGS = setOf(
            ExifInterface.TAG_ORIENTATION
        )

        val EXIF_GENERAL_TAGS = setOf(
            ExifInterface.TAG_COLOR_SPACE,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_EXPOSURE_PROGRAM,
            ExifInterface.TAG_EXPOSURE_MODE,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_LIGHT_SOURCE,
            ExifInterface.TAG_METERING_MODE,
            ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
            ExifInterface.TAG_IMAGE_DESCRIPTION,
            ExifInterface.TAG_USER_COMMENT,
            ExifInterface.TAG_ARTIST,
            ExifInterface.TAG_COPYRIGHT
        )

        val ALL_SUPPORTED_TAGS: Set<String> =
            GPS_TAGS + CAMERA_TAGS + LENS_TAGS + DATE_TAGS + SOFTWARE_TAGS + ORIENTATION_TAGS + EXIF_GENERAL_TAGS
    }

    /**
     * Reads all supported EXIF metadata from the source URI into a structured [ImageMetadata] object.
     * Note: Access is strictly read-only and never modifies the source.
     */
    fun extractMetadata(sourceUri: Uri): ImageMetadata {
        val raw = readRawAttributes(sourceUri)
        if (raw.isEmpty()) return ImageMetadata()

        val camera = CameraMetadata(
            make = raw[ExifInterface.TAG_MAKE],
            model = raw[ExifInterface.TAG_MODEL]
        )

        val lens = LensMetadata(
            lensMake = raw[ExifInterface.TAG_LENS_MAKE],
            lensModel = raw[ExifInterface.TAG_LENS_MODEL],
            focalLength = raw[ExifInterface.TAG_FOCAL_LENGTH],
            fNumber = raw[ExifInterface.TAG_F_NUMBER],
            iso = raw[ExifInterface.TAG_ISO_SPEED_RATINGS] ?: raw[ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY],
            focalLengthIn35mm = raw[ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM]
        )

        val date = DateMetadata(
            dateTime = raw[ExifInterface.TAG_DATETIME],
            dateTimeOriginal = raw[ExifInterface.TAG_DATETIME_ORIGINAL],
            dateTimeDigitized = raw[ExifInterface.TAG_DATETIME_DIGITIZED]
        )

        val latLong = extractGpsCoordinates(sourceUri, raw)
        val gps = if (latLong != null || raw.keys.any { it in GPS_TAGS }) {
            GpsMetadata(
                latitude = latLong?.first,
                longitude = latLong?.second,
                altitude = extractAltitude(sourceUri, raw),
                processingMethod = raw[ExifInterface.TAG_GPS_PROCESSING_METHOD],
                dateStamp = raw[ExifInterface.TAG_GPS_DATESTAMP],
                timeStamp = raw[ExifInterface.TAG_GPS_TIMESTAMP]
            )
        } else null

        val orientationTag = raw[ExifInterface.TAG_ORIENTATION]?.toIntOrNull() ?: 1
        val degrees = when (orientationTag) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        val exifDetails = ExifDetails(
            flash = raw[ExifInterface.TAG_FLASH],
            whiteBalance = raw[ExifInterface.TAG_WHITE_BALANCE],
            exposureTime = raw[ExifInterface.TAG_EXPOSURE_TIME],
            exposureProgram = raw[ExifInterface.TAG_EXPOSURE_PROGRAM],
            colorSpace = raw[ExifInterface.TAG_COLOR_SPACE],
            imageDescription = raw[ExifInterface.TAG_IMAGE_DESCRIPTION],
            artist = raw[ExifInterface.TAG_ARTIST],
            copyright = raw[ExifInterface.TAG_COPYRIGHT]
        )

        return ImageMetadata(
            camera = camera,
            lens = lens,
            date = date,
            gps = gps,
            software = raw[ExifInterface.TAG_SOFTWARE],
            orientation = OrientationMetadata(orientationTag, degrees),
            exif = exifDetails,
            rawAttributes = raw
        )
    }

    /**
     * Reads all non-null supported EXIF attributes from the source URI using a read-only stream.
     * The original source is never mutated.
     */
    fun readRawAttributes(sourceUri: Uri): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri) ?: return result
            inputStream.use { stream ->
                val exif = ExifInterface(stream)
                for (tag in ALL_SUPPORTED_TAGS) {
                    exif.getAttribute(tag)?.let { value ->
                        result[tag] = value
                    }
                }
            }
        } catch (e: Exception) {
            // Source reading error handled gracefully
        }
        return result
    }

    /**
     * Filter metadata tags according to the specified [MetadataOption].
     */
    fun filterAttributes(
        attributes: Map<String, String>,
        option: MetadataOption,
        normalizeOrientation: Boolean = false
    ): Map<String, String> {
        val filtered = when (option) {
            MetadataOption.KEEP_METADATA -> attributes.toMutableMap()
            MetadataOption.REMOVE_GPS -> attributes.filterKeys { it !in GPS_TAGS }.toMutableMap()
            MetadataOption.REMOVE_ALL -> return emptyMap()
        }

        // 64. IMAGE ROTATION:
        // Pixels are physically normalized to upright orientation during decode.
        // When applying to output files, any preserved EXIF orientation tag must be
        // set to ORIENTATION_NORMAL (1) so viewers display the image correctly
        // and do not apply a secondary rotation on already-normalized pixels.
        if (normalizeOrientation && filtered.containsKey(ExifInterface.TAG_ORIENTATION)) {
            filtered[ExifInterface.TAG_ORIENTATION] = ExifInterface.ORIENTATION_NORMAL.toString()
        }

        return filtered
    }

    /**
     * Copies and applies filtered metadata from [sourceUri] to [destinationFile].
     *
     * IMPORTANT: [sourceUri] is opened strictly for reading; it is NEVER modified.
     * Only [destinationFile] receives attributes.
     */
    fun applyMetadataToFile(
        sourceUri: Uri,
        destinationFile: File,
        option: MetadataOption
    ) {
        if (option == MetadataOption.REMOVE_ALL) {
            // Destination file retains no metadata
            return
        }

        val rawAttributes = readRawAttributes(sourceUri)
        val filtered = filterAttributes(rawAttributes, option, normalizeOrientation = true)

        if (filtered.isNotEmpty()) {
            writeAttributesToFile(destinationFile, filtered)
        }
    }

    /**
     * Copies and applies filtered metadata from [sourceUri] to [destinationUri].
     *
     * IMPORTANT: [sourceUri] is opened strictly for reading; it is NEVER modified.
     */
    fun applyMetadata(
        sourceUri: Uri,
        destinationUri: Uri,
        option: MetadataOption
    ) {
        if (option == MetadataOption.REMOVE_ALL) {
            return
        }

        val rawAttributes = readRawAttributes(sourceUri)
        val filtered = filterAttributes(rawAttributes, option, normalizeOrientation = true)

        if (filtered.isNotEmpty()) {
            try {
                if (destinationUri.scheme == "file") {
                    destinationUri.path?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            writeAttributesToFile(file, filtered)
                            return
                        }
                    }
                }

                // Android Q+ MediaStore URI or Content URI
                context.contentResolver.openFileDescriptor(destinationUri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    for ((tag, value) in filtered) {
                        exif.setAttribute(tag, value)
                    }
                    exif.saveAttributes()
                }
            } catch (e: Exception) {
                // Writing metadata failure should not corrupt or crash image output
            }
        }
    }

    /**
     * Writes attribute map directly to a destination File.
     */
    fun writeAttributesToFile(file: File, attributes: Map<String, String>) {
        try {
            val exif = ExifInterface(file)
            for ((tag, value) in attributes) {
                exif.setAttribute(tag, value)
            }
            exif.saveAttributes()
        } catch (e: Exception) {
            // Handled gracefully
        }
    }

    private fun extractGpsCoordinates(sourceUri: Uri, raw: Map<String, String>): Pair<Double, Double>? {
        val fromExif = try {
            val stream = context.contentResolver.openInputStream(sourceUri)
            stream?.use {
                val exif = ExifInterface(it)
                val latLong = FloatArray(2)
                @Suppress("DEPRECATION")
                if (exif.getLatLong(latLong)) {
                    Pair(latLong[0].toDouble(), latLong[1].toDouble())
                } else null
            }
        } catch (e: Throwable) {
            null
        }

        if (fromExif != null) return fromExif

        val latStr = raw[ExifInterface.TAG_GPS_LATITUDE]
        val latRef = raw[ExifInterface.TAG_GPS_LATITUDE_REF]
        val lonStr = raw[ExifInterface.TAG_GPS_LONGITUDE]
        val lonRef = raw[ExifInterface.TAG_GPS_LONGITUDE_REF]
        return if (latStr != null && lonStr != null) {
            val lat = parseDms(latStr, latRef)
            val lon = parseDms(lonStr, lonRef)
            if (lat != null && lon != null) Pair(lat, lon) else null
        } else null
    }

    private fun extractAltitude(sourceUri: Uri, raw: Map<String, String>): Double? {
        val fromExif = try {
            val stream = context.contentResolver.openInputStream(sourceUri)
            stream?.use {
                val exif = ExifInterface(it)
                val alt = exif.getAltitude(Double.NaN)
                if (!alt.isNaN()) alt else null
            }
        } catch (e: Throwable) {
            null
        }

        if (fromExif != null) return fromExif

        return raw[ExifInterface.TAG_GPS_ALTITUDE]?.let { altStr ->
            parseRational(altStr)?.let { rational ->
                val isBelowSeaLevel = raw[ExifInterface.TAG_GPS_ALTITUDE_REF] == "1"
                if (isBelowSeaLevel) -rational else rational
            }
        }
    }

    private fun parseDms(dmsStr: String, ref: String?): Double? {
        try {
            val parts = dmsStr.split(",").map { it.trim() }
            if (parts.size != 3) return null
            val deg = parseRational(parts[0]) ?: return null
            val min = parseRational(parts[1]) ?: return null
            val sec = parseRational(parts[2]) ?: return null
            var result = deg + (min / 60.0) + (sec / 3600.0)
            if (ref.equals("S", ignoreCase = true) || ref.equals("W", ignoreCase = true)) {
                result = -result
            }
            return result
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseRational(str: String): Double? {
        return try {
            if (str.contains("/")) {
                val parts = str.split("/")
                val num = parts[0].trim().toDouble()
                val den = parts[1].trim().toDouble()
                if (den != 0.0) num / den else null
            } else {
                str.toDoubleOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }
}

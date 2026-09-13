package com.scanflow.photocompressor.domain.model

/**
 * Structured container for image metadata attributes.
 */
data class ImageMetadata(
    val camera: CameraMetadata = CameraMetadata(),
    val lens: LensMetadata = LensMetadata(),
    val date: DateMetadata = DateMetadata(),
    val gps: GpsMetadata? = null,
    val software: String? = null,
    val orientation: OrientationMetadata = OrientationMetadata(),
    val exif: ExifDetails = ExifDetails(),
    val rawAttributes: Map<String, String> = emptyMap()
) {
    val hasExif: Boolean
        get() = rawAttributes.isNotEmpty()

    val hasGps: Boolean
        get() = gps != null && (gps.latitude != null || gps.longitude != null)
}

/**
 * Camera device hardware attributes.
 */
data class CameraMetadata(
    val make: String? = null,
    val model: String? = null
) {
    val displayName: String?
        get() = when {
            make != null && model != null -> "$make $model"
            make != null -> make
            model != null -> model
            else -> null
        }
}

/**
 * Lens and shooting exposure parameters.
 */
data class LensMetadata(
    val lensMake: String? = null,
    val lensModel: String? = null,
    val focalLength: String? = null,
    val fNumber: String? = null,
    val iso: String? = null,
    val focalLengthIn35mm: String? = null
) {
    val summary: String?
        get() {
            val parts = mutableListOf<String>()
            lensModel?.let { parts.add(it) }
            focalLength?.let { parts.add("${it}mm") }
            fNumber?.let { parts.add("f/$it") }
            iso?.let { parts.add("ISO $it") }
            return if (parts.isNotEmpty()) parts.joinToString(" • ") else null
        }
}

/**
 * Capture and modification timestamps.
 */
data class DateMetadata(
    val dateTime: String? = null,
    val dateTimeOriginal: String? = null,
    val dateTimeDigitized: String? = null
) {
    val primaryDate: String?
        get() = dateTimeOriginal ?: dateTime ?: dateTimeDigitized
}

/**
 * Geographic positioning attributes.
 */
data class GpsMetadata(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val processingMethod: String? = null,
    val dateStamp: String? = null,
    val timeStamp: String? = null
) {
    val coordinatesString: String?
        get() = if (latitude != null && longitude != null) {
            String.format("%.5f, %.5f", latitude, longitude)
        } else null
}

/**
 * Image orientation attributes.
 */
data class OrientationMetadata(
    val tagValue: Int = 1,
    val rotationDegrees: Int = 0
)

/**
 * General shooting and artistic EXIF details.
 */
data class ExifDetails(
    val flash: String? = null,
    val whiteBalance: String? = null,
    val exposureTime: String? = null,
    val exposureProgram: String? = null,
    val colorSpace: String? = null,
    val imageDescription: String? = null,
    val artist: String? = null,
    val copyright: String? = null
)

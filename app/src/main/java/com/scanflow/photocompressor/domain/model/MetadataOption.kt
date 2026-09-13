package com.scanflow.photocompressor.domain.model

/**
 * Policy options for handling image metadata during processing.
 */
enum class MetadataOption(val label: String, val description: String) {
    /**
     * Preserves all metadata: Camera, Lens, Date, Software, Orientation, EXIF, and GPS.
     */
    KEEP_METADATA(
        label = "Keep Metadata",
        description = "Preserve camera, lens, date, GPS, and EXIF settings"
    ),

    /**
     * Preserves camera, lens, date, software, orientation, and EXIF settings,
     * but removes all GPS location coordinates for user privacy.
     */
    REMOVE_GPS(
        label = "Remove GPS",
        description = "Strip location data for privacy while keeping camera & shot details"
    ),

    /**
     * Strips all metadata from the output image for maximum privacy and minimal file size.
     */
    REMOVE_ALL(
        label = "Remove All Metadata",
        description = "Strip all metadata for maximum privacy and smaller file size"
    )
}

package com.scanflow.photocompressor.domain.model

/**
 * Supported image output formats.
 */
enum class ImageFormat(
    val extension: String,
    val mimeType: String,
    val supportsAlpha: Boolean = true,
    val bestFor: String = ""
) {
    JPEG(
        extension = "jpg",
        mimeType = "image/jpeg",
        supportsAlpha = false,
        bestFor = "Photography • Small output • No transparency"
    ),
    PNG(
        extension = "png",
        mimeType = "image/png",
        supportsAlpha = true,
        bestFor = "Transparency • Graphics • Lossless use cases"
    ),
    WEBP(
        extension = "webp",
        mimeType = "image/webp",
        supportsAlpha = true,
        bestFor = "Modern compressed format • Suitable for web/general image delivery"
    ),
    WEBP_LOSSLESS(
        extension = "webp",
        mimeType = "image/webp",
        supportsAlpha = true,
        bestFor = "Lossless modern compression with alpha support"
    );

    companion object {
        fun fromMimeType(mimeType: String): ImageFormat {
            return when {
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> JPEG
                mimeType.contains("png") -> PNG
                mimeType.contains("webp") -> WEBP
                else -> JPEG
            }
        }

        fun fromExtension(extension: String): ImageFormat {
            return when (extension.lowercase()) {
                "jpg", "jpeg" -> JPEG
                "png" -> PNG
                "webp" -> WEBP
                else -> JPEG
            }
        }
    }
}

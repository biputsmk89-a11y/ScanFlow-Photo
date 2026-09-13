package com.scanflow.photocompressor.engine

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.scanflow.photocompressor.domain.model.ImageAnalysis
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-performance, memory-safe image analyzer.
 *
 * CRITICAL RULE:
 * This analyzer NEVER decodes the full image pixels into memory.
 * It strictly reads file headers via [BitmapFactory.Options.inJustDecodeBounds]
 * and stream-based [ExifInterface] inspection.
 */
@Singleton
class ImageAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun analyze(uri: Uri): Result<ImageAnalysis> = withContext(Dispatchers.IO) {
        try {
            // 1. MIME detection
            val detectedMime = context.contentResolver.getType(uri) ?: "image/jpeg"

            // 2. Bounds read (header-only, zero pixel memory allocation)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")

            val width = options.outWidth
            val height = options.outHeight
            val finalMime = options.outMimeType ?: detectedMime

            if (width <= 0 || height <= 0) {
                throw IllegalStateException("Failed to read image dimensions: invalid bounds")
            }

            // 3. File size query
            var fileSizeBytes = 0L
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            fileSizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore cursor query failure
            }

            if (fileSizeBytes <= 0L) {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        fileSizeBytes = pfd.statSize
                    }
                } catch (e: Exception) {
                    // Fallback
                }
            }

            // 4. Orientation & EXIF availability
            var orientation = 0
            var exifAvailable = false

            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientationTag = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )

                    orientation = when (orientationTag) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }

                    exifAvailable = exif.hasAttribute(ExifInterface.TAG_DATETIME) ||
                            exif.hasAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ||
                            exif.hasAttribute(ExifInterface.TAG_MAKE) ||
                            exif.hasAttribute(ExifInterface.TAG_MODEL) ||
                            exif.hasAttribute(ExifInterface.TAG_FOCAL_LENGTH) ||
                            orientationTag != ExifInterface.ORIENTATION_UNDEFINED
                }
            } catch (e: Exception) {
                // EXIF may not exist for some formats (e.g. basic PNG/GIF)
            }

            // 5. Alpha transparency detection
            val hasAlpha = finalMime.contains("png", ignoreCase = true) ||
                    finalMime.contains("webp", ignoreCase = true) ||
                    finalMime.contains("gif", ignoreCase = true)

            // 6. Estimated memory allocation for decoded ARGB_8888 bitmap (4 bytes per pixel)
            val effectiveWidth = if (orientation == 90 || orientation == 270) height else width
            val effectiveHeight = if (orientation == 90 || orientation == 270) width else height
            val estimatedMemoryBytes = effectiveWidth.toLong() * effectiveHeight.toLong() * 4L

            Result.success(
                ImageAnalysis(
                    width = effectiveWidth,
                    height = effectiveHeight,
                    mimeType = finalMime,
                    fileSizeBytes = fileSizeBytes,
                    orientation = orientation,
                    hasAlpha = hasAlpha,
                    estimatedMemoryBytes = estimatedMemoryBytes,
                    exifAvailable = exifAvailable
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

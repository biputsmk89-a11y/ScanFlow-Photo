package com.scanflow.photocompressor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for memory-efficient bitmap loading and manipulation.
 * Handles inSampleSize calculations to prevent OOM errors on large images.
 */
@Singleton
class BitmapUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Max bitmap dimension to load into memory at full resolution
        const val MAX_BITMAP_DIMENSION = 4096

        // Max memory to use for a single bitmap (in bytes) - 64MB default ceiling
        const val MAX_BITMAP_MEMORY = 64L * 1024 * 1024
    }

    /**
     * Determine dynamic memory budget for a single bitmap allocation.
     * Never uses more than 25% of the available JVM max heap.
     */
    fun getSafeMemoryBudget(): Long {
        val maxHeap = Runtime.getRuntime().maxMemory()
        return if (maxHeap > 0) minOf(MAX_BITMAP_MEMORY, maxHeap / 4) else MAX_BITMAP_MEMORY
    }

    /**
     * Calculate optimal inSampleSize for memory-efficient bitmap loading.
     * The result is always a power of 2.
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        return calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight)
    }

    /**
     * Overloaded calculateInSampleSize taking raw dimensions directly.
     * Safe for large images (e.g. 12000x9000).
     */
    fun calculateInSampleSize(
        rawWidth: Int,
        rawHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        if (rawWidth <= 0 || rawHeight <= 0) return 1
        val targetWidth = if (reqWidth > 0) reqWidth else MAX_BITMAP_DIMENSION
        val targetHeight = if (reqHeight > 0) reqHeight else MAX_BITMAP_DIMENSION

        var inSampleSize = 1

        if (rawHeight > targetHeight || rawWidth > targetWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2

            while ((halfHeight / inSampleSize) >= targetHeight &&
                (halfWidth / inSampleSize) >= targetWidth
            ) {
                inSampleSize *= 2
            }
        }

        // Safety guard: if decoded memory with current inSampleSize still exceeds safe memory budget,
        // continue increasing inSampleSize by powers of 2 until it fits safely within memory limit.
        val maxMemoryBudget = getSafeMemoryBudget()
        var sampledWidth = rawWidth / inSampleSize
        var sampledHeight = rawHeight / inSampleSize
        while (estimateBitmapMemory(sampledWidth, sampledHeight) > maxMemoryBudget) {
            inSampleSize *= 2
            sampledWidth = rawWidth / inSampleSize
            sampledHeight = rawHeight / inSampleSize
        }

        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Get image dimensions without loading the full bitmap.
     * Accounts for EXIF orientation (swapping width and height if rotated 90° or 270°)
     * so that reported dimensions always match the upright visual dimensions.
     */
    fun getImageDimensions(uri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        val degrees = getExifOrientationDegrees(uri)
        return if (degrees == 90 || degrees == 270) {
            Pair(options.outHeight, options.outWidth)
        } else {
            Pair(options.outWidth, options.outHeight)
        }
    }

    /**
     * Extract EXIF orientation rotation in degrees (0, 90, 180, 270).
     */
    fun getExifOrientationDegrees(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * P0 LARGE IMAGE MEMORY SAFETY DECODER:
     * 1. bounds -> inJustDecodeBounds = true (no pixel allocation)
     * 2. required dimensions -> evaluate target constraints
     * 3. calculate sample size -> power of 2 downsampling within memory budget
     * 4. safe decode -> decodeStream with OOM catch, GC, sample-doubling & RGB_565 fallback
     */
    fun decodeBitmap(uri: Uri, maxWidth: Int = 0, maxHeight: Int = 0): Bitmap {
        val fileSizeBytes = getFileSize(uri)

        // 1. BOUNDS: Read image dimensions without allocating bitmap memory
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalStateException("Could not read image bounds from URI: $uri")

        val rawWidth = options.outWidth
        val rawHeight = options.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) {
            throw IllegalStateException("Invalid image bounds (${rawWidth}x${rawHeight}) from URI: $uri")
        }

        // Apply Large Image Strategy based on input size tier:
        // - Normal (1–5 MB): Fast path
        // - Large (10–30 MB): Stable path
        // - Very Large (50–100 MB): Safe fallback / graceful rejection
        val decodePlan = LargeImageStrategy.createDecodePlan(
            fileSizeBytes = fileSizeBytes,
            rawWidth = rawWidth,
            rawHeight = rawHeight,
            reqWidth = maxWidth,
            reqHeight = maxHeight
        )

        // Graceful rejection if device memory is insufficient
        if (decodePlan.shouldReject) {
            throw LargeImageRejectedException(
                fileSizeMB = fileSizeBytes.toDouble() / LargeImageStrategy.ONE_MB,
                rawWidth = rawWidth,
                rawHeight = rawHeight,
                message = decodePlan.rejectionReason ?: "Image is too large for current device memory to safely process"
            )
        }

        val effectiveMaxWidth = minOf(if (maxWidth > 0) maxWidth else decodePlan.maxDimension, decodePlan.maxDimension)
        val effectiveMaxHeight = minOf(if (maxHeight > 0) maxHeight else decodePlan.maxDimension, decodePlan.maxDimension)

        // 2. REQUIRED DIMENSIONS & 3. CALCULATE SAMPLE SIZE
        val baseSampleSize = calculateInSampleSize(rawWidth, rawHeight, effectiveMaxWidth, effectiveMaxHeight)
        options.inSampleSize = maxOf(decodePlan.inSampleSize, baseSampleSize)

        // 4. SAFE DECODE with OOM guard & retry
        options.inJustDecodeBounds = false
        options.inPreferredConfig = decodePlan.preferredConfig

        var attempts = 0
        var decodedBitmap: Bitmap? = null
        while (attempts < 3 && decodedBitmap == null) {
            try {
                decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
            } catch (oom: OutOfMemoryError) {
                attempts++
                System.gc()
                options.inSampleSize *= 2
                options.inPreferredConfig = Bitmap.Config.RGB_565
                if (attempts >= 3) {
                    throw OutOfMemoryError("Memory safety critical: Unable to safely decode image ($rawWidth x $rawHeight) even after reducing sample size: ${oom.message}")
                }
            }
        }

        val bitmap = decodedBitmap ?: throw IllegalStateException("Could not decode bitmap from URI: $uri")

        // Apply EXIF rotation if needed
        return applyExifRotation(uri, bitmap)
    }

    /**
     * Decode a thumbnail directly into a scaled Bitmap without decoding full-resolution into memory.
     * Uses inJustDecodeBounds -> power-of-2 downsample to maxDimension -> safe decode with RGB_565.
     */
    fun decodeThumbnail(uri: Uri, maxDimension: Int = 512): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalStateException("Could not read bounds for thumbnail: $uri")

        val rawWidth = options.outWidth
        val rawHeight = options.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) {
            throw IllegalStateException("Invalid image bounds (${rawWidth}x${rawHeight}) for thumbnail: $uri")
        }

        // Calculate power-of-2 sample size specifically targeted at maxDimension
        var sampleSize = 1
        val maxRaw = maxOf(rawWidth, rawHeight)
        while ((maxRaw / (sampleSize * 2)) >= maxDimension) {
            sampleSize *= 2
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize.coerceAtLeast(1)
        options.inPreferredConfig = Bitmap.Config.RGB_565 // Half memory footprint for thumbnails

        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalStateException("Could not decode thumbnail from: $uri")

        return applyExifRotation(uri, decoded)
    }

    /**
     * Estimate memory needed for a bitmap in ARGB_8888 format.
     */
    fun estimateBitmapMemory(width: Int, height: Int): Long {
        return width.toLong() * height.toLong() * 4L // 4 bytes per pixel for ARGB_8888
    }

    /**
     * Apply EXIF rotation to a bitmap so it displays correctly.
     */
    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.preScale(-1f, 1f)
                }
                else -> return bitmap
            }

            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) {
                bitmap.recycle()
            }
            return rotated
        } catch (oom: OutOfMemoryError) {
            System.gc()
            return bitmap
        } catch (e: Exception) {
            // If EXIF reading fails, return original bitmap
            return bitmap
        }
    }

    /**
     * Get MIME type from URI supporting content:// and file:// schemes without assuming filesystem path.
     */
    fun getMimeType(uri: Uri): String {
        return com.scanflow.photocompressor.util.UriHelper.getMimeType(context, uri)
    }

    /**
     * Get file name from URI supporting content:// and file:// schemes without assuming filesystem path.
     */
    fun getFileName(uri: Uri): String {
        return com.scanflow.photocompressor.util.UriHelper.getFileName(context, uri)
    }

    /**
     * Get file size from URI supporting content:// and file:// schemes without assuming filesystem path.
     */
    fun getFileSize(uri: Uri): Long {
        return com.scanflow.photocompressor.util.UriHelper.getFileSize(context, uri)
    }
}

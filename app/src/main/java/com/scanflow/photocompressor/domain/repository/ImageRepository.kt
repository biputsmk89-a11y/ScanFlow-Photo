package com.scanflow.photocompressor.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ImageInfo

/**
 * Repository interface for image loading and saving operations.
 */
interface ImageRepository {
    /**
     * Load image info (metadata only, no bitmap) from a URI.
     */
    suspend fun getImageInfo(uri: Uri): ImageInfo

    /**
     * Load a bitmap from a URI with optional max dimensions for memory efficiency.
     */
    suspend fun loadBitmap(uri: Uri, maxWidth: Int = 0, maxHeight: Int = 0): Bitmap

    /**
     * Save a bitmap to the output directory.
     * @return The URI of the saved file.
     */
    suspend fun saveBitmap(
        bitmap: Bitmap,
        fileName: String,
        format: ImageFormat,
        quality: Int
    ): Uri

    /**
     * Promote and save a validated temporary file to the final output destination.
     * Guaranteed never to overwrite the original source file.
     * @return The URI of the saved final file.
     */
    suspend fun saveFromFile(
        tempFile: java.io.File,
        fileName: String,
        format: ImageFormat
    ): Uri

    /**
     * Get the file size of a URI.
     */
    suspend fun getFileSize(uri: Uri): Long

    /**
     * Delete a file by URI.
     */
    suspend fun deleteFile(uri: Uri): Boolean

    /**
     * Get the output directory path.
     */
    fun getOutputDirectory(): String
}

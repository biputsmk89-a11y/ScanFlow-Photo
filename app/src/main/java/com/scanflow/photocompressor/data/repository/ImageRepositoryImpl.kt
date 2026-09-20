package com.scanflow.photocompressor.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.data.storage.FileNamingEngine
import com.scanflow.photocompressor.domain.model.ConflictStrategy
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ImageInfo
import com.scanflow.photocompressor.domain.model.ValidationResult
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.repository.PreferencesRepository
import com.scanflow.photocompressor.engine.BitmapUtils
import com.scanflow.photocompressor.engine.OutputValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ImageRepository adhering to strict storage rules:
 * Flow: Original -> Temp Output -> Validate -> Final Output
 *
 * Rules:
 * - Original file is NEVER modified or overwritten.
 * - Encoding is written to a temporary file first.
 * - Validation verifies file exists, > 0 bytes, valid MIME, decodes successfully, and dimensions valid.
 * - Only after validation passes is the file promoted to final output storage.
 */
@Singleton
class ImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bitmapUtils: BitmapUtils,
    private val fileManager: FileManager,
    private val outputValidator: OutputValidator,
    private val preferencesRepository: PreferencesRepository,
    private val fileNamingEngine: FileNamingEngine = FileNamingEngine()
) : ImageRepository {

    private val outputDir: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "PhotoCompressor")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    override suspend fun getImageInfo(uri: Uri): ImageInfo = withContext(Dispatchers.IO) {
        val fileName = bitmapUtils.getFileName(uri)
        val fileSize = bitmapUtils.getFileSize(uri)
        val mimeType = bitmapUtils.getMimeType(uri)
        val (width, height) = bitmapUtils.getImageDimensions(uri)
        val format = ImageFormat.fromMimeType(mimeType)
        val hasAlpha = format == ImageFormat.PNG || format == ImageFormat.WEBP ||
                mimeType.contains("png", ignoreCase = true) ||
                mimeType.contains("webp", ignoreCase = true) ||
                mimeType.contains("gif", ignoreCase = true)

        ImageInfo(
            uri = uri,
            fileName = fileName,
            fileSize = fileSize,
            width = width,
            height = height,
            format = format,
            mimeType = mimeType,
            hasAlpha = hasAlpha
        )
    }

    override suspend fun loadBitmap(uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap =
        withContext(Dispatchers.IO) {
            bitmapUtils.decodeBitmap(uri, maxWidth, maxHeight)
        }

    /**
     * Flow 58: Output File Flow
     * Input URI -> Temp file -> Encode -> Close -> Validate -> Move/Publish -> Final output URI
     * If failed: cleanup temp
     */
    override suspend fun saveBitmap(
        bitmap: Bitmap,
        fileName: String,
        format: ImageFormat,
        quality: Int
    ): Uri = withContext(Dispatchers.IO) {
        // Step 1: Write to Temp Output
        val tempFile = fileManager.createTempFile("compress_temp_", format.extension)
        try {
            // Step 2 & 3: Encode and Close
            FileOutputStream(tempFile).use { outputStream ->
                val compressFormat = when (format) {
                    ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                    ImageFormat.WEBP, ImageFormat.WEBP_LOSSLESS -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (format == ImageFormat.WEBP_LOSSLESS) Bitmap.CompressFormat.WEBP_LOSSLESS
                            else Bitmap.CompressFormat.WEBP_LOSSY
                        } else {
                            @Suppress("DEPRECATION")
                            Bitmap.CompressFormat.WEBP
                        }
                    }
                }
                bitmap.compress(compressFormat, quality, outputStream)
                outputStream.flush()
            }

            // Step 4: Validate (5-step verification)
            val validation = outputValidator.validate(tempFile, format)
            if (validation is ValidationResult.Failure) {
                throw IllegalStateException("Output validation failed: ${validation.reason} (Step: ${validation.step})")
            }

            // Step 5: Move/Publish to final output destination
            saveFromFile(tempFile, fileName, format)
        } finally {
            // Cleanup temp file on success or failure
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Promote and save a validated temporary file to the final output destination.
     * Guaranteed never to overwrite the original source file or an existing file without a collision strategy.
     */
    override suspend fun saveFromFile(
        tempFile: File,
        fileName: String,
        format: ImageFormat
    ): Uri = withContext(Dispatchers.IO) {
        val prefs = preferencesRepository.preferencesFlow.first()
        val strategy = prefs.behavior.conflictStrategy
        val sanitizedBase = fileNamingEngine.sanitizeBaseName(fileName)
        val ext = format.extension
        val cleanCandidateName = "$sanitizedBase.$ext"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveTempToMediaStore(tempFile, cleanCandidateName, format, strategy)
        } else {
            saveTempToFile(tempFile, cleanCandidateName, strategy)
        }
    }

    /**
     * Save temp file into MediaStore (Android 10+).
     * Enforces IS_PENDING state during publish and checks for duplicate filenames to prevent silent overwriting.
     */
    private fun saveTempToMediaStore(
        tempFile: File,
        fileName: String,
        format: ImageFormat,
        strategy: ConflictStrategy
    ): Uri {
        val finalFileName = fileNamingEngine.resolveConflict(fileName, strategy) { candidate ->
            isMediaStoreFileExists(candidate)
        }

        // If overwrite is selected and file exists in MediaStore, clean up previous row first
        if (strategy == ConflictStrategy.OVERWRITE && isMediaStoreFileExists(finalFileName)) {
            runCatching {
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(finalFileName, "${Environment.DIRECTORY_PICTURES}/PhotoCompressor%")
                context.contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs)
            }
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PhotoCompressor")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IllegalStateException("Failed to create MediaStore entry for $finalFileName")

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                outputStream.flush()
            } ?: throw IllegalStateException("Failed to open output stream for $finalFileName")

            // Release IS_PENDING to finalize publication
            val publishValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, publishValues, null, null)

            return uri
        } catch (e: Exception) {
            // Cleanup partial MediaStore record if write fails
            runCatching { context.contentResolver.delete(uri, null, null) }
            throw e
        }
    }

    /**
     * Save temp file to app-specific file storage without overwriting existing files (pre-Android 10).
     */
    private fun saveTempToFile(
        tempFile: File,
        fileName: String,
        strategy: ConflictStrategy
    ): Uri {
        val destFile = fileNamingEngine.resolveFileConflict(outputDir, fileName, strategy)
        tempFile.copyTo(destFile, overwrite = (strategy == ConflictStrategy.OVERWRITE))
        return Uri.fromFile(destFile)
    }

    private fun isMediaStoreFileExists(fileName: String): Boolean {
        return try {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf(fileName, "${Environment.DIRECTORY_PICTURES}/PhotoCompressor%")
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getFileSize(uri: Uri): Long = withContext(Dispatchers.IO) {
        bitmapUtils.getFileSize(uri)
    }

    override suspend fun deleteFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    override fun getOutputDirectory(): String = outputDir.absolutePath
}

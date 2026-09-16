package com.scanflow.photocompressor.data.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages output directories, temp files, and storage statistics.
 */
@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val outputDir: File by lazy {
        val dir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "PhotoCompressor")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val tempDir: File by lazy {
        val dir = File(context.cacheDir, "photo_compressor_temp")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    /**
     * Get the output directory for compressed images.
     */
    fun getOutputDirectory(): File = outputDir

    /**
     * Get the temp directory for intermediate files.
     */
    fun getTempDirectory(): File = tempDir

    /**
     * Create a temp file with the given prefix and extension.
     */
    fun createTempFile(prefix: String, extension: String): File {
        val safeExt = extension.trimStart('.')
        return File.createTempFile(prefix, ".$safeExt", tempDir)
    }

    /**
     * Executes a block with a managed temporary file, ensuring it is deleted on completion or failure.
     */
    inline fun <T> withTempFile(prefix: String, extension: String, block: (File) -> T): T {
        val tempFile = createTempFile(prefix, extension)
        return try {
            block(tempFile)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Clean up all temp files.
     */
    fun cleanTempFiles() {
        tempDir.listFiles()?.forEach { 
            runCatching { it.delete() }
        }
    }

    /**
     * Get the total size of all output files in bytes.
     */
    fun getOutputDirectorySize(): Long {
        return outputDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    /**
     * Get the number of files in the output directory.
     */
    fun getOutputFileCount(): Int {
        return outputDir.listFiles()?.count { it.isFile } ?: 0
    }

    /**
     * Get available storage space on the device.
     */
    fun getAvailableStorage(): Long {
        val stat = android.os.StatFs(outputDir.absolutePath)
        return stat.availableBytes
    }

    /**
     * Format bytes to human-readable string.
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Save a generated PDF file to public Documents/PhotoCompressor via MediaStore on Android 10+
     * or to app's external Documents directory on older versions.
     */
    fun savePdfToDocuments(tempFile: File, candidateName: String): android.net.Uri {
        val sanitized = if (candidateName.endsWith(".pdf", ignoreCase = true)) candidateName else "$candidateName.pdf"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, sanitized)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_DOCUMENTS}/PhotoCompressor")
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Files.getContentUri("external"),
                values
            ) ?: throw IllegalStateException("Failed to create MediaStore entry for PDF: $sanitized")

            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IllegalStateException("Could not open output stream for $uri")

                values.clear()
                values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                return uri
            } catch (e: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw e
            }
        } else {
            val docsDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), "PhotoCompressor")
            if (!docsDir.exists()) docsDir.mkdirs()
            val targetFile = File(docsDir, sanitized)
            tempFile.copyTo(targetFile, overwrite = true)
            return androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )
        }
    }
}

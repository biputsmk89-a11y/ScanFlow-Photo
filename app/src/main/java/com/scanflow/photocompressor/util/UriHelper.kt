package com.scanflow.photocompressor.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Robust URI Compatibility & Persistence Engine.
 *
 * Requirements:
 * - Full support for both `content://` and `file://` schemes.
 * - Proper use of [ContentResolver] without assuming any URI has a direct filesystem path.
 * - Rule 48 (URI Persistence): Take persisted URI permissions where supported.
 * - Background Processing Safety: Guarantees WorkManager workers can still read URIs across
 *   configuration changes, process death, and lifecycle transitions by staging transient external URIs.
 */
object UriHelper {

    private const val TAG = "UriHelper"
    private const val STAGED_DIR_NAME = "worker_staged_uris"

    /**
     * Checks if URI is a content scheme (`content://`).
     */
    fun isContentUri(uri: Uri): Boolean = uri.scheme == ContentResolver.SCHEME_CONTENT

    /**
     * Checks if URI is a direct file scheme (`file://`).
     */
    fun isFileUri(uri: Uri): Boolean = uri.scheme == ContentResolver.SCHEME_FILE

    /**
     * Extracts display file name from URI.
     * Uses ContentResolver for `content://` and never assumes a direct file path exists.
     */
    fun getFileName(context: Context, uri: Uri): String {
        if (isFileUri(uri)) {
            val path = uri.path
            if (!path.isNullOrBlank()) {
                val name = File(path).name
                if (name.isNotBlank()) return name
            }
            return uri.lastPathSegment ?: "unknown_file.jpg"
        }

        // For content:// URIs: Query OpenableColumns.DISPLAY_NAME via ContentResolver
        var displayName: String? = null
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ContentResolver query failed for DISPLAY_NAME on $uri: ${e.message}")
        }

        return displayName
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "image_${System.currentTimeMillis()}.jpg"
    }

    /**
     * Extracts file size in bytes from URI.
     * Tries ContentResolver query, file descriptor statSize, available stream bytes,
     * or direct file length without assuming a filesystem path.
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        if (isFileUri(uri)) {
            val path = uri.path
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists()) return file.length()
            }
        }

        var size = 0L

        // Attempt 1: ContentResolver query OpenableColumns.SIZE
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ContentResolver query failed for SIZE on $uri: ${e.message}")
        }

        // Attempt 2: ParcelFileDescriptor statSize
        if (size <= 0L) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val statSize = pfd.statSize
                    if (statSize > 0L) size = statSize
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Attempt 3: InputStream available bytes
        if (size <= 0L) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val available = stream.available().toLong()
                    if (available > 0L) size = available
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        return size.coerceAtLeast(0L)
    }

    /**
     * Resolves MIME type for the given URI.
     */
    fun getMimeType(context: Context, uri: Uri): String {
        if (isFileUri(uri)) {
            val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (!ext.isNullOrBlank()) {
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
                if (!mime.isNullOrBlank()) return mime
            }
        }

        return try {
            context.contentResolver.getType(uri) ?: "image/jpeg"
        } catch (e: Exception) {
            "image/jpeg"
        }
    }

    /**
     * Opens an [InputStream] for the given URI using ContentResolver.
     * Works seamlessly for both `content://` and `file://` schemes.
     */
    fun openInputStream(context: Context, uri: Uri): InputStream? {
        return try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to openInputStream for $uri: ${e.message}")
            null
        }
    }

    /**
     * Rule 48 (URI Persistence): Takes persisted URI permissions according to Android API.
     * Safely catches [SecurityException] when provider does not support persistable permissions
     * (e.g. transient tree/clipData URIs from ACTION_SEND).
     *
     * @return true if permission was persisted successfully; false otherwise.
     */
    fun takePersistablePermission(
        context: Context,
        uri: Uri,
        flags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
    ): Boolean {
        if (!isContentUri(uri)) return false

        return try {
            context.contentResolver.takePersistableUriPermission(uri, flags)
            Log.d(TAG, "Successfully took persistable URI permission for: $uri")
            true
        } catch (e: SecurityException) {
            Log.d(TAG, "URI does not support persistable permission: $uri (${e.message})")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to take persistable URI permission for $uri: ${e.message}")
            false
        }
    }

    /**
     * Prepares an input URI for long-running background processing (Workers).
     *
     * Guarantees:
     * 1. If URI permission is persistable, takes persisted permission and returns original URI.
     * 2. If URI is a local file or FileProvider, returns original URI.
     * 3. If URI is a transient external URI that cannot be persisted (e.g. from third-party ACTION_SEND),
     *    stages a temporary sandbox copy in `cacheDir/worker_staged_uris` so Worker can reliably read it
     *    even after configuration changes or process death.
     */
    fun prepareUriForBackgroundProcessing(context: Context, uri: Uri): Uri {
        if (isFileUri(uri)) return uri
        if (uri.authority == "${context.packageName}.fileprovider") return uri

        // Step 1: Attempt to take persistable URI permission
        val isPersisted = takePersistablePermission(context, uri)
        if (isPersisted) {
            return uri
        }

        // Step 2: Fallback for transient URIs - stage a private sandbox copy in app cache
        return try {
            val stagingDir = File(context.cacheDir, STAGED_DIR_NAME).apply { mkdirs() }
            val cleanName = uri.lastPathSegment?.filter { it.isLetterOrDigit() } ?: "item"
            val stagedFile = File(stagingDir, "stage_${System.currentTimeMillis()}_$cleanName.tmp")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(stagedFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (stagedFile.exists() && stagedFile.length() > 0) {
                Log.d(TAG, "Staged transient URI to sandbox cache: ${stagedFile.absolutePath}")
                Uri.fromFile(stagedFile)
            } else {
                uri
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not stage transient URI $uri: ${e.message}")
            uri
        }
    }

    /**
     * Cleans up transient staged worker files when jobs complete or cancel.
     */
    fun cleanupStagedUris(context: Context) {
        try {
            val stagingDir = File(context.cacheDir, STAGED_DIR_NAME)
            if (stagingDir.exists() && stagingDir.isDirectory) {
                stagingDir.listFiles()?.forEach { file ->
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete staged file ${file.name}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up staged URIs: ${e.message}")
        }
    }
}

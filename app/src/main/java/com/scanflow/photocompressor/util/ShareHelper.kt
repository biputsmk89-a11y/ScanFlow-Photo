package com.scanflow.photocompressor.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Utility for sharing compressed outputs using the Android Share Sheet.
 * Supports both single image (ACTION_SEND) and multiple images (ACTION_SEND_MULTIPLE).
 */
object ShareHelper {

    /**
     * Share a single image output via the Android Share Sheet.
     */
    fun shareImage(
        context: Context,
        uri: Uri,
        mimeType: String = "image/jpeg",
        title: String = "Share Photo"
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Share multiple image outputs via the Android Share Sheet (ACTION_SEND_MULTIPLE).
     */
    fun shareImages(
        context: Context,
        uris: List<Uri>,
        mimeType: String = "image/*",
        title: String = "Share Photos"
    ) {
        if (uris.isEmpty()) return
        if (uris.size == 1) {
            shareImage(context, uris.first(), mimeType, title)
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

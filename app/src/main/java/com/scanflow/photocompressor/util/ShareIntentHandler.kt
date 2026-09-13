package com.scanflow.photocompressor.util

import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Utility for parsing incoming share intents (ACTION_SEND and ACTION_SEND_MULTIPLE).
 */
object ShareIntentHandler {

    /**
     * Extracts image URIs from an incoming share [Intent].
     * Supports both single image (ACTION_SEND) and multiple images (ACTION_SEND_MULTIPLE).
     */
    fun extractUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()

        val uris = mutableListOf<Uri>()
        val action = intent.action
        val type = intent.type

        val isImageIntent = type?.startsWith("image/") == true ||
                type?.equals("*/*") == true ||
                intent.hasExtra(Intent.EXTRA_STREAM)

        if (!isImageIntent) return emptyList()

        when (action) {
            Intent.ACTION_SEND -> {
                // Single image share
                val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }

                if (streamUri != null) {
                    uris.add(streamUri)
                } else {
                    // Fallback to intent data
                    intent.data?.let { uris.add(it) }

                    // Fallback to ClipData
                    intent.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            clipData.getItemAt(i).uri?.let { uris.add(it) }
                        }
                    }
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                // Multiple images share
                val streamUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }

                if (!streamUris.isNullOrEmpty()) {
                    uris.addAll(streamUris.filterNotNull())
                } else {
                    // Fallback to ClipData
                    intent.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            clipData.getItemAt(i).uri?.let { uris.add(it) }
                        }
                    }
                }
            }
        }

        return uris.distinct()
    }
}

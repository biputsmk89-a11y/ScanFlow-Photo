package com.scanflow.photocompressor.domain.usecase

import android.content.Context
import android.net.Uri
import com.scanflow.photocompressor.util.ShareHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 56. USE CASE: ShareOutputUseCase
 * Launches the native Android Share Sheet to share single or multiple processed images.
 */
class ShareOutputUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shareHelper: ShareHelper
) {
    operator fun invoke(uri: Uri, mimeType: String = "image/*") {
        shareHelper.shareImage(context, uri, mimeType)
    }

    operator fun invoke(uris: List<Uri>, mimeType: String = "image/*") {
        shareHelper.shareImages(context, uris, mimeType)
    }
}

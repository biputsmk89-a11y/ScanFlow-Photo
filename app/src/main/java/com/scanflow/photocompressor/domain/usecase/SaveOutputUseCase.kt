package com.scanflow.photocompressor.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.repository.ImageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 56. USE CASE: SaveOutputUseCase
 * Persists processed images/files safely into application storage or public media gallery.
 */
class SaveOutputUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageRepository: ImageRepository
) {
    suspend fun saveFromFile(
        tempFile: File,
        fileName: String,
        format: ImageFormat
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val uri = imageRepository.saveFromFile(tempFile, fileName, format)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBitmap(
        bitmap: Bitmap,
        fileName: String,
        format: ImageFormat,
        quality: Int = 90
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val uri = imageRepository.saveBitmap(bitmap, fileName, format, quality)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

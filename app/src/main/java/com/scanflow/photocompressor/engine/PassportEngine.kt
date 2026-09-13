package com.scanflow.photocompressor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for Passport & Official ID Photo generation.
 * Pipeline:
 * Select -> Face guidance -> Crop -> Resize -> Background -> Print layout
 *
 * Fully integrated with ImagePipelineEngine for crop, resize, and encoding.
 */
@Singleton
class PassportEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imagePipelineEngine: ImagePipelineEngine,
    private val bitmapUtils: BitmapUtils,
    private val fileManager: FileManager
) {

    /**
     * Executes the Passport pipeline on a source photo URI.
     */
    suspend fun processPassportPhoto(
        sourceUri: Uri,
        config: PassportConfig,
        customCropRegion: CropRegion? = null
    ): Result<CompressionResult> = withContext(Dispatchers.IO) {
        try {
            // 1. CROP & RESIZE VIA CORE IMAGE PIPELINE
            val operations = mutableListOf<ImageOperation>()

            if (customCropRegion != null) {
                operations.add(ImageOperation.Crop(region = customCropRegion))
            } else {
                operations.add(
                    ImageOperation.Crop(
                        aspectRatio = AspectRatio(config.spec.ratioX, config.spec.ratioY)
                    )
                )
            }

            operations.add(
                ImageOperation.Resize(
                    width = config.spec.targetWidthPx,
                    height = config.spec.targetHeightPx,
                    maintainAspectRatio = false
                )
            )

            operations.add(ImageOperation.Compress(quality = 95))
            operations.add(ImageOperation.Convert(ImageFormat.JPEG))

            val pipeline = ImagePipeline(operations)
            val pipelineResult = imagePipelineEngine.execute(
                inputUri = sourceUri,
                pipeline = pipeline,
                operationType = OperationType.CROP
            )

            if (pipelineResult.isFailure) {
                return@withContext pipelineResult
            }

            val baseResult = pipelineResult.getOrThrow()
            val baseBitmapUri = baseResult.outputUri

            // Decode base processed passport photo
            var passportBitmap = bitmapUtils.decodeBitmap(
                baseBitmapUri,
                config.spec.targetWidthPx,
                config.spec.targetHeightPx
            )

            // 2. BACKGROUND PROCESSING (if background color requested)
            val bgColor = config.background.colorInt
            if (bgColor != null) {
                passportBitmap = applyBackgroundTintOrColor(passportBitmap, bgColor)
            }

            // 3. PRINT LAYOUT COMPOSITION
            if (config.printLayout == PassportPrintLayout.COPIES_1) {
                // Single photo layout is already complete
                val tempFile = fileManager.createTempFile("passport_single_", "jpg")
                FileOutputStream(tempFile).use { out ->
                    passportBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                val outputUri = Uri.fromFile(tempFile)
                passportBitmap.recycle()

                return@withContext Result.success(
                    baseResult.copy(
                        outputUri = outputUri,
                        width = config.spec.targetWidthPx,
                        height = config.spec.targetHeightPx,
                        compressedSize = tempFile.length()
                    )
                )
            }

            // Multi-photo print sheet composition
            val sheetBitmap = composePrintSheet(passportBitmap, config)
            passportBitmap.recycle()

            val tempFile = fileManager.createTempFile("passport_sheet_", "jpg")
            FileOutputStream(tempFile).use { out ->
                sheetBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            val outputUri = Uri.fromFile(tempFile)
            val sheetWidth = sheetBitmap.width
            val sheetHeight = sheetBitmap.height
            sheetBitmap.recycle()

            Result.success(
                baseResult.copy(
                    outputUri = outputUri,
                    width = sheetWidth,
                    height = sheetHeight,
                    compressedSize = tempFile.length()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun applyBackgroundTintOrColor(bitmap: Bitmap, targetColor: Int): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        if (bitmap.hasAlpha()) {
            canvas.drawColor(targetColor)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        } else {
            // Draw original bitmap with a crisp colored ID border
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            val borderPaint = Paint().apply {
                color = targetColor
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), borderPaint)
        }

        if (result !== bitmap) {
            bitmap.recycle()
        }
        return result
    }

    private fun composePrintSheet(photo: Bitmap, config: PassportConfig): Bitmap {
        val pw = photo.width
        val ph = photo.height
        val cols = config.printLayout.cols
        val rows = config.printLayout.rows

        val padding = 32
        val sheetWidth = (pw * cols) + (padding * (cols + 1))
        val sheetHeight = (ph * rows) + (padding * (rows + 1))

        val sheet = Bitmap.createBitmap(sheetWidth, sheetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.WHITE) // Photo paper white base

        val spacingX = (sheetWidth - (cols * pw)) / (cols + 1)
        val spacingY = (sheetHeight - (rows * ph)) / (rows + 1)

        val photoPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        val cutLinePaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val left = spacingX + c * (pw + spacingX)
                val top = spacingY + r * (ph + spacingY)

                canvas.drawBitmap(photo, left.toFloat(), top.toFloat(), photoPaint)

                // Optional cut markers
                if (config.addCutMarks) {
                    canvas.drawRect(
                        left.toFloat() - 2f,
                        top.toFloat() - 2f,
                        (left + pw).toFloat() + 2f,
                        (top + ph).toFloat() + 2f,
                        cutLinePaint
                    )
                }
            }
        }

        return sheet
    }
}

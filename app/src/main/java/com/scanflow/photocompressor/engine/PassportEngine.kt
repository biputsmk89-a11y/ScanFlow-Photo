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
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
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
    private val fileManager: FileManager,
    private val imageRepository: ImageRepository,
    private val historyRepository: HistoryRepository,
    private val portraitSegmentationEngine: PortraitSegmentationEngine
) {

    /**
     * Executes the Passport pipeline on a source photo URI.
     */
    suspend fun processPassportPhoto(
        sourceUri: Uri,
        config: PassportConfig,
        customCropRegion: CropRegion? = null,
        cutoutUri: Uri? = null
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

            val isCustomBg = config.effectiveBackgroundColor != null
            val inputForPipeline = if (isCustomBg && cutoutUri != null) cutoutUri else sourceUri
            val formatForPipeline = if (isCustomBg && cutoutUri != null) ImageFormat.PNG else ImageFormat.JPEG

            operations.add(ImageOperation.Compress(quality = 95))
            operations.add(ImageOperation.Convert(formatForPipeline))

            val pipeline = ImagePipeline(operations)
            val pipelineResult = imagePipelineEngine.execute(
                inputUri = inputForPipeline,
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

            // 1.5 APPLY USER POSITION TRANSFORMS (Zoom, Pan, Rotation) if modified
            if (config.zoom != 1.0f || config.panX != 0f || config.panY != 0f || config.rotationDegrees != 0f) {
                val transformed = Bitmap.createBitmap(
                    config.spec.targetWidthPx,
                    config.spec.targetHeightPx,
                    Bitmap.Config.ARGB_8888
                )
                val transformCanvas = Canvas(transformed)
                val matrix = android.graphics.Matrix().apply {
                    postTranslate(-passportBitmap.width / 2f, -passportBitmap.height / 2f)
                    postScale(config.zoom, config.zoom)
                    postRotate(config.rotationDegrees)
                    postTranslate(
                        (passportBitmap.width / 2f) + config.panX,
                        (passportBitmap.height / 2f) + config.panY
                    )
                }
                transformCanvas.drawBitmap(passportBitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
                passportBitmap.recycle()
                passportBitmap = transformed
            }

            // 2. BACKGROUND PROCESSING (if background color requested)
            val bgColor = config.effectiveBackgroundColor
            if (bgColor != null) {
                passportBitmap = applyBackgroundTintOrColor(passportBitmap, bgColor)
            }

            // 2.5 ID FRAME ENGINE (Thin, Classic, Professional)
            if (config.frameStyle != IdFrameStyle.NONE) {
                passportBitmap = applyIdFrame(passportBitmap, config.frameStyle)
            }

            // 3. PRINT LAYOUT COMPOSITION
            if (config.printLayout == PassportPrintLayout.COPIES_1) {
                // Single photo layout is already complete
                val tempFile = fileManager.createTempFile("passport_single_", "jpg")
                try {
                    FileOutputStream(tempFile).use { out ->
                        passportBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    val fileName = "passport_${config.spec.displayName.replace(" ", "_")}_${System.currentTimeMillis()}"
                    val finalSavedUri = imageRepository.saveFromFile(tempFile, fileName, ImageFormat.JPEG)
                    val savedSize = tempFile.length()

                    val result = baseResult.copy(
                        outputUri = finalSavedUri,
                        width = config.spec.targetWidthPx,
                        height = config.spec.targetHeightPx,
                        compressedSize = savedSize
                    )

                    runCatching {
                        historyRepository.addEntry(
                            ProcessingHistory(
                                inputUri = sourceUri.toString(),
                                outputUri = finalSavedUri.toString(),
                                inputFileName = "passport_source",
                                outputFileName = "$fileName.jpg",
                                originalBytes = baseResult.originalSize,
                                outputBytes = savedSize,
                                operationType = OperationType.PASSPORT,
                                width = config.spec.targetWidthPx,
                                height = config.spec.targetHeightPx,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }

                    return@withContext Result.success(result)
                } finally {
                    passportBitmap.recycle()
                    if (tempFile.exists()) tempFile.delete()
                }
            }

            // Multi-photo print sheet composition
            val sheetBitmap = composePrintSheet(passportBitmap, config)
            passportBitmap.recycle()

            val tempFile = fileManager.createTempFile("passport_sheet_", "jpg")
            try {
                FileOutputStream(tempFile).use { out ->
                    sheetBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                val sheetWidth = sheetBitmap.width
                val sheetHeight = sheetBitmap.height
                val fileName = "passport_sheet_${config.spec.displayName.replace(" ", "_")}_${System.currentTimeMillis()}"
                val finalSavedUri = imageRepository.saveFromFile(tempFile, fileName, ImageFormat.JPEG)
                val savedSize = tempFile.length()

                val result = baseResult.copy(
                    outputUri = finalSavedUri,
                    width = sheetWidth,
                    height = sheetHeight,
                    compressedSize = savedSize
                )

                runCatching {
                    historyRepository.addEntry(
                        ProcessingHistory(
                            inputUri = sourceUri.toString(),
                            outputUri = finalSavedUri.toString(),
                            inputFileName = "passport_source",
                            outputFileName = "$fileName.jpg",
                            originalBytes = baseResult.originalSize,
                            outputBytes = savedSize,
                            operationType = OperationType.PASSPORT,
                            width = sheetWidth,
                            height = sheetHeight,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                Result.success(result)
            } finally {
                sheetBitmap.recycle()
                if (tempFile.exists()) tempFile.delete()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun applyBackgroundTintOrColor(bitmap: Bitmap, targetColor: Int): Bitmap {
        if (bitmap.hasAlpha()) {
            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            canvas.drawColor(targetColor)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            bitmap.recycle()
            return result
        }
        val result = portraitSegmentationEngine.replaceBackground(bitmap, targetColor)
        bitmap.recycle()
        return result
    }

    private fun applyIdFrame(bitmap: Bitmap, frameStyle: IdFrameStyle): Bitmap {
        if (frameStyle == IdFrameStyle.NONE) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw base bitmap
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        when (frameStyle) {
            IdFrameStyle.NONE -> { /* No op */ }
            IdFrameStyle.THIN -> {
                val borderPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                canvas.drawRect(1.5f, 1.5f, width - 1.5f, height - 1.5f, borderPaint)
            }
            IdFrameStyle.CLASSIC -> {
                // Outer 6px border with 1px hairline
                val outerPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                    isAntiAlias = true
                }
                canvas.drawRect(3f, 3f, width - 3f, height - 3f, outerPaint)

                val hairlinePaint = Paint().apply {
                    color = Color.LTGRAY
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                    isAntiAlias = true
                }
                canvas.drawRect(6.5f, 6.5f, width - 6.5f, height - 6.5f, hairlinePaint)
            }
            IdFrameStyle.PROFESSIONAL -> {
                // Formal ID Card Border: 8px frame with crisp safe-area inner margin
                val whiteFramePaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 8f
                    isAntiAlias = true
                }
                canvas.drawRect(4f, 4f, width - 4f, height - 4f, whiteFramePaint)

                val innerGuidePaint = Paint().apply {
                    color = Color.rgb(200, 200, 200)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                    isAntiAlias = true
                }
                canvas.drawRect(9f, 9f, width - 9f, height - 9f, innerGuidePaint)
            }
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

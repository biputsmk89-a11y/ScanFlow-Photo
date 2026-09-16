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
    private val historyRepository: HistoryRepository
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

    private fun applyBackgroundTintOrColor(bitmap: Bitmap, targetColor: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        if (bitmap.hasAlpha()) {
            val canvas = Canvas(result)
            canvas.drawColor(targetColor)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        } else {
            // Smart Offline Background Replacement
            // Samples perimeter/corner backdrop color and replaces background pixels with targetColor
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // Sample corner patches (top-left, top-right)
            val sampleRadius = (minOf(width, height) / 25).coerceIn(4, 16)
            var sumR = 0L
            var sumG = 0L
            var sumB = 0L
            var count = 0

            // Sample top-left corner
            for (y in 0 until sampleRadius) {
                for (x in 0 until sampleRadius) {
                    val p = pixels[y * width + x]
                    sumR += Color.red(p)
                    sumG += Color.green(p)
                    sumB += Color.blue(p)
                    count++
                }
            }
            // Sample top-right corner
            for (y in 0 until sampleRadius) {
                for (x in (width - sampleRadius) until width) {
                    val p = pixels[y * width + x]
                    sumR += Color.red(p)
                    sumG += Color.green(p)
                    sumB += Color.blue(p)
                    count++
                }
            }

            val bgR = if (count > 0) (sumR / count).toInt() else 240
            val bgG = if (count > 0) (sumG / count).toInt() else 240
            val bgB = if (count > 0) (sumB / count).toInt() else 240

            val targetR = Color.red(targetColor)
            val targetG = Color.green(targetColor)
            val targetB = Color.blue(targetColor)

            // Color tolerance thresholds (Euclidean distance)
            val tolerance = 48.0
            val featherBand = 18.0

            // Background mask using top-down flood connectivity from top border
            val isBgMask = BooleanArray(width * height)
            val queue = java.util.ArrayDeque<Int>()

            // Seed queue from top row and outer top corners
            for (x in 0 until width) {
                val idx = x
                val p = pixels[idx]
                val dist = colorDistance(Color.red(p), Color.green(p), Color.blue(p), bgR, bgG, bgB)
                if (dist <= tolerance + featherBand) {
                    isBgMask[idx] = true
                    queue.add(idx)
                }
            }

            // Seed left and right borders in upper half
            val halfHeight = height / 2
            for (y in 0 until halfHeight) {
                val leftIdx = y * width
                if (!isBgMask[leftIdx]) {
                    val p = pixels[leftIdx]
                    if (colorDistance(Color.red(p), Color.green(p), Color.blue(p), bgR, bgG, bgB) <= tolerance + featherBand) {
                        isBgMask[leftIdx] = true
                        queue.add(leftIdx)
                    }
                }
                val rightIdx = y * width + (width - 1)
                if (!isBgMask[rightIdx]) {
                    val p = pixels[rightIdx]
                    if (colorDistance(Color.red(p), Color.green(p), Color.blue(p), bgR, bgG, bgB) <= tolerance + featherBand) {
                        isBgMask[rightIdx] = true
                        queue.add(rightIdx)
                    }
                }
            }

            // BFS flood fill to only replace connected background region (protects clothing/eyes)
            val dx = intArrayOf(1, -1, 0, 0)
            val dy = intArrayOf(0, 0, 1, -1)

            while (!queue.isEmpty()) {
                val curr = queue.removeFirst()
                val cx = curr % width
                val cy = curr / width

                for (i in 0 until 4) {
                    val nx = cx + dx[i]
                    val ny = cy + dy[i]
                    if (nx in 0 until width && ny in 0 until height) {
                        val nIdx = ny * width + nx
                        if (!isBgMask[nIdx]) {
                            val p = pixels[nIdx]
                            val dist = colorDistance(Color.red(p), Color.green(p), Color.blue(p), bgR, bgG, bgB)
                            if (dist <= tolerance + featherBand) {
                                isBgMask[nIdx] = true
                                queue.add(nIdx)
                            }
                        }
                    }
                }
            }

            // Apply background color replacement with feathering
            for (i in pixels.indices) {
                if (isBgMask[i]) {
                    val p = pixels[i]
                    val r = Color.red(p)
                    val g = Color.green(p)
                    val b = Color.blue(p)
                    val dist = colorDistance(r, g, b, bgR, bgG, bgB)

                    if (dist <= tolerance) {
                        // 100% background replacement
                        pixels[i] = Color.rgb(targetR, targetG, targetB)
                    } else {
                        // Feathered blending boundary
                        val factor = ((dist - tolerance) / featherBand).coerceIn(0.0, 1.0).toFloat()
                        val blendedR = (targetR * (1f - factor) + r * factor).toInt()
                        val blendedG = (targetG * (1f - factor) + g * factor).toInt()
                        val blendedB = (targetB * (1f - factor) + b * factor).toInt()
                        pixels[i] = Color.rgb(blendedR, blendedG, blendedB)
                    }
                }
            }

            result.setPixels(pixels, 0, width, 0, 0, width, height)

            // Draw crisp subtle 2px ID boundary border
            val canvas = Canvas(result)
            val borderPaint = Paint().apply {
                color = targetColor
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
        }

        if (result !== bitmap) {
            bitmap.recycle()
        }
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

    private fun colorDistance(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double {
        val dr = (r1 - r2).toDouble()
        val dg = (g1 - g2).toDouble()
        val db = (b1 - b2).toDouble()
        return kotlin.math.sqrt(dr * dr + dg * dg + db * db)
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

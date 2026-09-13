package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import com.scanflow.photocompressor.domain.model.CropRegion
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for cropping bitmaps to specified regions.
 */
@Singleton
class CropEngine @Inject constructor() {

    /**
     * Crop a bitmap to the specified region.
     *
     * @param bitmap Source bitmap.
     * @param region The crop region.
     * @param originalWidth Original width if bitmap was downsampled on decode.
     * @param originalHeight Original height if bitmap was downsampled on decode.
     * @return Cropped bitmap.
     */
    fun crop(
        bitmap: Bitmap,
        region: CropRegion,
        originalWidth: Int = 0,
        originalHeight: Int = 0
    ): Bitmap {
        // If image was downsampled from large original dimensions (e.g. 12000x9000),
        // map crop region coordinates proportionally to match the safely decoded bitmap.
        val effectiveRegion = if (originalWidth > 0 && originalHeight > 0 &&
            (originalWidth != bitmap.width || originalHeight != bitmap.height)
        ) {
            val scaleX = bitmap.width.toFloat() / originalWidth.toFloat()
            val scaleY = bitmap.height.toFloat() / originalHeight.toFloat()
            val mappedX = (region.x * scaleX).toInt().coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
            val mappedY = (region.y * scaleY).toInt().coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
            val mappedWidth = (region.width * scaleX).toInt().coerceAtLeast(1).coerceAtMost(bitmap.width - mappedX)
            val mappedHeight = (region.height * scaleY).toInt().coerceAtLeast(1).coerceAtMost(bitmap.height - mappedY)
            CropRegion(mappedX, mappedY, mappedWidth, mappedHeight)
        } else {
            val clampedX = region.x.coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
            val clampedY = region.y.coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
            val clampedWidth = region.width.coerceAtLeast(1).coerceAtMost(bitmap.width - clampedX)
            val clampedHeight = region.height.coerceAtLeast(1).coerceAtMost(bitmap.height - clampedY)
            CropRegion(clampedX, clampedY, clampedWidth, clampedHeight)
        }

        return Bitmap.createBitmap(
            bitmap,
            effectiveRegion.x,
            effectiveRegion.y,
            effectiveRegion.width,
            effectiveRegion.height
        )
    }

    /**
     * Map a crop region defined in raw (unoriented) storage coordinates into visual upright coordinates
     * based on the EXIF orientation degrees (0, 90, 180, 270).
     */
    fun mapRawCropRegionToOriented(
        region: CropRegion,
        rawWidth: Int,
        rawHeight: Int,
        exifOrientationDegrees: Int
    ): CropRegion {
        return when (exifOrientationDegrees) {
            90 -> {
                // 90 deg clockwise: (x, y) maps to (rawHeight - y - h, x)
                CropRegion(
                    x = (rawHeight - region.bottom).coerceAtLeast(0),
                    y = region.x.coerceAtLeast(0),
                    width = region.height.coerceAtLeast(1),
                    height = region.width.coerceAtLeast(1)
                )
            }
            180 -> {
                // 180 deg: (x, y) maps to (rawWidth - x - w, rawHeight - y - h)
                CropRegion(
                    x = (rawWidth - region.right).coerceAtLeast(0),
                    y = (rawHeight - region.bottom).coerceAtLeast(0),
                    width = region.width.coerceAtLeast(1),
                    height = region.height.coerceAtLeast(1)
                )
            }
            270 -> {
                // 270 deg clockwise (90 CCW): (x, y) maps to (y, rawWidth - x - w)
                CropRegion(
                    x = region.y.coerceAtLeast(0),
                    y = (rawWidth - region.right).coerceAtLeast(0),
                    width = region.height.coerceAtLeast(1),
                    height = region.width.coerceAtLeast(1)
                )
            }
            else -> region
        }
    }

    /**
     * Map a crop region defined in visual upright coordinates back into raw unoriented coordinates.
     */
    fun mapOrientedCropRegionToRaw(
        region: CropRegion,
        orientedWidth: Int,
        orientedHeight: Int,
        exifOrientationDegrees: Int
    ): CropRegion {
        return when (exifOrientationDegrees) {
            90 -> {
                CropRegion(
                    x = region.y.coerceAtLeast(0),
                    y = (orientedWidth - region.right).coerceAtLeast(0),
                    width = region.height.coerceAtLeast(1),
                    height = region.width.coerceAtLeast(1)
                )
            }
            180 -> {
                CropRegion(
                    x = (orientedWidth - region.right).coerceAtLeast(0),
                    y = (orientedHeight - region.bottom).coerceAtLeast(0),
                    width = region.width.coerceAtLeast(1),
                    height = region.height.coerceAtLeast(1)
                )
            }
            270 -> {
                CropRegion(
                    x = (orientedHeight - region.bottom).coerceAtLeast(0),
                    y = region.x.coerceAtLeast(0),
                    width = region.height.coerceAtLeast(1),
                    height = region.width.coerceAtLeast(1)
                )
            }
            else -> region
        }
    }

    /**
     * Calculate an exact crop region given aspect ratio, zoom scale, and pan offsets.
     */
    fun calculateAspectCropRegion(
        imageWidth: Int,
        imageHeight: Int,
        targetRatioX: Int,
        targetRatioY: Int,
        zoomScale: Float = 1f,
        panOffsetX: Float = 0f,
        panOffsetY: Float = 0f
    ): CropRegion {
        if (imageWidth <= 0 || imageHeight <= 0) return CropRegion(0, 0, 1, 1)

        val (baseWidth, baseHeight) = if (targetRatioX > 0 && targetRatioY > 0) {
            val targetRatio = targetRatioX.toFloat() / targetRatioY.toFloat()
            val srcRatio = imageWidth.toFloat() / imageHeight.toFloat()
            if (srcRatio > targetRatio) {
                val w = (imageHeight * targetRatio).toInt()
                Pair(w, imageHeight)
            } else {
                val h = (imageWidth / targetRatio).toInt()
                Pair(imageWidth, h)
            }
        } else {
            Pair(imageWidth, imageHeight)
        }

        // Apply zoom scale (zooming in shrinks the crop frame in image space)
        val effectiveScale = zoomScale.coerceAtLeast(1f)
        val cropW = (baseWidth / effectiveScale).toInt().coerceIn(1, imageWidth)
        val cropH = (baseHeight / effectiveScale).toInt().coerceIn(1, imageHeight)

        // Center position + pan offset
        val centerX = imageWidth / 2f + panOffsetX
        val centerY = imageHeight / 2f + panOffsetY

        val x = (centerX - cropW / 2f).toInt().coerceIn(0, imageWidth - cropW)
        val y = (centerY - cropH / 2f).toInt().coerceIn(0, imageHeight - cropH)

        return CropRegion(x, y, cropW, cropH)
    }

    /**
     * Center crop a bitmap to the specified dimensions.
     * The crop is centered on the bitmap.
     */
    fun centerCrop(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val cropWidth = minOf(targetWidth, bitmap.width)
        val cropHeight = minOf(targetHeight, bitmap.height)

        val x = (bitmap.width - cropWidth) / 2
        val y = (bitmap.height - cropHeight) / 2

        return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
    }

    /**
     * Crop a bitmap to the specified aspect ratio, centered.
     *
     * @param bitmap Source bitmap.
     * @param aspectRatioX Width component of the aspect ratio (e.g., 16 for 16:9).
     * @param aspectRatioY Height component of the aspect ratio (e.g., 9 for 16:9).
     * @return Cropped bitmap with the specified aspect ratio.
     */
    fun cropToAspectRatio(bitmap: Bitmap, aspectRatioX: Int, aspectRatioY: Int): Bitmap {
        val targetRatio = aspectRatioX.toFloat() / aspectRatioY.toFloat()
        val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        val (cropWidth, cropHeight) = if (srcRatio > targetRatio) {
            // Source is wider than target ratio → crop width
            val w = (bitmap.height * targetRatio).toInt()
            Pair(w, bitmap.height)
        } else {
            // Source is taller than target ratio → crop height
            val h = (bitmap.width / targetRatio).toInt()
            Pair(bitmap.width, h)
        }

        val x = (bitmap.width - cropWidth) / 2
        val y = (bitmap.height - cropHeight) / 2

        return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
    }

    /**
     * Calculate the default centered crop region for given dimensions.
     */
    fun calculateCenterCropRegion(
        bitmapWidth: Int,
        bitmapHeight: Int,
        cropWidth: Int,
        cropHeight: Int
    ): CropRegion {
        val effectiveWidth = minOf(cropWidth, bitmapWidth)
        val effectiveHeight = minOf(cropHeight, bitmapHeight)
        val x = (bitmapWidth - effectiveWidth) / 2
        val y = (bitmapHeight - effectiveHeight) / 2
        return CropRegion(x, y, effectiveWidth, effectiveHeight)
    }
}

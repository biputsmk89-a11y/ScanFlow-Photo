package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import android.graphics.Matrix
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for rotating and flipping bitmaps using Matrix transformations.
 */
@Singleton
class RotateEngine @Inject constructor() {

    /**
     * Rotate a bitmap by the specified degrees (clockwise).
     *
     * @param bitmap Source bitmap.
     * @param degrees Rotation angle in degrees (positive = clockwise).
     * @return Rotated bitmap. Returns the original if degrees is 0 or 360.
     */
    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val normalizedDegrees = degrees % 360f
        if (normalizedDegrees == 0f) return bitmap

        val matrix = Matrix().apply {
            postRotate(normalizedDegrees, bitmap.width / 2f, bitmap.height / 2f)
        }

        // For 90/270 degree rotations, width and height swap
        return if (normalizedDegrees % 90f == 0f && normalizedDegrees % 180f != 0f) {
            // Create a matrix that rotates around the center of the new dimensions
            val rotateMatrix = Matrix().apply {
                postRotate(normalizedDegrees)
            }

            // Calculate new dimensions
            val rect = android.graphics.RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            rotateMatrix.mapRect(rect)

            val newWidth = rect.width().toInt()
            val newHeight = rect.height().toInt()

            val finalMatrix = Matrix().apply {
                postRotate(normalizedDegrees, bitmap.width / 2f, bitmap.height / 2f)
                postTranslate(
                    (newWidth - bitmap.width) / 2f,
                    (newHeight - bitmap.height) / 2f
                )
            }

            val result = Bitmap.createBitmap(newWidth, newHeight, bitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(result)
            canvas.drawBitmap(bitmap, finalMatrix, null)
            result
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }

    /**
     * Rotate a bitmap by 90 degrees clockwise.
     */
    fun rotate90CW(bitmap: Bitmap): Bitmap = rotate(bitmap, 90f)

    /**
     * Rotate a bitmap by 90 degrees counter-clockwise.
     */
    fun rotate90CCW(bitmap: Bitmap): Bitmap = rotate(bitmap, 270f)

    /**
     * Rotate a bitmap by 180 degrees.
     */
    fun rotate180(bitmap: Bitmap): Bitmap = rotate(bitmap, 180f)

    /**
     * Flip a bitmap horizontally and/or vertically.
     *
     * @param bitmap Source bitmap.
     * @param horizontal If true, flip horizontally (mirror).
     * @param vertical If true, flip vertically.
     * @return Flipped bitmap. Returns the original if no flip is applied.
     */
    fun flip(bitmap: Bitmap, horizontal: Boolean = false, vertical: Boolean = false): Bitmap {
        if (!horizontal && !vertical) return bitmap

        val sx = if (horizontal) -1f else 1f
        val sy = if (vertical) -1f else 1f

        val matrix = Matrix().apply {
            preScale(sx, sy, bitmap.width / 2f, bitmap.height / 2f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

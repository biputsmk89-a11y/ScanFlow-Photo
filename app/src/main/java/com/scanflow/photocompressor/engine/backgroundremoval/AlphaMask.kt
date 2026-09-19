package com.scanflow.photocompressor.engine.backgroundremoval

/**
 * Encapsulates a continuous alpha mask representing human subject segmentation.
 * Values range from 0.0f (pure background) to 1.0f (pure foreground).
 */
data class AlphaMask(
    val width: Int,
    val height: Int,
    val alphaBuffer: FloatArray
) {
    init {
        require(alphaBuffer.size == width * height) {
            "Alpha buffer size (${alphaBuffer.size}) must match dimensions ($width x $height = ${width * height})"
        }
    }

    operator fun get(x: Int, y: Int): Float {
        if (x !in 0 until width || y !in 0 until height) return 0f
        return alphaBuffer[y * width + x]
    }

    operator fun set(x: Int, y: Int, value: Float) {
        if (x in 0 until width && y in 0 until height) {
            alphaBuffer[y * width + x] = value.coerceIn(0f, 1f)
        }
    }

    fun copy(): AlphaMask {
        return AlphaMask(width, height, alphaBuffer.copyOf())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AlphaMask
        if (width != other.width) return false
        if (height != other.height) return false
        if (!alphaBuffer.contentEquals(other.alphaBuffer)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + alphaBuffer.contentHashCode()
        return result
    }
}

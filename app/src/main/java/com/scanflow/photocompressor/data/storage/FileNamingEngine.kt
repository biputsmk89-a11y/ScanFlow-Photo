package com.scanflow.photocompressor.data.storage

import com.scanflow.photocompressor.domain.model.ConflictStrategy
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.OperationType
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles safe file naming according to specification 59:
 * - Sanitizes invalid characters to prevent file system and MediaStore corruption.
 * - Standardizes operation suffix formats (e.g. IMG_1234_compressed.jpg, IMG_1234_resized.jpg, IMG_1234_webp.webp).
 * - Enforces collision resolution strategies so files are never overwritten without explicit strategy.
 */
@Singleton
class FileNamingEngine @Inject constructor() {

    companion object {
        // Characters forbidden in Linux/Android/Windows filesystems: \ / : * ? " < > | and control chars (0x00 to 0x1F)
        private val INVALID_CHARS_REGEX = Regex("[\\\\/:*?\"<>|\\x00-\\x1F]")
        private val REPEATED_SEPARATORS_REGEX = Regex("[_\\s]{2,}")
        private const val MAX_BASE_NAME_LENGTH = 120
    }

    /**
     * Sanitizes a base file name to ensure it only contains valid filesystem characters.
     */
    fun sanitizeBaseName(originalName: String): String {
        // Strip extension if present
        val rawBase = if (originalName.contains(".")) {
            originalName.substringBeforeLast(".")
        } else {
            originalName
        }

        var sanitized = rawBase
            .replace(INVALID_CHARS_REGEX, "_")
            .replace(REPEATED_SEPARATORS_REGEX, "_")
            .trim('.', ' ', '_')

        if (sanitized.length > MAX_BASE_NAME_LENGTH) {
            sanitized = sanitized.take(MAX_BASE_NAME_LENGTH).trimEnd('.', ' ', '_')
        }

        return if (sanitized.isBlank()) "image" else sanitized
    }

    /**
     * Generates a standard formatted file name:
     * - Compress: IMG_1234_compressed.jpg
     * - Resize: IMG_1234_resized.jpg
     * - Convert: IMG_1234_webp.webp (or matching target format)
     * - Crop: IMG_1234_cropped.jpg
     * - Rotate: IMG_1234_rotated.jpg
     * - Watermark: IMG_1234_watermarked.jpg
     */
    fun generateFileName(
        originalName: String,
        operationType: OperationType = OperationType.COMPRESS,
        targetFormat: ImageFormat = ImageFormat.JPEG
    ): String {
        val baseName = sanitizeBaseName(originalName)
        val ext = targetFormat.extension

        val suffix = when (operationType) {
            OperationType.COMPRESS -> "compressed"
            OperationType.RESIZE -> "resized"
            OperationType.CROP -> "cropped"
            OperationType.ROTATE, OperationType.FLIP -> "rotated"
            OperationType.WATERMARK -> "watermarked"
            OperationType.CONVERT -> ext // e.g. IMG_1234_webp.webp
            OperationType.BATCH -> "compressed"
        }

        return "${baseName}_${suffix}.${ext}"
    }

    /**
     * Resolves file name conflicts using the specified [ConflictStrategy].
     * Never overwrites existing files unless explicitly configured with [ConflictStrategy.OVERWRITE].
     *
     * @param targetFileName The candidate file name to check.
     * @param strategy The conflict resolution strategy (INCREMENT, TIMESTAMP, OVERWRITE).
     * @param existsPredicate Predicate that returns true if a file with the given name already exists.
     * @return A unique filename guaranteed to avoid unintentional collisions.
     */
    fun resolveConflict(
        targetFileName: String,
        strategy: ConflictStrategy = ConflictStrategy.INCREMENT,
        existsPredicate: (String) -> Boolean
    ): String {
        if (!existsPredicate(targetFileName)) {
            return targetFileName
        }

        if (strategy == ConflictStrategy.OVERWRITE) {
            return targetFileName
        }

        val nameWithoutExt = if (targetFileName.contains(".")) {
            targetFileName.substringBeforeLast(".")
        } else {
            targetFileName
        }
        val ext = if (targetFileName.contains(".")) {
            targetFileName.substringAfterLast(".")
        } else {
            ""
        }
        val extWithDot = if (ext.isNotEmpty()) ".$ext" else ""

        return when (strategy) {
            ConflictStrategy.TIMESTAMP -> {
                var timestampCandidate = "${nameWithoutExt}_${System.currentTimeMillis()}$extWithDot"
                while (existsPredicate(timestampCandidate)) {
                    timestampCandidate = "${nameWithoutExt}_${System.currentTimeMillis() + 1}$extWithDot"
                }
                timestampCandidate
            }
            ConflictStrategy.INCREMENT -> {
                var counter = 1
                var candidate = "${nameWithoutExt}_$counter$extWithDot"
                while (existsPredicate(candidate)) {
                    counter++
                    candidate = "${nameWithoutExt}_$counter$extWithDot"
                }
                candidate
            }
            ConflictStrategy.OVERWRITE -> targetFileName
        }
    }

    /**
     * Resolves conflict on a filesystem directory.
     */
    fun resolveFileConflict(
        targetDir: File,
        targetFileName: String,
        strategy: ConflictStrategy = ConflictStrategy.INCREMENT
    ): File {
        val uniqueName = resolveConflict(targetFileName, strategy) { candidateName ->
            File(targetDir, candidateName).exists()
        }
        return File(targetDir, uniqueName)
    }
}

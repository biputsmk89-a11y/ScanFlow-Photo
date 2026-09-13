package com.scanflow.photocompressor.domain.model

/**
 * 52. ERROR MODEL
 * Typed errors for all image processing operations across ScanFlow Photo.
 */
sealed interface ProcessingError {
    data object InvalidImage : ProcessingError
    data object UnsupportedFormat : ProcessingError
    data object OutOfMemory : ProcessingError
    data object StorageFull : ProcessingError
    data object PermissionDenied : ProcessingError
    data object ProcessingCancelled : ProcessingError

    data class FileTooLarge(
        val fileSizeMB: Double = 0.0,
        val reason: String = ""
    ) : ProcessingError

    data class Unknown(
        val reason: String
    ) : ProcessingError

    /**
     * 53. ERROR UX
     * User-facing messages that never expose raw stack traces.
     */
    val userFacingMessage: String
        get() = when (this) {
            is InvalidImage -> "This image could not be opened.\nTry another file."
            is UnsupportedFormat -> "This image format is not supported.\nPlease choose a JPEG, PNG, or WEBP image."
            is OutOfMemory -> "This image is too large to process safely on this device.\nTry reducing its dimensions first."
            is FileTooLarge -> "This image is too large for device memory to safely process.\nProcessing was rejected to prevent crashes."
            is StorageFull -> "There is not enough storage space to save the result."
            is PermissionDenied -> "Permission was denied. Please grant permission to access the image."
            is ProcessingCancelled -> "Processing was cancelled."
            is Unknown -> if (reason.isNotBlank()) reason else "An unexpected error occurred. Please try again."
        }

    companion object {
        /**
         * Maps any generic [Throwable] or error string into a strongly-typed [ProcessingError].
         * Ensures no raw stack trace leaks to the user.
         */
        fun fromThrowable(throwable: Throwable?): ProcessingError {
            if (throwable == null) return Unknown("An unknown error occurred.")
            val msg = throwable.message?.lowercase() ?: ""

            return when {
                throwable is java.util.concurrent.CancellationException ||
                        throwable is kotlinx.coroutines.CancellationException -> ProcessingCancelled

                throwable is com.scanflow.photocompressor.engine.LargeImageRejectedException ||
                        msg.contains("safely rejected") ||
                        msg.contains("too large for current available device memory") -> {
                    val size = (throwable as? com.scanflow.photocompressor.engine.LargeImageRejectedException)?.fileSizeMB ?: 0.0
                    FileTooLarge(size, throwable.message ?: "")
                }

                throwable is OutOfMemoryError ||
                        msg.contains("out of memory") ||
                        msg.contains("oom") -> OutOfMemory

                throwable is SecurityException ||
                        msg.contains("permission denial") ||
                        msg.contains("permission denied") -> PermissionDenied

                throwable is java.io.IOException && (msg.contains("enospc") || msg.contains("no space left") || msg.contains("storage full")) -> StorageFull

                msg.contains("unsupported format") ||
                        msg.contains("unsupported mime") -> UnsupportedFormat

                throwable is java.io.FileNotFoundException ||
                        msg.contains("not exist") ||
                        msg.contains("no such file") ||
                        msg.contains("could not be decoded") ||
                        msg.contains("invalid image") ||
                        msg.contains("empty") ||
                        msg.contains("corrupt") ||
                        msg.contains("decode") -> InvalidImage

                else -> Unknown(throwable.localizedMessage?.take(100) ?: "An unexpected error occurred.")
            }
        }
    }
}

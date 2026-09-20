package com.scanflow.photocompressor

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class annotated with @HiltAndroidApp to enable Hilt DI.
 * Configures Coil's global ImageLoader with strict memory limits and smooth crossfade transitions.
 */
@HiltAndroidApp
class PhotoCompressorApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        setupGlobalCrashHandler()
    }

    private fun setupGlobalCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                android.util.Log.e("PhotoCompressorApp", "FATAL CRASH on thread ${thread.name}: ${throwable.message}", throwable)
                // Persist crash log to internal storage for diagnostic analysis
                val crashFile = getFileStreamPath("last_crash.log")
                crashFile?.outputStream()?.use { out ->
                    out.write("Crash on thread ${thread.name}: ${throwable.message}\n${android.util.Log.getStackTraceString(throwable)}".toByteArray())
                }
            } catch (e: Exception) {
                // Ignore logging failures to prevent secondary crashes
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Bound memory cache to at most 25% of available JVM heap to prevent OOM
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_preview_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB disk cache ceiling
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}

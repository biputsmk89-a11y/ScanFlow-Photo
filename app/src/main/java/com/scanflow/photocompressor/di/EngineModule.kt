package com.scanflow.photocompressor.di

import com.scanflow.photocompressor.engine.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing image processing engine instances.
 * All engines are singletons since they are stateless.
 */
@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideImagePipelineEngine(
        compressionEngine: CompressionEngine,
        resizeEngine: ResizeEngine,
        cropEngine: CropEngine,
        rotateEngine: RotateEngine,
        watermarkEngine: WatermarkEngine,
        formatConverter: FormatConverter,
        imageRepository: com.scanflow.photocompressor.domain.repository.ImageRepository,
        historyRepository: com.scanflow.photocompressor.domain.repository.HistoryRepository,
        exifHandler: com.scanflow.photocompressor.data.storage.ExifHandler,
        metadataEngine: MetadataEngine,
        fileManager: com.scanflow.photocompressor.data.storage.FileManager,
        outputValidator: OutputValidator
    ): ImagePipelineEngine {
        return ImagePipelineEngine(
            compressionEngine,
            resizeEngine,
            cropEngine,
            rotateEngine,
            watermarkEngine,
            formatConverter,
            imageRepository,
            historyRepository,
            exifHandler,
            metadataEngine,
            fileManager,
            outputValidator
        )
    }

    @Provides
    @Singleton
    fun provideBatchProcessor(
        pipelineEngine: ImagePipelineEngine
    ): BatchProcessor {
        return BatchProcessor(pipelineEngine)
    }

    @Provides
    @Singleton
    fun provideImageEngine(
        imageAnalyzer: ImageAnalyzer,
        pipelineEngine: ImagePipelineEngine
    ): ImageEngine {
        return ImageEngineImpl(imageAnalyzer, pipelineEngine)
    }

    @Provides
    @Singleton
    fun providePresetEngine(
        presetRepository: com.scanflow.photocompressor.domain.repository.PresetRepository
    ): PresetEngine {
        return PresetEngineImpl(presetRepository)
    }

    @Provides
    @Singleton
    fun provideBackgroundRemovalEngine(
        impl: com.scanflow.photocompressor.engine.backgroundremoval.MlKitSegmentationEngine
    ): com.scanflow.photocompressor.engine.backgroundremoval.BackgroundRemovalEngine {
        return impl
    }
}

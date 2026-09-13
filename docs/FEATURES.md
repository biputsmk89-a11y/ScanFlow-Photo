# ScanFlow Photo — Feature Inventory & Regression Guard

This document serves as the formal feature registry for **ScanFlow Photo (Photo Compressor + Image Tools)**. All features listed as `IMPLEMENTED` are actively tested and must be preserved during all builds and release pipelines.

---

## 1. Feature Registry

| Feature | Status | Module | UI Screen | Use Case | Engine | Unit Tests | Release |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Compress** | `IMPLEMENTED` | `:app` | `CompressScreen` | `CompressImageUseCase` | `CompressionEngine` | `CompressImageUseCaseTest` | **YES** |
| **Quick Compress** | `IMPLEMENTED` | `:app` | `HomeScreen` | `CompressImageUseCase` | `CompressionEngine` | `CompressViewModelTest` | **YES** |
| **Quality Control** | `IMPLEMENTED` | `:app` | `CompressScreen` | `CompressImageUseCase` | `CompressionEngine` | `ImageEngineTest` | **YES** |
| **Target File Size** | `IMPLEMENTED` | `:app` | `CompressScreen` | `CompressImageUseCase` | `ImagePipelineEngine` | `TargetSizeAlgorithmTest` | **YES** |
| **Resize** | `IMPLEMENTED` | `:app` | `ResizeScreen` | `CompressImageUseCase` | `ResizeEngine` | `ResizeEngineTest` | **YES** |
| **Crop** | `IMPLEMENTED` | `:app` | `CropScreen` | `CompressImageUseCase` | `CropEngine` | `CropEngineTest` | **YES** |
| **Convert (JPG)** | `IMPLEMENTED` | `:app` | `ConvertScreen` | `CompressImageUseCase` | `FormatConverter` | `FormatConverterTest` | **YES** |
| **Convert (PNG)** | `IMPLEMENTED` | `:app` | `ConvertScreen` | `CompressImageUseCase` | `FormatConverter` | `FormatConverterTest` | **YES** |
| **Convert (WebP)** | `IMPLEMENTED` | `:app` | `ConvertScreen` | `CompressImageUseCase` | `FormatConverter` | `FormatConverterTest` | **YES** |
| **Batch Processing** | `IMPLEMENTED` | `:app` | `BatchScreen` | `BatchCompressUseCase` | `BatchProcessor` | `BatchProcessorTest` | **YES** |
| **Batch Progress** | `IMPLEMENTED` | `:app` | `BatchScreen` | `BatchCompressUseCase` | `BatchProcessor` | `BatchViewModelTest` | **YES** |
| **Batch Cancel** | `IMPLEMENTED` | `:app` | `BatchScreen` | `BatchCompressUseCase` | `BatchProcessor` | `BatchProcessorTest` | **YES** |
| **Batch Retry** | `IMPLEMENTED` | `:app` | `BatchScreen` | `BatchCompressUseCase` | `BatchProcessor` | `BatchProcessorTest` | **YES** |
| **Result Summary** | `IMPLEMENTED` | `:app` | `CompressScreen`, `BatchScreen` | N/A | N/A | `UiFlowAndNavigationTest` | **YES** |
| **Before / After** | `IMPLEMENTED` | `:app` | `CompressScreen` | N/A | `ImagePreviewStrategy` | `ImagePreviewStrategyTest` | **YES** |
| **Save (MediaStore)** | `IMPLEMENTED` | `:app` | Common | `StorageManager` | `FileNamingEngine` | `OutputFileFlowTest` | **YES** |
| **Share Sheet** | `IMPLEMENTED` | `:app` | Common | `ShareHelper` | `FileProvider` | `ShareIntentHandlerTest` | **YES** |
| **History (Room)** | `IMPLEMENTED` | `:app` | `HistoryScreen` | `HistoryRepository` | Room DAO | `HistoryRepositoryImplTest` | **YES** |
| **System Presets** | `IMPLEMENTED` | `:app` | `CompressScreen` | `ManagePresetsUseCase` | `PresetEngine` | `PresetEngineTest` | **YES** |
| **Custom Presets** | `IMPLEMENTED` | `:app` | `CompressScreen` | `ManagePresetsUseCase` | `PresetEngine` | `PresetEngineTest` | **YES** |
| **EXIF Metadata** | `IMPLEMENTED` | `:app` | `CompressScreen` | `CompressImageUseCase` | `MetadataEngine` | `MetadataEngineTest` | **YES** |
| **GPS Removal** | `IMPLEMENTED` | `:app` | `CompressScreen` | `CompressImageUseCase` | `MetadataEngine` | `MetadataEngineTest` | **YES** |
| **Multi-page PDF** | `IMPLEMENTED` | `:app` | `PdfScreen` | `PdfEngine` | `ImagePipelineEngine` | `PdfEngineTest` | **YES** |
| **Passport & ID** | `IMPLEMENTED` | `:app` | `PassportScreen` | `PassportEngine` | `ImagePipelineEngine` | `PassportEngineTest` | **YES** |
| **Social Presets** | `IMPLEMENTED` | `:app` | `SocialScreen` | `SocialMediaEngine` | `ImagePipelineEngine` | `SocialMediaEngineTest` | **YES** |
| **WhatsApp Ready** | `IMPLEMENTED` | `:app` | `WhatsAppScreen` | `WhatsAppEngine` | `ImagePipelineEngine` | `WhatsAppEngineTest` | **YES** |
| **Google Play Billing** | `IMPLEMENTED` | `:app` | `SettingsScreen` | `UserTierRepository` | `BillingManager` | `FeatureGatingAndBillingTest` | **YES** |
| **Entitlement Tier** | `IMPLEMENTED` | `:app` | Common | `EntitlementRepository` | N/A | `UserTierArchitectureTest` | **YES** |
| **AdMob Protection** | `IMPLEMENTED` | `:app` | Common | `AdManager` | N/A | `FeatureGatingAndBillingTest` | **YES** |
| **Offline Privacy** | `IMPLEMENTED` | `:app` | Core | `PrivacyContract` | N/A | `PrivacyContractTest` | **YES** |
| **Large Image Safety** | `IMPLEMENTED` | `:app` | Core | `BitmapUtils` | `LargeImageStrategy` | `LargeImageStrategyTest` | **YES** |

---

## 2. Regression Protection Guarantees
1. **Zero Feature Loss**: No CI or release script is permitted to exclude or disable any feature listed above.
2. **Offline Processing Invariant**: All image processing executes strictly on-device without network calls.
3. **Concurrency Limit**: Batch operations strictly maintain a maximum concurrency of 1 in-flight bitmap to prevent OOM.

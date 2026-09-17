<p align="center">
  <img src=".github/assets/banner.png" alt="ScanFlow Photo Banner" width="100%" />
</p>

<p align="center">
  <a href="https://github.com/biputsmk89-a11y/ScanFlow-Photo"><img src="https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android" alt="Android Platform" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.0-7F52FF.svg?style=flat&logo=kotlin" alt="Kotlin Version" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg?style=flat" alt="Jetpack Compose" /></a>
  <img src="https://img.shields.io/badge/Architecture-Clean%20%2B%20MVI%2FMVVM-0E1728.svg?style=flat" alt="Clean Architecture" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline-10B981.svg?style=flat" alt="100% Offline" />
</p>

# ScanFlow Photo — Photo Compressor + Image Studio

A state-of-the-art, 100% offline Android photo utility built with Jetpack Compose, Material 3, Kotlin Coroutines, and Clean Architecture.
Modern UI/UX design crafted with Google Stitch.

## 📱 UI Showcase (Google Stitch System)

<p align="center">
  <img src=".github/assets/ui_home.png" width="19%" alt="Home" />
  <img src=".github/assets/ui_compress.png" width="19%" alt="Compress & Target Size" />
  <img src=".github/assets/ui_batch.png" width="19%" alt="Batch Processing" />
  <img src=".github/assets/ui_passport.png" width="19%" alt="Passport & ID Studio" />
  <img src=".github/assets/ui_pdf.png" width="19%" alt="PDF Maker" />
</p>

## 🌟 Key Features

- **Photo Compression**:
  - Precision quality slider (1–100%)
  - Target file size (KB/MB) with iterative binary search convergence and dimension fallback
  - Real-time statistics: Before, After, Saved KB/MB, and Reduction %
- **Image Transformations**:
  - Proportional & custom resizing
  - Freeform and preset aspect-ratio cropping (1:1, 4:3, 16:9, etc.)
  - 90° rotation and orientation correction
- **Format Conversion**:
  - Full cross-format conversion between JPG, PNG, and WebP (Lossy & Lossless)
- **Batch Processing**:
  - Multi-image picker with WorkManager background execution
  - Peak memory safety: strict Concurrency = 1 (only 1 bitmap in-flight at any time)
  - Live progress updates, cooperative cancellation, and smart retry (skips completed items)
- **Advanced Tools**:
  - **Multi-page PDF**: Select, reorder, auto-fit, and generate standardized PDF documents
  - **Passport & ID**: Biometric guideline overlays, standard ID dimensions, and print cut-sheets
  - **Social Media Presets**: Ready-to-use aspect ratios for Instagram, Facebook, YouTube, TikTok
  - **WhatsApp Optimization**: Small, Balanced, and HD presets tailored for messaging
- **Privacy & Metadata**:
  - On-device EXIF metadata stripping and selective GPS removal
  - **100% Offline Processing**: Zero telemetry on photo bytes, paths, or contents
- **Persistent History**:
  - Room database tracking all operations, compression savings, and output files with direct sharing

---

## 🏗 Architecture

The app follows **Clean Architecture** and Android Architecture Components:

```text
UI (Jetpack Compose + Material 3)
       │
Presentation (StateFlow + ViewModels)
       │
Domain (UseCases & Feature Models)
       │
Image Pipeline (ImagePipelineEngine)
 ├── ImageAnalyzer (Bounds & EXIF decoding)
 ├── ResizeEngine / CropEngine / RotateEngine
 ├── CompressionEngine / FormatConverter
 └── OutputValidator
       │
Storage & Persistence
 ├── MediaStore & FileProvider (Scoped Storage)
 ├── Room Database (Processing History)
 └── DataStore (Preferences)
```

---

## 🚀 Building & Testing

### Prerequisites
- JDK 17 (Eclipse Temurin recommended)
- Android SDK (API 34)

### Build Commands
```bash
# Run unit tests
./gradlew test

# Run Android Lint
./gradlew lintDebug

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Release APK (with R8 minification)
./gradlew assembleRelease

# Bundle Release AAB (for Google Play Console)
./gradlew bundleRelease
```

---

## 📦 Build Variants & Artifacts

| Variant | APK Output | AAB Output | Signed | Purpose |
|:---|:---:|:---:|:---:|:---|
| **Debug** | `app/build/outputs/apk/debug/app-debug.apk` | N/A | Debug Key | Local development and UI smoke testing |
| **Release** | `app/build/outputs/apk/release/app-release-unsigned.apk` (or signed) | `app/build/outputs/bundle/release/app-release.aab` | Configurable | Production distribution and Google Play Console upload |

---

## 🤖 GitHub Actions & CI/CD

Automated workflows are located in `.github/workflows/`:
1. **`android-ci.yml`**: Runs on push to `main`/`develop` and on pull requests. Runs tests, lint, and builds debug APK.
2. **`pull-request.yml`**: Validates incoming pull requests.
3. **`android-release.yml`**: Triggered on tag push (`v*.*.*`). Decodes keystore secrets, executes test gates, builds signed APK and AAB, computes SHA-256 checksums, and publishes a formal GitHub Release.

For signing configuration details, see [SIGNING.md](docs/SIGNING.md).  
For the feature registry, see [FEATURES.md](docs/FEATURES.md).  
For the release procedure, see [RELEASE.md](docs/RELEASE.md).

---

## 🔒 Privacy Guarantee

ScanFlow Photo operates entirely offline. All image decoding, transformation, compression, and encoding happen strictly within device memory and private sandboxed caches. No photo, pixel data, or personal information is ever transmitted to external servers.

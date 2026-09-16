# ScanFlow Photo — Remediation Baseline Snapshot

**Timestamp:** 2026-09-15 12:57:00 UTC+7  
**Branch:** `main`  
**Latest Commit:** `8cdb693 chore(build): upgrade Gradle wrapper to 8.9 to synchronize IDE and CLI build environments`  
**Git Working Tree Status:** Clean  
**Android Gradle Plugin:** 8.5.1  
**Kotlin:** 2.0.0  
**Compile SDK:** 34  
**Target SDK:** 34  
**Min SDK:** 26  

---

## 1. Baseline Verification Status

| Check | Result | Detail |
|---|---|---|
| **Git Status** | **PASS** | Clean working tree, on branch `main` |
| **Debug Build (`assembleDebug`)** | **PASS** | Verified |
| **Release Build (`assembleRelease`)** | **PASS** | R8 Minification, Proguard, & Lint Vital pass |
| **Release Bundle (`bundleRelease`)** | **PASS** | App Bundle `.aab` successfully generated |
| **Unit Test Suite (`test`)** | **PASS** | 62 actionable tasks clean, all tests passing |

---

## 2. Protected Working Modules (DO NOT BREAK / REWRITE)

These modules are verified working, robust, memory-safe, and must remain intact during remediation:

1. **`ImagePipelineEngine.kt`** — Centralized 9-stage memory-safe image processing pipeline.
2. **`BitmapUtils.kt`** — Memory-safe decode, downsampling, EXIF orientation normalization, anti-OOM bounds.
3. **`LargeImageStrategy.kt`** — Tiered decoding thresholds for up to 100+ Megapixel photos.
4. **`PassportEngine.kt`** — Offline BFS flood-fill background replacement, corner color sampling, face guidance framing, multi-photo print sheet layouts with cut marks.
5. **`PdfEngine.kt`** — Multi-image PDF document generation using native Android `PdfDocument`.
6. **`FormatConverter.kt`** — Alpha flattening and clean format transcoding between JPEG, PNG, WEBP.
7. **`ResizeEngine.kt`** & **`RotateEngine.kt`** — Aspect ratio dimension math and lossless 90/180/270 degree rotation/flips.
8. **`WatermarkEngine.kt`** — Text overlay with relative font scaling and shadow layer.
9. **`BatchProcessor.kt`** — Adaptive concurrency policy, cooperative coroutine cancellation, temp file cleanup.
10. **`SocialPresetRegistry.kt`** — Comprehensive preset resolution matrix for Instagram, TikTok, YouTube, Facebook, LinkedIn.

---

## 3. Known Audit Issues Catalog

| ID | Issue Area | Priority | Root Cause & Remediation Target |
|---|---|---|---|
| **P0-1** | **Fake In-App Billing / Mock Pro** | P0 (Blocker) | `BillingManagerImpl.kt` generates fake order IDs/tokens without Google Play Billing Library. Pro claims "zero ads & unlimited batch" are misleading. **Remediation: Free-First Release (Option B)** — remove Pro purchase UI card, keep architecture clean and extensible, position app honestly as 100% Free & Offline. |
| **P0-2** | **Save Flow Semantics Mismatch** | P0 (Blocker) | Files are already auto-persisted to `Pictures/PhotoCompressor` during pipeline execution; "SAVE" button on ResultScreen is an illusion that merely changes UI state. **Remediation:** Update button semantics to "Open in Gallery" / "View File" with transparent "Auto-saved to Pictures/PhotoCompressor" banner. |
| **P0-3** | **WhatsApp Quality Slider Bug** | P0 (Blocker) | `WhatsAppScreen.kt` slider `onValueChange` calls `viewModel.updateCustomTargetSize(it.toInt())` instead of `updateCustomQuality`. Slider doesn't update quality and silently corrupts target size. **Remediation:** Bind to `updateCustomQuality()`. |
| **P0-4** | **Crop Coordinate Space & Direction Bug** | P0 (Blocker) | `CropScreen.kt` gestures send screen pixels (~280dp box) directly to `CropEngine.kt` bitmap pixels (4000x3000px) with inverted pan translation. **Remediation:** Implement explicit screen-to-bitmap coordinate scaling and correct pan direction. |
| **P0-5** | **History Summary Stale & Items Unclickable** | P0 (Blocker) | `HistoryViewModel.kt` calculates `totalSavedBytes` and `totalOperations` only once at `init`. History items in `HistoryScreen.kt` cannot be clicked to open or share. **Remediation:** Reactive summary from Flow, clickable items opening detail sheet with Open/Share actions and missing file handling. |
| **P0-6** | **PDF Transient Cache Persistence** | P0 (Blocker) | Generated PDF resides in temporary cache with only "Share" button. If not shared immediately, user can lose the PDF. **Remediation:** Provide persistent save to `Downloads/Documents` via MediaStore/SAF and "Open Document" action. |
| **P1-1** | **Before/After Semantics & Negative Savings** | P1 (High) | `BeforeAfterPreview.kt` shows `"Saved -24.5%"` in green when output is larger. **Remediation:** Introduce `SizeChangeType` (REDUCED, UNCHANGED, INCREASED) with proper color semantics and decouple pixel resolution from byte size. |
| **P1-2** | **PNG Target Size Quality Loop** | P1 (High) | PNG ignores quality in `Bitmap.compress()`. Target size loop wastes 7 iterations on binary search. **Remediation:** Branch PNG to dimension scaling only. |
| **P1-3** | **Localization & Dead Feature Flag Cleanup** | P1 (Cleanup) | Passport notice hardcoded in Indonesian while screen is English. Compose strings hardcoded in Kotlin. `ENABLE_AI = false` unused. **Remediation:** Clean up hardcoded strings, unify language, remove dead `ENABLE_AI` flag. |

---

## 4. Remediation Order

- **PROMPT 0:** Baseline Snapshot & Safe Setup *(Completed)*
- **PROMPT 1:** Free-First Release: Remove Fake Billing, Remove Misleading Claims
- **PROMPT 2:** Save Flow Semantics: Real Output Lifecycle, Auto-save Transparency, Open in Gallery
- **PROMPT 3:** WhatsApp Quality Slider Fix & State Decoupling
- **PROMPT 4:** Crop Coordinate Space Transformation, Proper Scaling & Pan Direction
- **PROMPT 5:** History Reactive Aggregation & Interactive Item Detail/Open/Share
- **PROMPT 6:** PDF Persistent Storage (Save to Documents/Downloads) & Open Action
- **PROMPT 7:** Before/After Semantic State (Reduced / Unchanged / Increased)
- **PROMPT 8:** PNG Target-Size Strategy (Dimension-only, Quality Search Skipped)
- **PROMPT 9:** Localization Consistency & `ENABLE_AI` Cleanup
- **PROMPT 10:** Full Integration Regression & Final Release Gate

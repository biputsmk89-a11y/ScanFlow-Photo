# ScanFlow Photo — Release Procedure

This document describes the end-to-end release process for generating, signing, verifying, and publishing **ScanFlow Photo (Photo Compressor + Image Tools)**.

---

## 1. Release Architecture Overview

```text
Tag Push (e.g. v1.0.0)
       ↓
GitHub Actions (android-release.yml)
       ↓
Decode Keystore Secret ($RUNNER_TEMP)
       ↓
Run Tests (./gradlew test)
       ↓
Build Signed APK (./gradlew assembleRelease)
       ↓
Build Signed AAB (./gradlew bundleRelease)
       ↓
Generate Checksums (SHA256SUMS.txt)
       ↓
Create GitHub Release + Attach Binaries
       ↓
Purge Keystore Material
```

---

## 2. Step-by-Step Release Guide

### Step 1: Verify Local State & Run Tests
Ensure all unit tests pass locally before initiating a release:
```bash
./gradlew test
./gradlew assembleDebug
```

### Step 2: Update Version
Update the release metadata in `app/build.gradle.kts`:
- Increment `versionCode` (e.g., `1` ➔ `2`)
- Update `versionName` (e.g., `"1.0.0"` ➔ `"1.0.1"`)

### Step 3: Commit and Push Changes
Commit the version bump with a conventional commit message:
```bash
git add app/build.gradle.kts
git commit -m "build(release): prepare v1.0.0"
git push origin main
```

### Step 4: Create and Push Git Tag
Tag the commit using semantic versioning prefixed with `v`:
```bash
git tag v1.0.0
git push origin v1.0.0
```

### Step 5: Automated GitHub Actions Release
1. The `android-release.yml` workflow triggers automatically upon detecting tag push `v*`.
2. It executes unit tests, builds the signed APK and signed AAB, generates `SHA256SUMS.txt`, and creates a GitHub Release.
3. If any test or build step fails, the workflow terminates immediately and no release is created.

### Step 6: Google Play Console Submission
1. Navigate to the created **GitHub Release** under `Releases`.
2. Download `PhotoCompressor-1.0.0-release.aab` and `PhotoCompressor-1.0.0-mapping.txt`.
3. Open **Google Play Console** ➔ Your App ➔ **Production** (or **Internal Testing**).
4. Create a new release and upload `PhotoCompressor-1.0.0-release.aab`.
5. Upload `PhotoCompressor-1.0.0-mapping.txt` under the App Bundle's deobfuscation files section.
6. Complete release rollout.

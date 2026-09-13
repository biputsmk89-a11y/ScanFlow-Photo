# ScanFlow Photo — Signing Architecture & Secret Management

This document outlines the secure signing infrastructure for **ScanFlow Photo**.

> **SECURITY NOTICE**: Never commit private keys, keystores (`.jks`/`.keystore`), or plain-text passwords into git repositories, issue trackers, or documentation.

---

## 1. Required GitHub Secrets

To enable automated release signing in GitHub Actions, configure the following secrets in **GitHub Repository Settings ➔ Secrets and variables ➔ Actions**:

| Secret Name | Description | Example / Format |
|:---|:---|:---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded binary string of the production release keystore (`.jks` file). | `base64 -w 0 release.jks` (Linux/macOS) or `[Convert]::ToBase64String([IO.File]::ReadAllBytes('release.jks'))` (PowerShell) |
| `ANDROID_KEYSTORE_PASSWORD` | Master password for the release keystore file. | `PasswordForKeystore` |
| `ANDROID_KEY_ALIAS` | Key alias identifying the release signing key within the keystore. | `scanflow-release-key` |
| `ANDROID_KEY_PASSWORD` | Password for the release key alias. | `PasswordForKeyAlias` |

---

## 2. Generating a Keystore Base64 String

To generate the base64 string for `ANDROID_KEYSTORE_BASE64`:

### PowerShell (Windows):
```powershell
[Convert]::ToBase64String([System.IO.File]::ReadAllBytes("path\to\release.jks")) | Set-Clipboard
```
*(The base64 encoded string is copied directly to your clipboard for pasting into GitHub Secrets).*

### Bash (macOS / Linux):
```bash
base64 -w 0 path/to/release.jks | pbcopy # or xclip
```

---

## 3. Secret Lifecycle in GitHub Actions Workflow

1. Keystore string is read from `secrets.ANDROID_KEYSTORE_BASE64`.
2. Decoded strictly into `$RUNNER_TEMP/keystore/release.jks` (isolated runner temporary storage).
3. Passed to Gradle via environment variables:
   - `KEYSTORE_FILE`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`
4. In the `always()` post-build phase, the temporary keystore directory is completely purged.
5. If secrets are not configured, the workflow falls back gracefully to unsigned release outputs for verification.

---

## 4. Key Rotation Procedure

If the release keystore needs rotation:
1. Generate the new keystore with Google Play App Signing key migration if already published on Google Play.
2. Generate the base64 string from the new keystore.
3. Update the `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` values in repository secrets.
4. Trigger a tag release build to verify signing against the rotated key.

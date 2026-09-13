# Privacy Policy for ScanFlow Photo

**Effective Date:** September 13, 2026  
**Last Updated:** September 13, 2026  
**Live URL for Google Play Console:** `https://biputsmk89-a11y.github.io/ScanFlow-Photo/privacy-policy.html`

---

## 1. Introduction
ScanFlow Photo ("we", "our", or "the application") is committed to protecting your personal privacy. This Privacy Policy details our data handling practices and confirms that **ScanFlow Photo functions as a 100% offline, on-device application.**

---

## 2. Zero Data Collection Policy
We believe your photos and files belong strictly to you. ScanFlow Photo:
* **Does NOT collect, store, upload, or transmit any photos or image content.**
* **Does NOT collect biometric data, facial geometry, or personal identifiers.**
* **Does NOT use telemetry, usage trackers, or behavioral analytics.**
* **Does NOT track user location, contacts, device identifiers (IMEI/MAC), or phone details.**
* **Does NOT require user account registration or personal authentication.**

---

## 3. On-Device Image Processing
All compression, resizing, conversion (JPEG, PNG, WebP), cropping, passport generation, and PDF export algorithms are executed completely inside your device's memory using local native Android APIs. 

No image data is ever transferred across the internet or stored on external cloud infrastructure.

---

## 4. Permissions & Android Scoped Storage
ScanFlow Photo requests only the minimum set of permissions necessary to function:
* **Read Images / Storage (`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`):** Required only to display your local photo library within the photo picker so you can select photos to edit.
* **Write Images / Storage (`WRITE_EXTERNAL_STORAGE` on Android 9 and lower):** Required to save processed images and generated PDF files to your device's public `Pictures/ScanFlow` directory. On modern Android versions (Android 10+), standard MediaStore scoped storage is used without broad storage access.
* **Camera (`android.permission.CAMERA`):** Optional. Used only when you choose to take a real-time photo within the ID/Passport capture screen.

---

## 5. In-App Purchases & Google Play Billing
If you purchase the optional ScanFlow Pro upgrade or subscription:
* Payments are processed directly through the **Google Play In-App Billing API**.
* We do not handle, store, or receive your credit card numbers, billing addresses, or payment credentials.
* Google LLC manages all billing interactions in accordance with the [Google Play Terms of Service](https://play.google.com/intl/en_us/about/play-terms/) and Google Privacy Policy.

---

## 6. Security
Your photos remain strictly protected within the Android application sandbox. Because the application does not transmit data over the network, there is zero risk of remote intercept or cloud data leaks.

---

## 7. Open Source & Transparency
The full source code of ScanFlow Photo is available for inspection and verification at:  
[https://github.com/biputsmk89-a11y/ScanFlow-Photo](https://github.com/biputsmk89-a11y/ScanFlow-Photo)

---

## 8. Contact
If you have any questions or feedback regarding this Privacy Policy, please file an issue on GitHub or contact the maintainers via the repository issue tracker.

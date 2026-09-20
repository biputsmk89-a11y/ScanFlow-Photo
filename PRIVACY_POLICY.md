# Privacy Policy for ScanFlow Photo

**Last updated:** September 20, 2026

ScanFlow Photo ("we", "our", or "the app") is committed to protecting your privacy. This Privacy Policy explains our practices regarding data collection, usage, and disclosure when you use the ScanFlow Photo mobile application.

---

### 1. 100% On-Device Local Processing
ScanFlow Photo is built from the ground up as an **offline-first, privacy-respecting utility**:
- **All photo compression, resizing, format conversion, cropping, watermark application, and PDF generation are performed entirely on your device.**
- We do **NOT** upload your photos, metadata, or documents to any remote servers, cloud infrastructure, or third parties.
- Your photos and files never leave your device unless you explicitly choose to share them using your device's native share menu.

---

### 2. Information We Do NOT Collect
- We do **NOT** collect personally identifiable information (such as your name, email address, phone number, or physical location).
- We do **NOT** collect biometric identifiers from photos. Any facial or subject detection (such as for passport photo background removal) uses on-device Google ML Kit models and runs strictly within device memory.
- We do **NOT** sell, rent, monetize, or trade any user data or imagery.

---

### 3. Device Permissions and How They Are Used
ScanFlow Photo requests only the minimum permissions necessary to function:

- **Photos and Media / Storage (`READ_EXTERNAL_STORAGE` on Android 12 and below):**
  Required strictly to allow you to select photos from your device gallery for compression and editing, and to save processed photos to your device storage. On Android 13 and above, the app utilizes the secure Android Photo Picker and MediaStore API without broad storage access.
- **Camera (`android.permission.CAMERA`):**
  Optional. Used solely when you tap the in-app camera button to capture a photo directly for compression or passport photo creation.

We do not request or use broad file manager permissions (`MANAGE_EXTERNAL_STORAGE`).

---

### 4. Third-Party Services & Libraries
ScanFlow Photo uses trusted, industry-standard Android Jetpack libraries:
- **Google ML Kit (Selfie Segmentation):** Executed locally on-device. No telemetry or images are transmitted.
- **AndroidX & Material 3:** System UI and architecture components.
- The app contains **no advertising SDKs, no behavioral trackers, and no third-party analytic networks.**

---

### 5. Children's Privacy
ScanFlow Photo does not knowingly collect personal information from children under the age of 13. The application is safe for general audiences of all ages.

---

### 6. Changes to This Privacy Policy
We may update our Privacy Policy periodically. Any updates will be reflected with a revised "Last updated" date at the top of this page.

---

### 7. Contact Us
If you have any questions or suggestions regarding this Privacy Policy, please contact us at:
- **Email:** support@scanflowphoto.com (or developer contact email)
- **Developer Organization:** ScanFlow Photo Team

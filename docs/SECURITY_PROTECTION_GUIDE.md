# Panduan Keamanan & Perlindungan Anti-Pencurian Kode GitHub
**Target Akun:** [biputsmk89-a11y](https://github.com/biputsmk89-a11y)  
**Proyek:** ScanFlow Photo & Seluruh Proyek Rahasia Anda

Dokumen ini adalah SOP (Standar Operasional Prosedur) untuk memastikan tidak ada satu orang pun di dunia yang bisa mendownload, mencuri, atau meng-copy paste source code proyek Anda di GitHub.

---

### 1. Kunci Utama: Pastikan Repositori Selalu "PRIVATE"
* **Status Private** adalah dinding pertahanan terkuat di GitHub:
  * Siapa pun yang mencoba membuka URL repository Anda (`https://github.com/biputsmk89-a11y/ScanFlow-Photo`) akan langsung mendapatkan halaman **404 Not Found**.
  * Tombol **"Code > Download ZIP"**, `git clone`, dan seluruh isi file **hanya bisa dilihat dan diunduh oleh Anda sendiri**.
* **Cara Cek:** Buka repositori Anda, pastikan ada label **"Private"** di samping nama repositori.

---

### 2. Matikan Fitur "Forking" (Anti-Duplikasi Repositori)
Secara default, jika Anda menambahkan teman/kontributor, mereka bisa menduplikasi (Fork) repositori ke akun mereka. Matikan fitur ini:
1. Buka repositori Anda di GitHub.
2. Klik tab **Settings** (di pojok kanan atas).
3. Di menu **General**, gulir ke bawah ke bagian **Features**.
4. Hapus centang pada: **"Allow forking"**.
5. Klik simpan. Sekarang, tidak ada yang bisa membuat salinan (*fork*) repositori Anda.

---

### 3. Batasi Akses Kolaborator (Collaborators)
1. Di halaman **Settings**, klik menu **Collaborators** di sisi kiri.
2. Pastikan **TIDAK ADA NAMA ORANG ASING** di daftar tersebut.
3. Jangan pernah mengundang orang yang tidak memiliki perjanjian kerahasiaan resmi (NDA).

---

### 4. Aktifkan Branch Protection pada Branch `main`
Agar tidak ada orang yang bisa menimpa, merusak, atau menghapus kode Anda:
1. Di **Settings**, klik menu **Branches** (di sisi kiri).
2. Klik **Add branch protection rule** (atau klik Edit pada rule `main`).
3. Ketik Branch name pattern: `main`.
4. Centang opsi:
   - **Require a pull request before merging**
   - **Require status checks to pass before merging** (pilih `Secret & Credential Leak Scan`)
   - **Do not allow bypassing the above settings**
   - **Restrict who can push to matching branches** -> Masukkan hanya akun Anda (`biputsmk89-a11y`).
5. Klik **Create** / **Save changes**.

---

### 5. Amankan Akun GitHub Anda (Cegah Akun Dibajak)
Pencurian kode sering terjadi karena akun developer diretas. Lakukan langkah ini sekarang:
1. Buka **[github.com/settings/security](https://github.com/settings/security)**.
2. **Two-Factor Authentication (2FA):** Pastikan sudah **Enabled** (aktif) menggunakan aplikasi Google Authenticator di HP Anda.
3. Buka **[github.com/settings/applications](https://github.com/settings/applications)**:
   - Cek tab **Authorized OAuth Apps**. Jika ada aplikasi pihak ketiga yang tidak Anda kenali, klik **Revoke**.
4. Buka **[github.com/settings/tokens](https://github.com/settings/tokens)**:
   - Periksa **Personal access tokens**. Hapus token yang sudah tidak digunakan atau tidak Anda buat.

---

### 6. Sistem Proteksi Otomatis yang Sudah Diaktifkan di Proyek Ini:
1. **Local Pre-Commit Hook (`.githooks/pre-commit`):**  
   Secara otomatis memblokir perintah `git commit` di komputer Anda jika tidak sengaja memasukkan file keystore `.jks`, `.properties`, atau kata sandi rahasia.
2. **Cloud Security Guard (`.github/workflows/security-guard.yml`):**  
   GitHub Actions akan otomatis memeriksa setiap push untuk memastikan tidak ada kunci rahasia yang lolos ke cloud.
3. **Proprietary License Legal Shield (`LICENSE`):**  
   Melarang keras segala bentuk penyalinan, rekayasa balik (reverse engineering), dekompilasi, atau publikasi ulang tanpa izin tertulis dari Anda.
4. **ProGuard R8 Code Obfuscation:**  
   Kode sumber pada APK/AAB hasil build diacak secara otomatis sehingga file instalasi di HP tidak bisa dibongkar kembali menjadi kode asli.

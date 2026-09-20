# Panduan & Metadata Rilis Google Play Console - ScanFlow Photo

Dokumen ini berisi seluruh data dan teks siap salin (*copy-paste*) untuk mengisi formulir di **Google Play Console**.

---

### 1. Informasi Aplikasi Utama (Store Listing)

* **Nama Aplikasi (App Name - Max 30 Karakter):**  
  `ScanFlow Photo: Kompres & Edit`

* **Deskripsi Singkat (Short Description - Max 80 Karakter):**  
  `Kompres foto, resize, ubah format, pasfoto & PDF cepat, presisi, 100% offline.`

* **Deskripsi Lengkap (Full Description - Max 4000 Karakter):**
```text
ScanFlow Photo adalah aplikasi utilitas foto serbaguna dan canggih yang dirancang untuk mengompres, mengubah ukuran (resize), mengonversi format gambar, membuat pasfoto formal, hingga mengekspor ke dokumen PDF dalam hitungan detik.

100% OFFLINE & PRIVASI TERJAMIN
Seluruh pemrosesan foto berjalan lokal di perangkat Anda. Foto dan dokumen Anda TIDAK PERNAH diunggah ke internet atau server cloud mana pun. Privasi data Anda aman 100%.

FITUR UNGGULAN SCANFLOW PHOTO:

1. Kompresi Foto Cerdas & Presisi
• Kompres ukuran file hingga 90% dengan tetap menjaga ketajaman gambar.
• Mode Target Ukuran: Tentukan ukuran target file yang Anda inginkan (misal tepat 200 KB atau 1 MB untuk syarat unggah CPNS, BUMN, atau portal kerja).
• Mode Kualitas Manual & Kompresi Cepat sekali ketuk.

2. Resize & Ubah Dimensi
• Sesuaikan dimensi foto berdasarkan piksel presisi atau persentase.
• Kunci rasio aspek (aspect ratio lock) agar foto tidak terdistorsi atau gepeng.

3. Konversi Format Gambar Lengkap
• Ubah format foto dengan cepat antara JPG/JPEG, PNG, dan WebP generasi baru dengan efisiensi tinggi.

4. Pembuat Pasfoto Formal (ID Photo Maker)
• Hapus background foto secara instan dan otomatis menggunakan AI On-Device.
• Pilihan warna latar belakang formal standar: Merah, Biru, Putih, atau Abu-abu.
• Ukuran standar cetak: 2x3 cm, 3x4 cm, 4x6 cm, dan rasio foto paspor internasional.

5. Gabung Foto ke PDF (Photo to PDF)
• Konversi satu atau banyak foto menjadi dokumen PDF bersih dan siap kirim atau cetak.

6. Mode Pemrosesan Massal (Batch Processing)
• Kompres atau konversi puluhan foto sekaligus dalam sekali proses untuk menghemat waktu Anda.

7. Watermark & Tanda Air
• Lindungi karya foto dan dokumen penting Anda dengan watermark teks kustom yang dapat diatur transparansi dan posisinya.

8. Riwayat & Manajemen Penyimpanan
• Pantau hasil penghematan ruang penyimpanan (MB yang berhasil dihemat).
• Akses hasil kompresi langsung ke galeri perangkat.

ScanFlow Photo dibuat ringan, cepat, tanpa iklan yang mengganggu, dan siap meningkatkan produktivitas Anda sehari-hari!
```

---

### 2. Panduan Kuesioner Keamanan Data (Data Safety)

Di Google Play Console menu **App Content > Data safety**, jawab sebagai berikut:
1. **Does your app collect or share any user data?**  
   -> Pilih: **No** (Aplikasi tidak mengumpulkan atau membagikan data pengguna ke server luar).
2. **Data transfer over a secure connection:**  
   -> Karena tidak ada transmisi data internet, otomatis aman.

---

### 3. Panduan Akses Izin (App Permissions)

* **Camera (`android.permission.CAMERA`):**  
  Tujuan: Mengambil foto secara langsung untuk diedit/dikompres atas inisiatif pengguna.
* **Storage (`READ_EXTERNAL_STORAGE` / MediaStore):**  
  Tujuan: Membaca foto yang dipilih pengguna dari galeri dan menyimpan foto yang telah diproses.

---

### 4. Aset Visual Toko (Sudah Siap di Folder `play_store_assets/`)

1. **Ikon Aplikasi:** `play_store_assets/app_icon_512x512.png` (512x512 px)
2. **Grafis Fitur (Feature Graphic):** `play_store_assets/feature_graphic_1024x500.png` (1024x500 px)
3. **Screenshot:** Ambil 2–4 tangkapan layar langsung dari HP Anda saat membuka halaman Home, Kompres, dan Pasfoto.

---

### 5. Detail Keystore Rilis Resmi

* **Lokasi File:** `scanflow-release-key.jks` (di root proyek)
* **Keystore Alias:** `scanflow`
* **Keystore Password:** `ScanFlow2024Pass!`
* **Key Password:** `ScanFlow2024Pass!`
* **Masa Berlaku:** 10.000 Hari (~27 Tahun hingga 2054)
* **File AAB Siap Upload:** `app/build/outputs/bundle/release/app-release.aab`

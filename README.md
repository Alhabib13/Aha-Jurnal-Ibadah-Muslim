# Aha : Jurnal Ibadah Muslim

Aplikasi Android untuk mencatat ibadah harian: sholat, puasa, dzikir, dan tilawah. Dibuat sebagai bagian dari skripsi Teknik Informatika oleh **Alhabib Husein Alaydrus**.

- Google Play: `com.Islamic.Aha` (versi produksi 1.1.1; versi 1.2.0 sedang dikembangkan)
- Kebijakan privasi: https://aha-jurnal-ibadah-muslim.web.app/privacy-policy.html
- Syarat & ketentuan: https://aha-jurnal-ibadah-muslim.web.app/terms-of-service.html

## Fitur

- **Sholat** — sholat fardhu dengan jadwal berdasarkan lokasi (Aladhan API), sholat sunnah, dan tarawih saat Ramadan.
- **Puasa** — puasa wajib Ramadan dan puasa sunnah yang ditambahkan pengguna.
- **Dzikir** — daftar dzikir pilihan pengguna dari katalog (pagi, petang, setelah sholat, sebelum tidur, istighfar, sholawat, dan lain-lain), dicatat per sesi.
- **Tilawah** — sesi bacaan harian, hafalan, dan murajaah berdasarkan rentang ayat, ditambah pencatatan Iqro/metode sejenis.
- **Mode Cuti** — ibadah yang sedang tidak wajib tidak dihitung dan tidak dinilai.
- **Statistik** — heatmap, streak, dan progres harian tanpa skor atau peringkat.
- **Pengingat** — notifikasi waktu sholat dan habit sunnah.
- **Mode tamu & sinkronisasi** — bisa dipakai tanpa akun; bila login, data disinkronkan ke cloud.

## Teknologi

- Kotlin, Jetpack Compose (Material 3), MVVM
- Hilt, Room (migrasi manual), WorkManager
- Firebase: Authentication, Cloud Firestore, Crashlytics, Hosting
- Firestore Security Rules diuji dengan Firebase Emulator (`firestore-rules-test/`)

## Menjalankan proyek

File berikut **tidak** ada di repo dan harus disiapkan sendiri:

| File | Keterangan |
|---|---|
| `app/google-services.json` | dari Firebase Console project kamu |
| `local.properties` | lokasi Android SDK (`sdk.dir=...`) |
| `keystore.properties` | hanya untuk build release; salin dari `keystore.properties.template` |

Build debug:

```bash
./gradlew assembleDebug
```

Tes:

```bash
./gradlew testDebugUnitTest            # tes unit
./gradlew connectedDebugAndroidTest    # tes instrumentasi (emulator/HP)
cd firestore-rules-test && npm test    # tes Firestore Security Rules
```

## Data penelitian

Mulai versi 1.2.0, catatan ibadah (jenis ibadah, tanggal, jam pencatatan) dikumpulkan untuk penelitian skripsi setelah pengguna menyetujui pemberitahuan di aplikasi. Data tidak dijual dan tidak dipakai untuk iklan. Rinciannya ada di kebijakan privasi.

## Atribusi

Data posisi ayat Al-Qur'an (jumlah ayat, halaman, juz) berasal dari [Tanzil Project](https://tanzil.net) (CC BY 3.0), dibangkitkan dengan `tools/gen_quran_data.py`.

## Kontak

- Email: alhabib.husein13@gmail.com
- LinkedIn: [Alhabib Husein Alaydrus](https://www.linkedin.com/in/alhabib-husein-alaydrus-669910291/)

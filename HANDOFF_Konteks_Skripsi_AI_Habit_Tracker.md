# KONTEKS PROYEK SKRIPSI — AI Habit Tracker Ibadah (Hybrid AI)

> **Cara pakai:** Copy-paste seluruh isi file ini sebagai pesan PERTAMA ke AI baru, lalu lanjutkan dengan pertanyaan/permintaan kamu. Ini supaya AI baru langsung paham konteks tanpa perlu dijelaskan ulang dari nol.
>
> Terakhir diperbarui: 14 Oktober 2026 (judul, arsitektur, dan status fitur ditulis ulang; versi lama "Predictive Delivery Notifikasi" sudah TIDAK dipakai).

---

## 1. IDENTITAS PROYEK

**Judul Skripsi (yang dipakai sekarang):**
"Penerapan Hybrid AI untuk Profiling Pengguna dan Notifikasi Pintar pada Aplikasi Habit Tracker Ibadah"

> Judul lama "… untuk Predictive Delivery Notifikasi …" sudah tidak dipakai. Jangan dijadikan acuan.

**Jenis Penelitian:** Research and Development (R&D) dengan metode pengembangan **SDLC Agile**.
Metode **Prototype TIDAK dipakai**, karena panduan KTI prodi menandainya khusus untuk penelitian multimedia atau game.

**Status Aplikasi:**
- Google Play: `com.Islamic.Aha`, versi **1.1.1** yang sedang tayang (belum mengumpulkan data penelitian).
- Versi **1.2.0** (pengumpulan data penelitian + dzikir + tilawah + iqro) ada di branch `feature/pengumpulan-data`, **belum dirilis**.
- Repo: https://github.com/Alhabib13/Aha-Jurnal-Ibadah-Muslim

**Aplikasi Referensi/Inspirasi:** "Muslim Tracker: Pray & Fasting" (Play Store, com.deenindietech.sunnahtracker).

---

## 2. RUMUSAN MASALAH

Rumusan masalah yang sah ada di **BAB I milik penulis**, bukan di file ini. Jangan menyusun ulang atau mengutip rumusan masalah dari file ini.

> Daftar 4 poin versi lama (predictive delivery, tone notifikasi, Actionable Notification + Widget untuk App Fatigue) sudah **usang dan dihapus**.

---

## 3. ARSITEKTUR HYBRID AI (2 lapis)

1. **Forward Chaining — menyiapkan fakta.** Aturan berbasis pengetahuan (divalidasi pakar) mengolah catatan ibadah mentah menjadi fakta yang sah, misalnya membedakan "tidak dikerjakan" dari "udzur" saat Mode Cuti.
2. **Random Forest — mengklasifikasi profil kebiasaan.** Fakta dari lapis 1 dipakai sebagai fitur untuk mengklasifikasi profil kebiasaan ibadah pengguna.

**Label Random Forest diturunkan dari riwayat ibadah**, bukan dari respons notifikasi. Task-nya **klasifikasi** (data berlabel), bukan clustering.

**Yang BUKAN lagi kebutuhan skripsi (jangan disarankan):**
- prediksi gaya bahasa (tone) notifikasi, atau varian pesan notifikasi untuk dicatat
- Widget dan Actionable Notification untuk mengurangi App Fatigue
- Exponential Decay Scoring (dulu "Layer 3")
- pencatatan respons notifikasi sebagai label model

**Tahap sekarang:** hanya **pengumpulan data** (koleksi Firestore `logs`). Belum ada model AI dan belum ada Cloud Functions (Firebase paket Spark).

---

## 4. FITUR APLIKASI (KONDISI NYATA — per commit `c32843d`, 14 Okt 2026)

1. **Sholat** — sholat fardhu dengan jadwal berdasarkan lokasi (Aladhan API), sholat sunnah pilihan pengguna, tarawih saat Ramadan. **SUDAH ADA.**
2. **Puasa** — puasa wajib Ramadan (otomatis saat Ramadan) + puasa sunnah pilihan pengguna. **SUDAH ADA.**
3. **Fitur berbasis gender** — **SUDAH ADA:**
   - Wanita → **Mode Cuti**: sholat & puasa disembunyikan dan tidak dihitung (tidak dinilai gagal). Cakupannya diatur di `ModeCutiPolicy` (dzikir, tilawah, iqro tetap aktif — MENUNGGU VALIDASI PAKAR). Penulisan status `udzur` ke log (Tahap 5) **belum** dibangun.
   - Laki-laki → setiap Jumat, Dzuhur diganti pengingat Sholat Jumat.
4. **Tilawah** — **SUDAH DIBANGUN.** Sesi bacaan harian / hafalan / murajaah berbasis rentang ayat (data posisi dari Tanzil, CC BY 3.0), penanda posisi terakhir, target halaman harian (bacaan harian).
5. **Iqro** (dan Qiroati/Tilawati/UMMI) — **SUDAH DIBANGUN.** Catat jilid (1–10) dan halaman terakhir.
6. **Dzikir** — **SUDAH DIBANGUN (mode centang).** Katalog 9 dzikir (pagi, petang, setelah sholat, sebelum tidur, istighfar, sholawat, tahlil, asmaul husna, lainnya), ditambahkan pengguna dari Add Habit, dicatat per sesi. Mode "layar" (penghitung + teks bacaan) = Tahap 3, menunggu buku rujukan; teks bacaan TIDAK boleh dikarang.
7. **Qadha** — `qadha_sholat` sudah ada sebagai jenis di skema data; puasa pengganti Ramadan dicatat sebagai puasa/qadha. Aturan kapan qadha boleh/tidak (mis. hari Tasyrik/Idul Fitri) **belum** dibangun.
8. **Pengumpulan data penelitian** — koleksi `logs` (18 kolom tetap), pemberitahuan penelitian sebelum data pertama ditulis, sinkron lewat WorkManager. Ikut v1.2.0, belum dirilis.

Detail teknis skema & keputusan: `FITUR_DZIKIR_README.md`, `PLAY_DATA_SAFETY_v1.2.0.md`, `firestore.rules`, `app/src/main/java/com/islami/Aha/data/research/ResearchSchema.kt`.

---

## 5. STATUS PENGUMPULAN JURNAL (Target: 24 jurnal, 6 kategori x 4 jurnal)

> Kategori jurnal disusun untuk judul lama. Kategori **Exponential Decay** tidak dipakai lagi; kategori **Notifikasi/UX** dan **Digital Fatigue** perlu ditinjau ulang terhadap judul baru (profiling pengguna) sebelum dicari tambahannya.

**Aturan dari dosen pembimbing:**
- Jurnal WAJIB terbit dalam 3 tahun terakhir (2023-2026)
- DILARANG dari penerbit STAIN dan Inotek
- Wajib SINTA atau internasional
- Excel kolom: Judul, Penulis&Tahun, Penerbit/Nama Jurnal, Akreditasi, Status 3 Tahun, Komponen Skripsi Terkait, Selaras (Mendukung), Tolak Belakang (Beda/Kontras), Link

### ✅ Forward Chaining (4/4 — SELESAI)
1. Implementation of FC to Identify Istihadhah Time (Android) — Cirebon dkk, 2024, JEAS Univ. NU Cirebon — https://journal.unucirebon.ac.id/index.php/jeas/article/view/846
2. Sistem Pakar Diagnosis Penyakit Kucing (FC + Naive Bayes) — Mahasiswa ITN Malang, 2025, JATI Sinta 4 — https://ejournal.itn.ac.id/jati/article/view/13823
3. ORF-XAI (FC + Random Forest, internasional) — 2025, Springer Nature — https://link.springer.com/chapter/10.1007/978-3-032-19318-6_15
4. Percepatan Diagnosis Batuk (FC + Certainty Factor) — Sekti dkk, 2024, JTIK Univ. M. Husni Thamrin, Sinta 4 — https://doi.org/10.37012/jtik.v10i2.2252

### 🟡 Random Forest (2/4 — butuh 2 lagi)
1. Real-Time ML Personalized Notification Qur'an Tahsin/Tahfiz — Tim Sisfokom, 2026, Sinta & Scopus — https://jurnal.atmaluhur.ac.id/index.php/sisfokom/article/view/2654
2. Perbandingan RF vs Naive Bayes (Studi Nutrisi) — Wijaya, 2025, REMIK — https://jurnal.polgan.ac.id/index.php/remik/article/view/14652

### ⛔ Exponential Decay/Konsistensi — TIDAK DIPAKAI LAGI (judul berubah)
1. ~~Model Kurva Lupa Ebbinghaus (λ=0.30) — Husna dkk, 2025, ATCSJ~~

### 🟡 Notifikasi/UX (1/4 — tinjau ulang relevansinya)
1. Sistem Notifikasi Real-Time Firebase Cloud Messaging — Tim UNIKA St. Thomas, 2024 — https://ejournal.ust.ac.id/index.php/JTIUST/article/view/4437

### 🟡 Aplikasi Ibadah Sejenis (2/4 — butuh 2 lagi)
1. Sistem Monitoring Mutaba'ah (Agile XP, SUS) — Rizki dkk, 2024, JIPI Sinta 3 — https://jurnal.stkippgritulungagung.ac.id/index.php/jipi/article/view/4326
2. Aplikasi Mobile Muhasabah (Kotlin + Jetpack Compose) — Hilmi & Rahmayu, 2026, JUSSI Sinta 5 — https://ejurnal.seminar-id.com/index.php/jussi/article/view/9841

### 🟡 Digital Fatigue/Gen Z (1/4 — tinjau ulang relevansinya)
1. Analisis Social Media Fatigue Mahasiswa — Zulvi dkk, 2025, JKP (IICET) Sinta 2 — https://jurnal.konselingindonesia.com/index.php/jkp/article/view/1390

### ❌ Jurnal yang SUDAH DIGANTI (jangan dipakai lagi)
- JEBE Hukum Darah Kewanitaan (2022, terlalu lama)
- JSIT Stunting FC+NB (2022, terlalu lama)
- J-PTIIK Push Notification (2017, terlalu lama)
- UAD Mutaba'ah (2019, terlalu lama)
- Jurnal Studi Pemuda UGM (artikel spesifik tidak ketemu yang relevan & baru)
- CogITo App Store Rating RF (domain tidak relevan/terlalu jauh)

---

## 6. FILE YANG SUDAH DIBUAT

> File-file ini disusun saat judul lama masih dipakai. Periksa dulu sebelum dikutip — bagian rumusan masalah, tone notifikasi, dan decay di dalamnya sudah usang.

1. `Ringkasan_Skripsi_AI_Habit_Tracker_v4.docx` — ringkasan + cheat sheet konsul (di repo hanya ada v3)
2. `Formulir_Pendaftaran_Skripsi_AI_Habit_Tracker.docx` — formulir pendaftaran (berisi rumusan masalah versi lama)
3. `Flowchart_Sistem_AI_Habit_Tracker.docx` — flowchart alur sistem
4. `Matriks_Jurnal_Skripsi.xlsx` — tracker jurnal (perlu disesuaikan dengan Bagian 5)

---

## 7. TUGAS YANG SEDANG DIKERJAKAN (belum selesai)

**A. Kuesioner untuk siswa SMK/pesantren:**
- A: Screening & Demografi
- B: Kebiasaan Ibadah Saat Ini
- C: Preferensi Waktu Notifikasi — **tinjau ulang:** label Random Forest kini diturunkan dari riwayat ibadah, bukan dari kuesioner
- D: Kebiasaan Digital (bagian Fatigue tinjau ulang terhadap judul baru)

> Bagian "Preferensi Gaya Bahasa/Tone Notifikasi" dihapus — bukan kebutuhan skripsi lagi.

**B. Form Validasi Rules Forward Chaining untuk pakar/ustaz** (disesuaikan ke fitur ASLI di Bagian 4):
- A: Identitas & Kompetensi Pakar
- B: Instruksi & Konteks
- C: Daftar Rules per fitur nyata — Sholat Wajib, Sholat Sunnah, Puasa Sunnah, Puasa Ramadan, **Mode Cuti (paling sensitif secara fiqih; termasuk cakupan dzikir/tilawah saat cuti)**, Reminder Jumat, Qadha, Tilawah, Dzikir (waktu default tiap dzikir, acuan Subuh/Ashar)
- D: Pertanyaan terbuka (rules yang terlewat)
- E: Persetujuan akhir

---

## 8. LANGKAH SELANJUTNYA

1. Rilis v1.2.0 supaya data penelitian mulai terkumpul (tes instrumentasi → merge `feature/pengumpulan-data` ke `main` → build AAB → Data Safety → upload).
2. Tahap penelitian berikutnya (urutan dari penulis): inferensi status "tidak" (sholat wajib & dzikir berjendela) → Tahap 5 Mode Cuti menulis `udzur` → Tahap 4 `sumber_pencatatan` → Tahap 3 onboarding profil → Tahap 2 Anonymous Auth. Dzikir mode layar menunggu buku rujukan.
3. Tinjau ulang kategori jurnal & kuesioner terhadap judul baru (Bagian 5 dan 7), lalu lengkapi jurnal.
4. Selesaikan Form Validasi Pakar (Bagian 7B) jadi file Word siap pakai.

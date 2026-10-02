# KONTEKS PROYEK SKRIPSI — AI Habit Tracker Ibadah (Hybrid AI)

> **Cara pakai:** Copy-paste seluruh isi file ini sebagai pesan PERTAMA ke AI baru, lalu lanjutkan dengan pertanyaan/permintaan kamu. Ini supaya AI baru langsung paham konteks tanpa perlu dijelaskan ulang dari nol.

---

## 1. IDENTITAS PROYEK

**Judul Skripsi (final, sudah didaftarkan):**
"Penerapan Hybrid Artificial Intelligence (Forward Chaining dan Random Forest) untuk Predictive Delivery Notifikasi pada Aplikasi Habit Tracker Ibadah Berbasis Android"

**Jenis Penelitian:** Kuantitatif (Research and Development / R&D, metode SDLC Agile)

**Status Aplikasi:** Sudah masuk **closed testing di Google Play Store, hari ke-10** (bukan cuma konsep, sudah dikembangkan nyata).

**Aplikasi Referensi/Inspirasi:** "Muslim Tracker: Pray & Fasting" (Play Store, com.deenindietech.sunnahtracker) — fitur andalan mereka: Prayer Tracking, Fasting options, Personalized Reminders, Progress Statistics, dan fitur baru "Quran Tracker" (Daily Reading, Memorization, Review, Statistics, Kalender Hijriah).

---

## 2. RUMUSAN MASALAH (4 poin — jumlah ini WAJIB sama dengan jumlah Tujuan Penelitian nanti)

1. Bagaimana merancang dan menerapkan sistem Hybrid AI (Forward Chaining dan Random Forest) untuk melakukan predictive delivery notifikasi pada aplikasi Habit Tracker ibadah berbasis Android?
2. Bagaimana perbandingan performa algoritma Random Forest dan Naive Bayes (Accuracy, Precision, Recall, F1-Score) dalam memprediksi waktu dan gaya bahasa notifikasi optimal?
3. Bagaimana pendekatan frictionless (Actionable Notification + Widget) mengurangi App Fatigue?
4. Bagaimana tingkat usability aplikasi diukur menggunakan System Usability Scale (SUS)?

---

## 3. ARSITEKTUR HYBRID AI (3 Layer)

- **Layer 1 — Forward Chaining:** Gate/filter validasi — cek apakah aktivitas ibadah user memenuhi syarat sah (valid/tidak valid).
- **Layer 2 — Random Forest:** Prediksi waktu optimal & gaya bahasa (tone) notifikasi, menggunakan data historis + hasil validasi Layer 1 sebagai fitur.
- **Layer 3 — Exponential Decay Scoring:** Skor konsistensi ibadah, formula: `Score_t = Score_0 × e^(-λt)`. Nilai λ awal disarankan diambil dari studi forgetting curve (λ=0.30, lihat jurnal ATCSJ di bagian 5), lalu dikalibrasi ulang pakai data user asli.

**Task Machine Learning ini bersifat KLASIFIKASI, bukan clustering** — karena data berlabel, dibandingkan dengan Naive Bayes (algoritma klasifikasi murni), dan pakai metrik Accuracy/Precision/Recall/F1 (bukan Silhouette Score dsb).

---

## 4. FITUR APLIKASI (KONDISI NYATA SAAT INI)

1. **Sholat** = Sholat Wajib + Sholat Sunnah
2. **Puasa** = Puasa Sunnah + Puasa Wajib Ramadan (event-based, muncul otomatis saat masuk bulan Ramadan)
3. **Fitur berbasis gender:**
   - Wanita → tombol **"Mode Cuti"** (untuk masa haid/berhalangan) — PENTING: saat mode ini aktif, sholat/puasa yang tidak dikerjakan harus dicap **"udzur syar'i"**, BUKAN dihitung gagal/bolong.
   - Laki-laki → setiap hari Jumat, notifikasi Dzuhur otomatis diganti jadi reminder Sholat Jumat.
4. **Tilawah** — BELUM DIBANGUN, masih bingung strukturnya. Inspirasi dari Muslim Tracker: Daily Reading (log halaman/ayat), Memorization, Review/Muroja'ah, Statistics, Kalender Hijriah.
5. **Dzikir** — BELUM DIBANGUN. Inspirasi: checklist dzikir pagi (Subuh–terbit matahari) & petang (Ashar–Maghrib), opsional tasbih counter digital.
6. **Sholat & Puasa Pengganti (Qadha)** — ide dari aplikasi lain. Perlu rule soal kapan qadha boleh/tidak boleh dikerjakan (misal: tidak boleh puasa qadha di hari Tasyrik/Idul Fitri).

---

## 5. STATUS PENGUMPULAN JURNAL (Target: 24 jurnal, 6 kategori x 4 jurnal)

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

### 🟡 Exponential Decay/Konsistensi (1/4 — butuh 3 lagi)
1. Model Kurva Lupa Ebbinghaus (λ=0.30) — Husna dkk, 2025, ATCSJ Sinta 4 — https://journal2.unusa.ac.id/index.php/ATCSJ/article/view/8491

### 🟡 Notifikasi/UX (1/4 — butuh 3 lagi)
1. Sistem Notifikasi Real-Time Firebase Cloud Messaging — Tim UNIKA St. Thomas, 2024 — https://ejournal.ust.ac.id/index.php/JTIUST/article/view/4437

### 🟡 Aplikasi Ibadah Sejenis (2/4 — butuh 2 lagi)
1. Sistem Monitoring Mutaba'ah (Agile XP, SUS) — Rizki dkk, 2024, JIPI Sinta 3 — https://jurnal.stkippgritulungagung.ac.id/index.php/jipi/article/view/4326
2. Aplikasi Mobile Muhasabah (Kotlin + Jetpack Compose) — Hilmi & Rahmayu, 2026, JUSSI Sinta 5 — https://ejurnal.seminar-id.com/index.php/jussi/article/view/9841

### 🟡 Digital Fatigue/Gen Z (1/4 — butuh 3 lagi)
1. Analisis Social Media Fatigue Mahasiswa — Zulvi dkk, 2025, JKP (IICET) Sinta 2 — https://jurnal.konselingindonesia.com/index.php/jkp/article/view/1390

**Total terkumpul: 11 dari 24 jurnal.**

### ❌ Jurnal yang SUDAH DIGANTI (jangan dipakai lagi)
- JEBE Hukum Darah Kewanitaan (2022, terlalu lama)
- JSIT Stunting FC+NB (2022, terlalu lama)
- J-PTIIK Push Notification (2017, terlalu lama)
- UAD Mutaba'ah (2019, terlalu lama)
- Jurnal Studi Pemuda UGM (artikel spesifik tidak ketemu yang relevan & baru)
- CogITo App Store Rating RF (domain tidak relevan/terlalu jauh)

---

## 6. FILE YANG SUDAH DIBUAT

1. `Ringkasan_Skripsi_AI_Habit_Tracker_v4.docx` — ringkasan lengkap + cheat sheet konsul
2. `Formulir_Pendaftaran_Skripsi_AI_Habit_Tracker.docx` — rumusan masalah & rencana tahapan penyelesaian masalah
3. `Flowchart_Sistem_AI_Habit_Tracker.docx` — flowchart lengkap alur sistem + tabel penjelasan
4. `Matriks_Jurnal_Skripsi.xlsx` — tracker 24 jurnal (perlu diupdate manual dengan data di Bagian 5 di atas)

---

## 7. TUGAS YANG SEDANG DIKERJAKAN (belum selesai)

**A. Kuesioner untuk siswa SMK/pesantren** (5 bagian):
- A: Screening & Demografi
- B: Kebiasaan Ibadah Saat Ini
- C: Preferensi Waktu Notifikasi (data latih Random Forest)
- D: Preferensi Gaya Bahasa/Tone Notifikasi
- E: Kebiasaan Digital & Fatigue

**B. Form Validasi Rules Forward Chaining untuk pakar/ustaz** (5 bagian, HARUS disesuaikan ke fitur ASLI di Bagian 4, bukan contoh generik):
- A: Identitas & Kompetensi Pakar
- B: Instruksi & Konteks
- C: Daftar Rules per fitur nyata — Sholat Wajib, Sholat Sunnah, Puasa Sunnah, Puasa Ramadan, **Mode Cuti (paling sensitif secara fiqih)**, Reminder Jumat, Qadha, Tilawah, Dzikir
- D: Pertanyaan terbuka (rules yang terlewat)
- E: Persetujuan akhir

---

## 8. LANGKAH SELANJUTNYA YANG PERLU DILANJUTKAN

1. Cari 13 jurnal tambahan untuk genap 24 (prioritas: Decay & Notifikasi/UX yang masih paling kosong)
2. Selesaikan draf lengkap Kuesioner (Bagian 7A) dan Form Validasi Pakar (Bagian 7B) jadi file Word siap pakai
3. Tentukan detail final fitur Tilawah & Dzikir yang masih belum dibangun
4. Update file Excel `Matriks_Jurnal_Skripsi.xlsx` dengan seluruh data di Bagian 5

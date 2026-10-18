# Aha : Jurnal Ibadah Muslim

Aplikasi Android (Kotlin, Jetpack Compose, Hilt, Room, Firebase) untuk mencatat ibadah harian,
sekaligus alat pengumpulan data skripsi *Penerapan Hybrid AI untuk Profiling Pengguna dan
Notifikasi Pintar pada Aplikasi Habit Tracker Ibadah*.

Dokumen terkait: `HANDOFF_Konteks_Skripsi_AI_Habit_Tracker.md` (konteks skripsi),
`FITUR_DZIKIR_README.md` (fitur dzikir), `PLAY_DATA_SAFETY_v1.2.0.md` (Data Safety Play Console).

## Keputusan yang dikunci, pengerjaannya ditunda

### Anonymous Auth (diputuskan 2026-10-18)

**Status: DITUNDA seluruhnya sampai v1.2.0 tayang di Play Store dan data mulai mengalir.**
Penyedia "Anonymous" di Firebase Console juga JANGAN diaktifkan sebelum aplikasi memakainya.

Alasan: responden penelitian direkrut langsung dan bisa membuat akun, jadi Anonymous Auth tidak
melayani mereka. Sesuai keputusan 2026-10-09, Anonymous Auth sengaja ditaruh paling akhir supaya
tidak dibundel dengan rilis fitur (jalur pergantian UID berisiko menghapus data lokal).

Saat dikerjakan nanti, aturannya:

1. **Tamu mendaftar akun baru** → tautkan akun anonim ke akun baru (`linkWithCredential`).
   UID tetap, semua data ikut.
2. **Tamu login ke akun yang SUDAH ADA** → jangan mengganti data lokal diam-diam. Tampilkan
   peringatan SEBELUM login yang menyebutkan habit apa saja yang akan hilang dari HP, dan minta
   pengguna menegaskan. Data selama menjadi tamu tetap di dataset dengan UID anonimnya.
3. **Setelah logout** → aplikasi BERHENTI mengirim data penelitian sampai pengguna login lagi.
   JANGAN membuat akun anonim baru secara otomatis: satu orang dengan dua UID mencemari dataset
   dan tidak bisa dibersihkan belakangan, sedangkan data yang tidak terkirim hanya hilang.
   Prinsip proyek: **data hilang lebih baik daripada data salah.**

Yang juga harus ikut saat dikerjakan: periksa semua tempat yang menganggap "ada `currentUser`"
sama dengan "sudah login" (Profil, sinkron habit, hapus akun, Splash), serta perbarui kebijakan
privasi dan Data Safety.

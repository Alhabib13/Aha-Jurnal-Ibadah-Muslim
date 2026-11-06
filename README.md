# Aha : Jurnal Ibadah Muslim

Aplikasi Android (Kotlin, Jetpack Compose, Hilt, Room, Firebase) untuk mencatat ibadah harian,
sekaligus alat pengumpulan data skripsi *Penerapan Hybrid AI untuk Profiling Pengguna dan
Notifikasi Pintar pada Aplikasi Habit Tracker Ibadah*.

Dokumen terkait: `HANDOFF_Konteks_Skripsi_AI_Habit_Tracker.md` (konteks skripsi),
`FITUR_DZIKIR_README.md` (fitur dzikir), `PLAY_DATA_SAFETY_v1.2.0.md` (Data Safety Play Console).

## Keputusan yang dikunci

### Anonymous Auth (diputuskan 2026-10-18, dikerjakan 2026-11-06 untuk v1.2.0)

Semula ditunda sampai v1.2.0 tayang; pemilik proyek memutuskan memasukkannya ke v1.2.0.
Penyedia "Anonymous" di Firebase Console harus diaktifkan sebelum rilis (dikerjakan pemilik
proyek). Selama belum aktif, aplikasi tetap berjalan sebagai tamu tanpa error.

1. **Masuk anonim otomatis HANYA untuk HP yang belum pernah login akun sungguhan**
   (`AuthRepository.masukAnonimBilaPerlu`). Catatan tamu sebelumnya diadopsi UID anonim itu.
2. **Tamu mendaftar akun baru** (email atau Google) → akun anonim ditautkan
   (`linkWithCredential`). UID tetap, semua data ikut.
3. **Tamu login ke akun yang SUDAH ADA** → peringatan SEBELUM login yang menyebutkan habit yang
   akan hilang dari HP (`DialogPindahAkun`), lalu data tamu yang tertunda dikirim dulu. Data
   selama menjadi tamu tetap di dataset dengan UID anonimnya.
4. **Setelah logout** → TIDAK dibuatkan akun anonim baru; data penelitian berhenti terkirim
   sampai login lagi. Satu orang dengan dua UID mencemari dataset dan tidak bisa dibersihkan,
   sedangkan data yang tidak terkirim hanya hilang. Prinsip proyek: **data hilang lebih baik
   daripada data salah.**
5. Tamu anonim BUKAN "login" untuk tampilan (`AuthRepository.isLoggedIn` = akun sungguhan);
   Profil/Pengaturan tetap mode tamu.
6. `users/{uid}` wajib berisi `disetujui_pada` (waktu server) sejak dibuat: persetujuan
   penelitian tercatat di dataset dan ditolak server bila tidak ada.

Masih harus dikerjakan sebelum rilis: perbarui kebijakan privasi (dua salinan) dan Data Safety
(data kini terkirim tanpa login, dengan ID acak).

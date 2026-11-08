# Data Safety (Play Console) — Aha v1.2.0 (versionCode 4)

Diperbarui 8 November 2026, disusun dari kode di branch `feature/pengumpulan-data`
(setelah commit 6ac406c). Dicek terhadap: `AuthRepository` (akun tanpa login, `meta/profile`,
penghapusan), `ResearchLogRepository` (`logs`, `users/{uid}`), `CompletionSyncRepository`
(`habit_completions`), `UserHabitRepository` (`sunnah_habits`), `SinkronJadwalPengingat` +
`NotifikasiServerModule` (`meta/notifikasi`, `jadwal_pengingat`), `PrayerTimeApiService` (Aladhan),
`AhaApplication` (Crashlytics), dan `firestore.rules`.

## Pertanyaan umum

| Pertanyaan | Jawaban | Dasar |
|---|---|---|
| Apakah aplikasi mengumpulkan/membagikan data pengguna? | **Ya** | lihat tabel di bawah |
| Semua data dienkripsi saat transit? | **Ya** | Firebase dan Aladhan (`https://api.aladhan.com`) memakai HTTPS |
| Pengguna bisa meminta penghapusan data? | **Ya** | Di aplikasi: akun email/Google lewat **Pengaturan › Akun & Keamanan › Hapus akun permanen**; tanpa login lewat **Pengaturan › Privasi & Data › Hapus semua data dan akun**. Juga lewat email (paling lambat 30 hari). |
| URL penghapusan akun (wajib) | `https://aha-jurnal-ibadah-muslim.web.app/hapus-akun.html` | `docs/hapus-akun.html` (Firebase Hosting) |
| Metode pembuatan akun | Email + kata sandi, Google Sign-In, dan akun tanpa login (Firebase Anonymous Auth, dibuat otomatis) | `AuthRepository.masukAnonimBilaPerlu` |

## Tipe data

| Kategori Play | Tipe data | Dikumpulkan | Dibagikan | Wajib/opsional | Tujuan | Catatan |
|---|---|---|---|---|---|---|
| Info pribadi | Nama | Ya | Tidak | Opsional (hanya jika punya akun) | Fungsi aplikasi, Pengelolaan akun | `meta/profile.name` |
| Info pribadi | Alamat email | Ya | Tidak | Opsional (hanya jika punya akun) | Pengelolaan akun | Firebase Auth + `meta/profile.email` |
| Info pribadi | **ID pengguna** | Ya | Tidak | **Wajib** | Fungsi aplikasi, Analitik, Pengelolaan akun | Firebase UID, **termasuk ID acak akun tanpa login** yang dibuat otomatis saat aplikasi pertama dibuka (tanpa nama/email). Ditambah **kode responden** (`users/{uid}.kode_responden`, opsional, hanya bila diisi di layar persetujuan penelitian). |
| Info pribadi | **Keyakinan agama atau politik** | Ya | Tidak | **Wajib** | Fungsi aplikasi, Analitik | Tidak ada field "agama", tetapi catatan ibadah (sholat, puasa, dzikir, tilawah) jelas mengungkap praktik keagamaan. Dicantumkan agar aman. |
| Info pribadi | Info pribadi lainnya | Ya | Tidak | Opsional | Fungsi aplikasi, Analitik | Jenis kelamin, rentang usia, kesibukan utama (bisa "Atur nanti"); merek HP (`manufaktur`) untuk peserta penelitian. |
| Kesehatan & kebugaran | **Info kesehatan** | Ya | Tidak | Opsional | Fungsi aplikasi, Analitik | Mode Cuti: `haidhDates`, `isHaidhMode` di `meta/profile` = data siklus haid. Status `udzur` di `logs` diturunkan dari sini (hanya bagi peserta penelitian). |
| Foto & video | Foto | Ya | Tidak | Opsional | Pengelolaan akun | Foto profil (`avatarBase64` di `meta/profile`) bila diunggah |
| Lokasi | **Lokasi persis** (ACCESS_FINE_LOCATION) | Ya (dikirim keluar perangkat) | **Ya**, ke Aladhan API | Opsional | Fungsi aplikasi | Koordinat dikirim ke Aladhan hanya untuk menghitung jadwal sholat; tidak disimpan di server kami. Centang **"diproses sementara" (ephemeral)**. |
| Aktivitas aplikasi | **Interaksi aplikasi** | Ya | Tidak | Opsional | Analitik | Jam pencatatan dan sumber pencatatan (`dari_notifikasi` / `buka_aplikasi`), selisih menit dari adzan, zona waktu. Hanya di `logs`, hanya bila pengguna **menyetujui** penelitian. |
| Aktivitas aplikasi | **Tindakan lain dalam aplikasi** (catatan ibadah) | Ya | Tidak | **Wajib**\* | Fungsi aplikasi, Analitik | Centang ibadah harian (`habit_completions`) selalu dicadangkan ke server dengan ID pengguna (termasuk ID acak tanpa login). Catatan penelitian lengkap (`logs`: status, tilawah, iqro, dzikir) hanya dikirim bila pengguna menyetujui. |
| Aktivitas aplikasi | Konten buatan pengguna lainnya | Ya | Tidak | **Wajib**\* | Fungsi aplikasi | Daftar habit sunnah (`sunnah_habits`), habit dzikir (`dzikir_habits`), penanda & target tilawah. Dicadangkan otomatis. |
| Info & performa aplikasi | Log error | Ya | Tidak | Wajib | Analitik | Crashlytics (aktif di build rilis) |
| Info & performa aplikasi | Diagnostik | Ya | Tidak | Wajib | Analitik | Crashlytics |
| ID perangkat atau lainnya | ID perangkat atau lainnya | Ya | Tidak | Wajib | Fungsi aplikasi, Analitik | **Token FCM** (`meta/notifikasi.fcm_token`) untuk mengirim pengingat; Firebase Installation ID (Crashlytics). Token dihapus dari server saat logout / hapus akun. |

\* "Wajib" berarti pengguna tidak bisa mematikan pencadangan ini selama memakai aplikasi. Keikutsertaan
penelitian tetap opsional: tanpa persetujuan, `logs` dan `users/{uid}` tidak dikirim.

Juga tersimpan di server tetapi bukan data pribadi baru: jadwal pengingat 7 hari
(`jadwal_pengingat`: waktu kirim, jenis ibadah, ID pengingat; dihitung di HP, tanpa lokasi).

### Siapa "pihak ketiga"
- **Firebase (Google)** adalah penyedia layanan yang memproses data atas nama kami, jadi **bukan** "dibagikan".
- **Aladhan** menerima koordinat lokasi, jadi lokasi persis ditandai **dibagikan**. Bila ingin cukup
  mendeklarasikan "lokasi perkiraan", koordinat harus dibulatkan sebelum dikirim (mis. 2 desimal ≈ 1 km);
  belum dilakukan.

### Tentang tujuan "penelitian"
Play Console tidak punya pilihan "Riset". Pilihan yang paling jujur adalah **Analitik**, ditambah
penjelasan di kebijakan privasi bagian 3. **Jangan** centang "Iklan atau pemasaran" atau
"Personalisasi" (belum ada fitur yang memakai data untuk personalisasi di v1.2.0).

## Yang TIDAK dikumpulkan (jawab "Tidak")
Info keuangan, pesan, kontak, kalender, audio, file/dokumen, histori web, aplikasi terpasang,
ras/etnis, orientasi seksual, alamat, nomor telepon.

## Hal yang perlu diputuskan pemilik aplikasi
1. **Target usia / Families policy.** Pilihan usia mencakup "Di bawah 15 tahun". Jika target audiens
   di Play Console mencakup < 13 tahun, berlaku kebijakan Families. Pastikan konsisten dengan kenyataan.
2. **Etika penelitian.** Untuk partisipan di bawah umur, institusi biasanya meminta persetujuan
   orang tua/wali (aplikasi mencatat `izinWali`). Selaraskan sebelum data dianalisis.

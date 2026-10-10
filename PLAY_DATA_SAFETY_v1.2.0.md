# Data Safety (Play Console) — Aha v1.2.0 (versionCode 4)

Disusun dari kode di branch `feature/pengumpulan-data` (commit setelah 08a2983).
Dicek terhadap: `AndroidManifest.xml`, `AuthRepository` (users/{uid}/meta/profile),
`CompletionSyncRepository`, `ResearchLogRepository` (koleksi `logs`), `UserHabitRepository`,
`PrayerTimeApiService` (Aladhan), Firebase Crashlytics.

## Pertanyaan umum

| Pertanyaan | Jawaban | Dasar |
|---|---|---|
| Apakah aplikasi mengumpulkan/membagikan data pengguna? | **Ya** | lihat tabel di bawah |
| Semua data dienkripsi saat transit? | **Ya** | Firebase & Aladhan memakai HTTPS |
| Pengguna bisa meminta penghapusan data? | **Ya** | In-app: Pengaturan > Keamanan Akun > Hapus Akun Permanen (menghapus habit, riwayat, profil, `logs`, `users/{uid}`, akun Auth). Juga lewat email. |
| URL penghapusan akun (wajib) | URL kebijakan privasi (bagian 7) | `docs/privacy-policy.html` |

## Tipe data

| Kategori Play | Tipe data | Dikumpulkan | Dibagikan | Wajib/opsional | Tujuan | Catatan |
|---|---|---|---|---|---|---|
| Lokasi | **Lokasi persis** (ACCESS_FINE_LOCATION) | Ya (dikirim keluar perangkat) | **Ya** ke Aladhan API | Opsional | Fungsi aplikasi | Hanya untuk jadwal sholat; tidak disimpan di server kami. Diproses sementara (ephemeral). |
| Info pribadi | Nama | Ya | Tidak | Opsional (hanya jika punya akun) | Fungsi aplikasi, Pengelolaan akun | `meta/profile.name` |
| Info pribadi | Alamat email | Ya | Tidak | Opsional (hanya jika punya akun) | Pengelolaan akun | Firebase Auth + `meta/profile.email` |
| Info pribadi | ID pengguna | Ya | Tidak | Opsional (hanya jika punya akun) | Fungsi aplikasi, Analitik, Pengelolaan akun | Firebase UID; kunci data penelitian (`logs.user_id`) |
| Info pribadi | Info pribadi lainnya | Ya | Tidak | Opsional | Fungsi aplikasi, Analitik | Jenis kelamin (`genderProfile`). Mulai Tahap 3: rentang umur, status aktivitas. |
| Kesehatan & kebugaran | **Info kesehatan** | Ya | Tidak | Opsional | Fungsi aplikasi, Analitik | Tanggal & status Mode Cuti (`haidhDates`, `isHaidhMode`) = data siklus haid. Status `udzur` di `logs` diturunkan dari sini (Tahap 5). |
| Foto & video | Foto | Ya | Tidak | Opsional | Fungsi aplikasi | Foto profil (`avatarBase64`) jika pengguna mengunggahnya |
| Aktivitas aplikasi | **Tindakan lain dalam aplikasi** | Ya | Tidak | Opsional* | Fungsi aplikasi, **Analitik** | Catatan ibadah + jam pencatatan (sholat, puasa, dzikir, tilawah halaman, iqro jilid/halaman). Dipakai untuk penelitian (lihat di bawah). |
| Info & performa aplikasi | Log error | Ya | Tidak | Wajib | Analitik | Crashlytics |
| Info & performa aplikasi | Diagnostik | Ya | Tidak | Wajib | Analitik | Crashlytics |
| ID perangkat atau lainnya | ID perangkat atau lainnya | Ya | Tidak | Wajib | Analitik | Firebase Installation ID (Crashlytics) |

\* Catatan ibadah tersimpan di perangkat untuk semua pengguna, tetapi hanya dikirim ke server bila pengguna masuk ke akun **dan** sudah menekan "Mengerti" pada pemberitahuan penelitian.

### Tentang tujuan "penelitian"
Play Console tidak punya pilihan "Riset". Pilihan yang paling jujur adalah **Analitik** ("dipakai untuk memahami cara pengguna menggunakan aplikasi / mengembangkan fitur"), ditambah penjelasan di kebijakan privasi bagian 3. **Jangan** centang "Iklan atau pemasaran" atau "Personalisasi" (belum ada fitur yang memakai data untuk personalisasi di v1.2.0; notifikasi adaptif baru nanti).

## Yang TIDAK dikumpulkan (jawab "Tidak")
Info keuangan, pesan, kontak, kalender, audio, file/dokumen, histori web, aplikasi terpasang, info pribadi sensitif lain (ras, agama sebagai field terpisah, orientasi, dll.).

> Catatan: aplikasi ini aplikasi ibadah, tetapi **tidak** menyimpan field "agama" — keyakinan hanya tersirat dari penggunaan aplikasi. Putuskan sendiri apakah perlu dicantumkan sebagai "Keyakinan agama atau filosofis".

## Hal yang perlu diputuskan pemilik aplikasi
1. **Target usia / Families policy.** Pengguna mencakup anak sekolah (< 18). Jika target audiens di Play Console mencakup < 13 tahun, berlaku kebijakan Families (aturan tambahan, termasuk SDK yang boleh dipakai). Pastikan pengaturan target audiens konsisten dengan kenyataan.
2. **Etika penelitian.** Untuk partisipan di bawah umur, institusi biasanya meminta persetujuan orang tua/wali. Itu urusan protokol penelitian, bukan Play Console, tetapi sebaiknya diselaraskan sebelum data dianalisis.
3. `docs/privacy-policy.html` baru berlaku untuk publik setelah di-push ke repo GitHub Pages.

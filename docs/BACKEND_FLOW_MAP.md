# Backend Flow Map (Aktual dari Kode)

Dokumen ini memetakan alur backend aplikasi dari layer UI -> Repository -> Local DB/Firebase -> fallback/error path.

## 1) Arsitektur Singkat

- UI pakai `ViewModel` (Auth/Home/Profile/Settings/Admin/Shared).
- Data lokal pakai Room `aha_db`:
  - `habits` (default/fardhu)
  - `user_habits` (habit sunnah user)
  - `habit_completion_records` (riwayat selesai)
  - `hadith_contents`, `surah_verses` (konten harian)
- Backend cloud utama:
  - Firebase Auth
  - Firestore
  - Firebase Storage
  - App Check
- Integrasi eksternal non-Firebase:
  - API waktu sholat Aladhan (`https://api.aladhan.com/v1/timings`)

## 2) Konfigurasi Env & App Check

- Flavor `dev`:
  - `APP_ENV = "dev"`
  - `APP_CHECK_MODE = "NONE"`
- Flavor `prod`:
  - `APP_ENV = "prod"`
  - `APP_CHECK_MODE = "PLAY_INTEGRITY"`
- `FORCE_APPCHECK_DEBUG=true` bisa override jadi DEBUG provider.
- Inisialisasi App Check dilakukan saat `AhaApplication.onCreate()`.

Catatan:
- Dokumen lama masih ada yang menyebut `dev -> DEBUG`; kode aktual saat ini `dev -> NONE`.

## 3) Data Path Cloud

Firestore:
- `fardhu_defaults/{docId}` (read publik, write ditolak)
- `app_config/{docId}` (read publik, write admin)
- `hadith_contents/{docId}` (read publik, write admin)
- `surah_verses/{docId}` (read publik, write admin)
- `users/{uid}/sunnah_habits/{habitId}` (read/write owner)
- `users/{uid}/meta/profile` (read/write owner dengan field terbatas)

Storage:
- `users/{uid}/profile/{fileName}` (read/write owner)

## 4) Flow Utama

### A. Login/Register

1. UI (`AuthViewModel`) panggil `AuthRepository.login/register/loginWithGoogleIdToken`.
2. Firebase Auth verifikasi kredensial.
3. Jika sukses:
   - session user disimpan ke `SecurePrefs` (`is_logged_in`, `user_name`, `user_email`, `user_avatar_uri`)
   - sinkronisasi profil cloud:
     - register/google: upsert `users/{uid}/meta/profile`
     - login: tarik profil cloud ke prefs lokal
4. Jika gagal:
   - error dipetakan ke pesan ramah user (`mapFirebaseError`)
   - kasus App Check diberi pesan khusus sesuai mode.

### B. Profil (Avatar)

1. UI (`ProfileViewModel`) -> `AuthRepository.updateAvatar`.
2. Upload ke Storage `users/{uid}/profile/avatar.jpg`.
3. Ambil download URL (dengan retry exponential untuk race object metadata).
4. Simpan URL ke `users/{uid}/meta/profile.avatarUrl` + prefs lokal.
5. Fallback: jika bucket modern gagal object-not-found, coba bucket legacy `*.appspot.com`.

### C. Habit Sunnah (CRUD + Sync)

1. UI (`SunnahHabitSharedViewModel`) menulis dulu ke Room `user_habits`.
2. Setelah local success, repository `syncUpsertToCloud/syncDeleteFromCloud` ke Firestore.
3. `syncFromCloudIfLoggedIn()`:
   - throttle minimal 30 detik
   - fetch `users/{uid}/sunnah_habits`
   - upsert ke local
   - bisa hapus local yang dianggap sudah hilang dari remote (berdasarkan `createdAt <= previousSyncAtMs`)
4. Toggle completion sunnah memakai transaksi Room (`withTransaction`) agar update habit + completion record atomik.

### D. Habit Fardhu Default

1. Saat seed awal Home:
   - coba ambil `fardhu_defaults` dari Firestore
   - validasi harus lengkap 5 waktu + format `HH:mm`
2. Jika cloud tidak valid/gagal, pakai fallback hardcoded lokal.
3. Update waktu sholat harian:
   - ambil lokasi (current/last/cached)
   - fetch API Aladhan
   - update time di table `habits` untuk 5 sholat
   - reschedule reminder yang aktif.

### E. Konten Harian (Hadits/Surah)

1. `DailyIslamicContentRepository` seed default jika tabel kosong.
2. Sync cloud maksimal 1x/jam:
   - fetch `hadith_contents` dan `surah_verses`
   - validasi field wajib
   - replace data lokal via transaksi DAO.

### F. Admin Feature Flag

1. `AdminConfigRepository` pasang snapshot listener ke `app_config/feature_flags`.
2. ViewModel (Home/Profile/Statistic/Notification/Admin) consume `featureConfig` flow.
3. Aksi admin:
   - cek admin via custom claim dulu
   - fallback cek `users/{uid}/meta/profile.role == "admin"`
   - update `puasaWajibRamadanEnabled` ke `app_config/feature_flags`.

### G. Delete Account

1. `AuthRepository.deleteAccount()`:
   - hapus cloud data user (`sunnah_habits` + `meta/profile`)
   - hapus akun Firebase Auth
   - clear session local
2. Jika perlu login ulang, lempar `ReAuthRequiredException`.

## 5) Error Handling & Offline Behavior

- Hampir semua error cloud dicatat ke Crashlytics.
- Banyak operasi cloud `runCatching` agar aplikasi tetap jalan walau sync gagal.
- Home menampilkan offline sync notice saat error jaringan.
- Lokasi punya fallback berlapis:
  - current location -> last location -> cached encrypted location.
- Jika lokasi service OFF:
  - app tetap bisa jalan dengan lokasi terakhir/cached.

## 6) Risiko Teknis yang Masih Ada

1. Sinkronisasi habit sunnah belum pakai versi data (`updatedAt/version`) sehingga conflict resolution masih last-write-wins.
2. Algoritma delete local saat sync cloud berpotensi menghapus data lokal tertentu jika clock/createdAt tidak konsisten.
3. `updateUsername()` di `ProfileViewModel` hanya update local prefs, belum upsert ke Firestore profile.
4. `deleteAccount()` menghapus data cloud dulu lalu hapus akun Auth; jika tahap akhir gagal, data bisa sudah hilang tapi akun masih ada.
5. Prayer API belum punya retry/backoff terstruktur; saat API timeout, hanya fallback notice.
6. Dokumen env App Check belum sepenuhnya sinkron dengan kode aktual.

## 7) Checklist Uji Manual (Backend)

1. Login email/password sukses/gagal (cek mapping error).
2. Google Sign-In di device + emulator.
3. Upload/hapus avatar dan cek Firestore profile + Storage object.
4. Tambah/edit/hapus/toggle habit sunnah saat online, lalu relogin di device lain.
5. Uji offline: tambah habit sunnah lokal, online lagi, pastikan cloud sinkron.
6. Uji seed fardhu:
   - Firestore valid -> dipakai
   - Firestore invalid/offline -> fallback lokal.
7. Uji App Check:
   - `dev` non-enforced
   - `prod` enforced Play Integrity.
8. Uji admin flag dengan akun admin vs non-admin.
9. Uji delete account biasa + jalur re-auth required.

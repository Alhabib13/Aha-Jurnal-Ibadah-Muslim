# Firebase App Check Debug (Android)

Dengan setup flavor baru, `dev` default memakai `APP_CHECK_MODE=DEBUG` supaya login di emulator/device debug tetap bisa saat enforcement App Check aktif.

Jika login Email/Google gagal dengan error `App attestation failed` atau `App Check token is invalid`, lakukan langkah berikut:

1. Jalankan aplikasi dalam build debug.
2. Buka Logcat dan cari tag `DebugAppCheckProvider`.
3. Salin debug secret/token yang muncul di Logcat.
4. Buka Firebase Console -> App Check -> Manage debug tokens.
5. Tambahkan token tadi, lalu jalankan ulang aplikasi.

## Opsional: paksa debug provider untuk build non-debug (hanya testing internal)

Tambahkan properti Gradle:

```properties
FORCE_APPCHECK_DEBUG=true
```

Setelah itu rebuild aplikasi. Jangan aktifkan ini untuk rilis produksi.

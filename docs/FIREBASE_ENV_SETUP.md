# Firebase Environment Setup (`dev` / `prod`)

Project ini sekarang memakai Android flavor:

- `dev`
- `prod`

## Mapping keamanan App Check

- `dev` -> `APP_CHECK_MODE=DEBUG`
- `prod` -> `APP_CHECK_MODE=PLAY_INTEGRITY`

`FORCE_APPCHECK_DEBUG=true` tetap bisa dipakai jika butuh paksa debug provider.

## File Firebase per flavor

Tempatkan file berikut:

- `app/src/dev/google-services.json` -> dari Firebase project DEV/STAGING
- `app/src/prod/google-services.json` -> dari Firebase project PRODUCTION

Untuk mode 1 project Firebase, kedua file boleh identik.

Project sekarang punya validasi otomatis:

- default -> warning kalau file identik
- strict mode -> build gagal kalau file identik

Aktifkan strict mode saat dibutuhkan:

```powershell
.\gradlew.bat :app:compileDevDebugKotlin -PSTRICT_FIREBASE_CONFIG=true
```

## Rekomendasi Firebase Console
1. Project `dev`:
   - App Check enforcement untuk Firestore/Storage/Auth dibuat longgar atau non-enforced untuk mempercepat development.
2. Project `prod`:
   - App Check enforced.
   - Provider Android: Play Integrity.

## Build command

```powershell
.\gradlew.bat :app:assembleDevDebug --no-daemon
.\gradlew.bat :app:assembleProdRelease --no-daemon
```

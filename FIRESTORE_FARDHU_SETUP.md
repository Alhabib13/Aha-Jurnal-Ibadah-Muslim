# Firestore Setup: `fardhu_defaults` (Client Read-Only)

## 1) Deploy Firestore Rules

Project ini sudah punya file rules:
- `firestore.rules`
- `firebase.json`

Deploy:

```bash
firebase use <your-project-id>
firebase deploy --only firestore:rules
```

## 2) Seed Koleksi `fardhu_defaults`

File seed:
- `fardhu_defaults.seed.json`

Script seed (admin/server-side):
- `seed-fardhu-defaults.mjs`

Install dependency sekali:

```bash
npm install firebase-admin
```

Set environment variable:

```bash
export FIREBASE_PROJECT_ID=<your-project-id>
export FIREBASE_SERVICE_ACCOUNT_PATH=/absolute/path/to/service-account.json
```

Windows PowerShell:

```powershell
$env:FIREBASE_PROJECT_ID="<your-project-id>"
$env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\path\to\service-account.json"
```

Run seed:

```bash
node seed-fardhu-defaults.mjs
```

## 3) Struktur Dokumen yang Di-seed

Collection: `fardhu_defaults`

Doc IDs:
- `subuh`
- `dzuhur`
- `ashar`
- `maghrib`
- `isya`

Fields:
- `name` (String)
- `defaultTime` (`HH:mm`)
- `icon` (String)
- `order` (Number)
- `isSystem` (Boolean, true)
- `updatedAt` (ISO String)

## 4) Aturan Akses yang Diterapkan

- Client app: boleh `read` koleksi `fardhu_defaults`
- Client app: tidak boleh `write` koleksi `fardhu_defaults`
- User sunnah data: hanya owner (`request.auth.uid == userId`)

## 5) Seed Koleksi Konten Home (`hadith_contents` dan `surah_verses`)

File seed:
- `hadith_contents.seed.json`
- `surah_verses.seed.json`

Script seed:
- `seed-islamic-content.mjs`

Environment variable (sama seperti seed fardhu):

```powershell
$env:FIREBASE_PROJECT_ID="<your-project-id>"
$env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\path\to\service-account.json"
```

Run:

```bash
node seed-islamic-content.mjs
```

Atau via npm script:

```bash
npm run seed:islamic-content
```

Menjalankan semua seed sekaligus (`fardhu_defaults` + konten Home):

```bash
npm run seed:all
```

Struktur field dokumen:

Collection `hadith_contents`:
- `text` (String)
- `source` (String)
- `updatedAt` (ISO String)

Collection `surah_verses`:
- `surahName` (String)
- `ayahNumber` (Number)
- `translation` (String)
- `updatedAt` (ISO String)

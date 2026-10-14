# Fitur Dzikir — Tahap 2 (UI mode centang)

Status: **mode centang aktif; mode layar ditunda ke Tahap 3** (menunggu buku rujukan).
Kartu dua slot pagi/petang sudah diganti daftar habit dzikir pilihan pengguna.

## Tahap 2 — ringkasan

- **Habit dzikir = konfigurasi**, bukan log: Room `dzikir_habits` (v16) dan Firestore
  `users/{uid}/dzikir_habits/{id}` (`slug`, `mode_pencatatan`, `waktu_dzikir`, `dibuat_pada`,
  opsional `nama_detail_asli` [wajib untuk dzikir_lainnya] dan `target_hitungan`).
  Kode: `data/dzikir/DzikirHabitRepository.kt`. Tidak pernah menulis `logs`/`research_logs`.
- **Add Habit saat Mode Cuti**: hanya kategori yang TETAP_AKTIF di ModeCutiPolicy (Dzikir) yang
  terbuka; Sholat & Puasa Sunnah tetap terkunci.
- **Add Habit → Dzikir**: 9 entri katalog, boleh pilih beberapa sekaligus, dzikir_lainnya boleh
  lebih dari satu asal namanya beda. Mode "layar" tampil tetapi nonaktif ("bacaannya belum
  tersedia"). Target hitungan opsional, kosong secara bawaan, tanpa nilai anjuran.
- **Beranda**: kategori Dzikir selalu tampil. Tanpa habit dzikir, isinya pintu masuk netral
  ("Belum ada dzikir... Tambahkan dzikir") yang membuka Add Habit di kategori Dzikir, bukan kartu
  progres 0/0 (fitur yang tidak terlihat tidak menghasilkan data). Tiap habit punya tombol
  "Catat" (satu sesi; ketukan ganda ≤ 60 detik = sesi yang sama), "Batalkan terakhir"
  (baris dihapus), dan "Hapus" (habit keluar dari daftar, catatan lama tetap).
  `sumber_pencatatan` = `tidak_diketahui`.
- **Rasio "Progress Hari Ini"**: habit dengan ≥ 1 sesi hari ini / semua habit dzikir, termasuk
  yang ber-waktu `bebas` (yang tidak pernah disimpulkan "tidak"). Heatmap: 1 per habit per hari.

## Mode Cuti — MENUNGGU_VALIDASI_PAKAR

Cakupan Mode Cuti adalah peta per `jenis_ibadah` di satu tempat:
`data/research/ModeCutiPolicy.kt` (`ModeCutiPolicy.CAKUPAN`, dipakai lewat `ModeCutiPolicy.aktif()`).

| jenis_ibadah | Bawaan sekarang |
|---|---|
| sholat_wajib, sholat_sunnah, qadha_sholat, puasa | NONAKTIF (perilaku lama) |
| dzikir | **TETAP_AKTIF** |
| tilawah, iqro | TETAP_AKTIF (perilaku v1.2.0) |

Alasan dzikir tetap aktif (sementara, perlu dikonfirmasi pakar): dzikir tidak mensyaratkan suci
dari hadats besar, sehingga menyembunyikannya bisa terbaca sebagai larangan. Selama Mode Cuti,
habit dzikir tetap tampil dan tetap masuk penyebut rasio. Bila pakar memutuskan lain, ubah nilai
`DZIKIR` di peta itu; Beranda, Statistik, dan Profil membacanya dari sana.
Catatan: kode lama sholat/puasa masih memeriksa kategori secara langsung (nilainya sama dengan peta).

## Keputusan desain (final)

1. Dzikir = katalog yang ditambahkan pengguna sendiri (seperti sholat sunnah), bukan daftar wajib.
2. Tiap habit dzikir punya mode pencatatan, disimpan per-habit dan boleh diubah:
   - `layar` — membaca di aplikasi, ada penghitung (`jumlah`, `durasi_detik` terisi).
   - `centang` — dzikir di luar aplikasi; aplikasi hanya tempat mencentang.
3. **Aturan kejujuran data:** pada mode `centang`, `jumlah` dan `durasi_detik` WAJIB null.
   Jangan diisi target — tidak tahu ≠ nol. Ditegakkan di tiga lapis: repository
   (`ResearchLogRepository.recordDikerjakan`), Firestore rules (`validDzikir`), dan tes emulator.
4. Tanpa skor, peringkat, atau bahasa menyalahkan. Mode Cuti tetap berlaku: tidak ada notifikasi
   dzikir dan tidak dihitung "tidak" selama cuti.

## Katalog — `app/src/main/assets/dzikir_katalog.json`

| slug (= `nama_detail`) | nama | waktu_default |
|---|---|---|
| `dzikir_bada_sholat` | Dzikir setelah sholat fardhu | `bada_sholat` |
| `dzikir_pagi` | Dzikir pagi | `pagi` |
| `dzikir_petang` | Dzikir petang | `petang` |
| `dzikir_sebelum_tidur` | Dzikir sebelum tidur | `sebelum_tidur` |
| `istighfar` | Istighfar | `bebas` |
| `sholawat` | Sholawat | `bebas` |
| `tahlil` | Tahlil | `bebas` |
| `asmaul_husna` | Asmaul Husna | `bebas` |
| `dzikir_lainnya` | Dzikir lainnya (custom) | `bebas` |

### TODO — DIISI PEMILIK PROYEK dari buku rujukan cetak

- [ ] **`bacaan` setiap entri masih `[]`.** Isi tiap elemen dengan:
  ```json
  { "arab": "...", "latin": "...", "arti": "...", "sumber": "...", "jumlah_anjuran": 33 }
  ```
  Teks ibadah TIDAK boleh dibuat oleh model bahasa (rawan salah kata / rujukan karangan).
  Parser membuang bacaan tanpa `arab` atau tanpa `sumber`.
- [ ] **`target_default` setiap entri masih `null`.** Angka anjuran (mis. jumlah bacaan) juga
  berasal dari dalil, jadi diisi bersama bacaan setelah divalidasi.
- [ ] Ganti `status_validasi` menjadi `TERVALIDASI` hanya setelah dicek pakar.

Selama `bacaan` kosong, entri itu **hanya bisa mode `centang`** (`DzikirKatalogEntry.modeTersedia`);
mode `layar` baru terbuka otomatis setelah bacaan diisi. Aplikasi tidak crash bila katalog kosong/rusak.

## Skema `logs/{id}` untuk `jenis_ibadah = "dzikir"`

| field | nilai |
|---|---|
| `nama_detail` | salah satu dari 9 slug |
| `nama_detail_asli` | teks bebas, HANYA untuk `dzikir_lainnya` (wajib terisi di sana, null di slug lain) |
| `mode_pencatatan` | `layar` \\| `centang` (wajib bila `dikerjakan`) |
| `waktu_dzikir` | `bada_sholat` \\| `pagi` \\| `petang` \\| `sebelum_tidur` \\| `bebas` (wajib bila `dikerjakan`) |
| `target_hitungan` | int 1–100000 \\| null |
| `jumlah` | layar: int 1–100000 · **centang: null** |
| `durasi_detik` | layar: int 1–86400 · **centang: null** |

Untuk jenis ibadah lain keempat kolom dzikir wajib null. ID dokumen:
`{uid}_{tanggal}_dzikir_{slug}_{idHabit}_{sesiMs}` (satu dokumen per sesi; sesiMs = epoch ms 13 digit).
`selisih_menit_dari_adzan`: dzikir_pagi dari Subuh, dzikir_petang dari Ashar, slug lain null.

Room v16: tabel `dzikir_habits` (migrasi 15→16 hanya CREATE TABLE). `research_logs` v15 (kolom `modePencatatan`, `waktuDzikir`, `targetHitungan`, `durasiDetik`,
semuanya nullable; migrasi 14→15 hanya `ALTER TABLE ADD COLUMN`).

## MENUNGGU_VALIDASI_PAKAR

- Isi `bacaan` ke-9 entri dan `target_default`.
- Pemetaan waktu default tiap slug (terutama `tahlil` dan `asmaul_husna` sebagai `bebas`).
- Acuan selisih adzan: dzikir pagi diukur dari Subuh, dzikir petang dari Ashar.
- Cakupan Mode Cuti terhadap dzikir (saat ini: TETAP_AKTIF; lihat bagian "Mode Cuti" di atas).

## Keputusan yang sudah ditutup

- **Status `udzur` untuk dzikir: TIDAK** (diputuskan pemilik proyek, 2026-10-14). Udzur adalah alasan
  sah yang menggugurkan kewajiban; dzikir bukan kewajiban, jadi tidak ada yang digugurkan.
  Rules (`validStatus`) dan `JENIS_BOLEH_UDZUR` sudah sesuai (hanya sholat_wajib & puasa). Tidak dibuka lagi.
- **Waktu dzikir tidak bisa dipilih pengguna**: diambil dari `waktu_default` katalog, supaya pembedaan
  berjendela vs bebas di rules tidak rusak (mis. "istighfar waktu pagi"). Ditinjau ulang setelah ada data nyata.
- **Heatmap dzikir**: 1 per habit per hari; data per sesi tetap utuh di `logs`.
- **Migrasi kartu dua slot: dihapus.** Kartu itu tidak pernah dirilis, jadi kodenya tidak melayani pengguna nyata.

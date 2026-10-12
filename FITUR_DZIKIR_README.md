# Fitur Dzikir — Tahap 1 (katalog + skema data)

Status: **wadah data saja, belum ada UI baru.** Kartu dzikir lama (dua centang pagi/petang)
masih dipakai dan kini menulis log dengan skema baru: `dzikir_pagi` / `dzikir_petang`,
`mode_pencatatan = "centang"`, `jumlah = null`.

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
`{uid}_{tanggal}_dzikir_{slug}`, atau `{uid}_{tanggal}_dzikir_dzikir_lainnya_{idHabit}`.
`selisih_menit_dari_adzan`: dzikir_pagi dari Subuh, dzikir_petang dari Ashar, slug lain null.

Room: `research_logs` v15 (kolom `modePencatatan`, `waktuDzikir`, `targetHitungan`, `durasiDetik`,
semuanya nullable; migrasi 14→15 hanya `ALTER TABLE ADD COLUMN`).

## MENUNGGU_VALIDASI_PAKAR

- Isi `bacaan` ke-9 entri dan `target_default`.
- Pemetaan waktu default tiap slug (terutama `tahlil` dan `asmaul_husna` sebagai `bebas`).
- Acuan selisih adzan: dzikir pagi diukur dari Subuh, dzikir petang dari Ashar.
- Cakupan Mode Cuti terhadap dzikir (saat ini: tetap boleh, tanpa notifikasi, tidak dihitung "tidak").

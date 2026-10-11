"""Bangkitkan QuranData.kt dari metadata Tanzil (CC BY 3.0). Hanya angka + nama Latin; tanpa teks Arab."""
import sys
import xml.etree.ElementTree as ET

src, out = sys.argv[1], sys.argv[2]

# Transliterasi Indonesia gaya Kemenag (Pedoman Transliterasi Arab-Latin SKB 1987).
NAMA = [
    "Al-Fatihah", "Al-Baqarah", "Ali 'Imran", "An-Nisa'", "Al-Ma'idah", "Al-An'am", "Al-A'raf",
    "Al-Anfal", "At-Taubah", "Yunus", "Hud", "Yusuf", "Ar-Ra'd", "Ibrahim", "Al-Hijr", "An-Nahl",
    "Al-Isra'", "Al-Kahf", "Maryam", "Taha", "Al-Anbiya'", "Al-Hajj", "Al-Mu'minun", "An-Nur",
    "Al-Furqan", "Asy-Syu'ara'", "An-Naml", "Al-Qasas", "Al-'Ankabut", "Ar-Rum", "Luqman",
    "As-Sajdah", "Al-Ahzab", "Saba'", "Fatir", "Yasin", "As-Saffat", "Sad", "Az-Zumar", "Gafir",
    "Fussilat", "Asy-Syura", "Az-Zukhruf", "Ad-Dukhan", "Al-Jasiyah", "Al-Ahqaf", "Muhammad",
    "Al-Fath", "Al-Hujurat", "Qaf", "Az-Zariyat", "At-Tur", "An-Najm", "Al-Qamar", "Ar-Rahman",
    "Al-Waqi'ah", "Al-Hadid", "Al-Mujadalah", "Al-Hasyr", "Al-Mumtahanah", "As-Saff",
    "Al-Jumu'ah", "Al-Munafiqun", "At-Tagabun", "At-Talaq", "At-Tahrim", "Al-Mulk", "Al-Qalam",
    "Al-Haqqah", "Al-Ma'arij", "Nuh", "Al-Jinn", "Al-Muzzammil", "Al-Muddassir", "Al-Qiyamah",
    "Al-Insan", "Al-Mursalat", "An-Naba'", "An-Nazi'at", "'Abasa", "At-Takwir", "Al-Infitar",
    "Al-Mutaffifin", "Al-Insyiqaq", "Al-Buruj", "At-Tariq", "Al-A'la", "Al-Gasyiyah", "Al-Fajr",
    "Al-Balad", "Asy-Syams", "Al-Lail", "Ad-Duha", "Asy-Syarh", "At-Tin", "Al-'Alaq", "Al-Qadr",
    "Al-Bayyinah", "Az-Zalzalah", "Al-'Adiyat", "Al-Qari'ah", "At-Takasur", "Al-'Asr",
    "Al-Humazah", "Al-Fil", "Quraisy", "Al-Ma'un", "Al-Kausar", "Al-Kafirun", "An-Nasr",
    "Al-Lahab", "Al-Ikhlas", "Al-Falaq", "An-Nas",
]
assert len(NAMA) == 114, len(NAMA)

root = ET.parse(src).getroot()
assert root.get("version") == "1.0" and root.get("license") == "cc-by"
suras = sorted(root.iter("sura"), key=lambda s: int(s.get("index")))
ayat = [int(s.get("ayas")) for s in suras]
assert len(ayat) == 114 and sum(ayat) == 6236
offset = [0]
for a in ayat[:-1]:
    offset.append(offset[-1] + a)
for s, o in zip(suras, offset):
    assert int(s.get("start")) == o, (s.get("index"), s.get("start"), o)


def absolute(sura, aya):
    assert 1 <= aya <= ayat[sura - 1]
    return offset[sura - 1] + aya - 1


pages = sorted(root.iter("page"), key=lambda p: int(p.get("index")))
juzs = sorted(root.iter("juz"), key=lambda j: int(j.get("index")))
assert len(pages) == 604 and len(juzs) == 30
page_start = [absolute(int(p.get("sura")), int(p.get("aya"))) for p in pages]
juz_start = [absolute(int(j.get("sura")), int(j.get("aya"))) for j in juzs]
assert page_start == sorted(page_start) and len(set(page_start)) == 604 and page_start[0] == 0
assert juz_start == sorted(juz_start) and juz_start[0] == 0


def ints(values, per_line=16):
    rows = [", ".join(str(v) for v in values[i:i + per_line]) for i in range(0, len(values), per_line)]
    return ",\n        ".join(rows)


names = ",\n        ".join(", ".join('"%s"' % n for n in NAMA[i:i + 6]) for i in range(0, 114, 6))

kt = f'''package com.islami.Aha.data.quran

/**
 * Data posisi Al-Qur'an (mushaf Madinah, 604 halaman) — DIBANGKITKAN OTOMATIS, jangan diedit tangan.
 *
 * Sumber angka: Tanzil Quran Metadata v1.0, (C) 2008-2009 Tanzil.info,
 * lisensi Creative Commons Attribution 3.0 (https://tanzil.net, https://creativecommons.org/licenses/by/3.0/).
 * Yang diambil HANYA angka (jumlah ayat, awal halaman, awal juz). Tidak ada teks Arab,
 * terjemahan, maupun audio. Nama surah = transliterasi Latin gaya Kemenag, ditulis sendiri.
 *
 * Indeks ayat absolut: 0 = 1:1, 6235 = 114:6.
 */
internal object QuranData {{
    const val TOTAL_AYAT = 6236
    const val TOTAL_HALAMAN = 604
    const val TOTAL_JUZ = 30

    /** Nama surah 1..114 (indeks 0..113). */
    val NAMA_SURAH: List<String> = listOf(
        {names}
    )

    /** Jumlah ayat tiap surah 1..114 (indeks 0..113). */
    val JUMLAH_AYAT: IntArray = intArrayOf(
        {ints(ayat)}
    )

    /** Indeks ayat absolut tempat halaman 1..604 dimulai (indeks 0..603). */
    val AWAL_HALAMAN: IntArray = intArrayOf(
        {ints(page_start)}
    )

    /** Indeks ayat absolut tempat juz 1..30 dimulai (indeks 0..29). */
    val AWAL_JUZ: IntArray = intArrayOf(
        {ints(juz_start)}
    )
}}
'''
open(out, "w", encoding="utf-8", newline="\n").write(kt)
print("ok", out, "| ayat 40 =", ayat[39], "| ayat 94 =", ayat[93])

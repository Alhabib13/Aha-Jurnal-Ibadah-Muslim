package com.islami.Aha.data.quran

/** Posisi surah:ayat (keduanya mulai dari 1). */
data class AyatPosition(val surah: Int, val ayat: Int) : Comparable<AyatPosition> {
    val isValid: Boolean
        get() = surah in 1..114 && ayat in 1..QuranData.JUMLAH_AYAT[surah - 1]

    /** Indeks absolut 0..6235; hanya untuk posisi yang valid. */
    val absolute: Int
        get() {
            require(isValid) { "Posisi tidak valid: $surah:$ayat" }
            return SURAH_OFFSET[surah - 1] + ayat - 1
        }

    override fun compareTo(other: AyatPosition): Int = absolute.compareTo(other.absolute)

    override fun toString(): String = "$surah:$ayat"

    companion object {
        val AWAL = AyatPosition(1, 1)
        val AKHIR = AyatPosition(114, 6)

        private val SURAH_OFFSET: IntArray = IntArray(114).also { arr ->
            for (i in 1 until 114) arr[i] = arr[i - 1] + QuranData.JUMLAH_AYAT[i - 1]
        }

        fun fromAbsolute(index: Int): AyatPosition {
            require(index in 0 until QuranData.TOTAL_AYAT)
            val surahIdx = SURAH_OFFSET.indexOfLast { it <= index }
            return AyatPosition(surahIdx + 1, index - SURAH_OFFSET[surahIdx] + 1)
        }
    }
}

/**
 * Hasil hitung satu sesi tilawah.
 * - [jumlahAyat]: ukuran HALUS untuk model (satuan alami hafalan).
 * - [jumlahHalaman]: untuk DITAMPILKAN; halaman mushaf yang tersentuh, minimal 1.
 * - [juzAwal]: juz tempat sesi dimulai (dimensi "di mana" orang membaca).
 */
data class TilawahRange(
    val dari: AyatPosition,
    val sampai: AyatPosition,
    val jumlahAyat: Int,
    val jumlahHalaman: Int,
    val juzAwal: Int
)

object QuranCalculator {

    /** Halaman mushaf (1..604) tempat ayat ini berada. */
    fun halamanOf(pos: AyatPosition): Int = upperIndex(QuranData.AWAL_HALAMAN, pos.absolute) + 1

    /** Juz (1..30) tempat ayat ini berada. */
    fun juzOf(pos: AyatPosition): Int = upperIndex(QuranData.AWAL_JUZ, pos.absolute) + 1

    fun awalJuz(juz: Int): AyatPosition {
        require(juz in 1..QuranData.TOTAL_JUZ)
        return AyatPosition.fromAbsolute(QuranData.AWAL_JUZ[juz - 1])
    }

    fun akhirJuz(juz: Int): AyatPosition {
        require(juz in 1..QuranData.TOTAL_JUZ)
        val nextStart = if (juz == QuranData.TOTAL_JUZ) QuranData.TOTAL_AYAT else QuranData.AWAL_JUZ[juz]
        return AyatPosition.fromAbsolute(nextStart - 1)
    }

    /** Ayat terakhir di halaman mushaf [halaman] (1..604). */
    fun akhirHalaman(halaman: Int): AyatPosition {
        require(halaman in 1..QuranData.TOTAL_HALAMAN)
        val nextStart = if (halaman == QuranData.TOTAL_HALAMAN) QuranData.TOTAL_AYAT else QuranData.AWAL_HALAMAN[halaman]
        return AyatPosition.fromAbsolute(nextStart - 1)
    }

    /** Surah yang (sebagian) masuk ke dalam juz ini, untuk pemilih "pilih juz dulu". */
    fun surahDalamJuz(juz: Int): IntRange = awalJuz(juz).surah..akhirJuz(juz).surah

    /** Null bila salah satu posisi tidak valid atau [sampai] sebelum [dari]. */
    fun hitung(dari: AyatPosition, sampai: AyatPosition): TilawahRange? {
        if (!dari.isValid || !sampai.isValid || sampai < dari) return null
        return TilawahRange(
            dari = dari,
            sampai = sampai,
            jumlahAyat = sampai.absolute - dari.absolute + 1,
            jumlahHalaman = halamanOf(sampai) - halamanOf(dari) + 1,
            juzAwal = juzOf(dari)
        )
    }

    /** Posisi setelah [akhir]; null bila [akhir] adalah ayat terakhir (khatam). */
    fun setelah(akhir: AyatPosition): AyatPosition? =
        if (akhir.absolute + 1 >= QuranData.TOTAL_AYAT) null
        else AyatPosition.fromAbsolute(akhir.absolute + 1)

    /** Indeks terakhir i dengan starts[i] <= value (starts terurut naik, starts[0] = 0). */
    private fun upperIndex(starts: IntArray, value: Int): Int {
        var lo = 0
        var hi = starts.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (starts[mid] <= value) lo = mid else hi = mid - 1
        }
        return lo
    }
}

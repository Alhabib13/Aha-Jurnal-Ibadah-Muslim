package com.islami.Aha.data.dzikir

import com.islami.Aha.data.research.ResearchSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DzikirKatalogTest {

    private fun assetKatalog(): String {
        // Tes dijalankan dari modul :app; file aset asli yang dibundel ke APK.
        val f = listOf("src/main/assets/dzikir_katalog.json", "app/src/main/assets/dzikir_katalog.json")
            .map(::File).first { it.exists() }
        return f.readText(Charsets.UTF_8)
    }

    @Test
    fun `aset katalog berisi tepat 9 slug yang dipakai sebagai nama_detail`() {
        val entri = DzikirKatalog.parse(assetKatalog())
        assertEquals(
            listOf(
                "dzikir_bada_sholat", "dzikir_pagi", "dzikir_petang", "dzikir_sebelum_tidur",
                "istighfar", "sholawat", "tahlil", "asmaul_husna", "dzikir_lainnya"
            ),
            entri.map { it.slug }
        )
        assertEquals(ResearchSchema.DETAIL_PER_JENIS.getValue(ResearchSchema.Jenis.DZIKIR), entri.map { it.slug }.toSet())
    }

    @Test
    fun `aset katalog belum berisi bacaan dan semua menunggu validasi pakar`() {
        val entri = DzikirKatalog.parse(assetKatalog())
        assertTrue(entri.all { it.bacaan.isEmpty() })
        assertTrue(entri.all { it.statusValidasi == DzikirKatalog.STATUS_MENUNGGU_VALIDASI })
        assertTrue(entri.all { it.targetDefault == null })
    }

    @Test
    fun `bacaan kosong hanya boleh mode centang`() {
        val entri = DzikirKatalog.parse(assetKatalog())
        entri.forEach {
            assertEquals(listOf(ResearchSchema.ModePencatatan.CENTANG), it.modeTersedia)
            assertFalse(it.bolehModeLayar)
        }
    }

    @Test
    fun `waktu default sesuai keputusan`() {
        val waktu = DzikirKatalog.parse(assetKatalog()).associate { it.slug to it.waktuDefault }
        assertEquals("bada_sholat", waktu["dzikir_bada_sholat"])
        assertEquals("pagi", waktu["dzikir_pagi"])
        assertEquals("petang", waktu["dzikir_petang"])
        assertEquals("sebelum_tidur", waktu["dzikir_sebelum_tidur"])
        listOf("istighfar", "sholawat", "tahlil", "asmaul_husna", "dzikir_lainnya").forEach {
            assertEquals("bebas", waktu[it])
        }
    }

    // Data uji sintetis: penanda "UJI-..." sengaja bukan teks dzikir sungguhan.
    private val sintetis = """
        {"entri":[
          {"slug":"istighfar","nama":"Istighfar","waktu_default":"bebas","target_default":5,
           "status_validasi":"TERVALIDASI",
           "bacaan":[
             {"arab":"UJI-ARAB-1","latin":"UJI-LATIN-1","arti":"UJI-ARTI-1","sumber":"UJI-SUMBER-1","jumlah_anjuran":3},
             {"arab":"UJI-ARAB-2","latin":"","arti":"","sumber":"","jumlah_anjuran":null},
             {"arab":"","latin":"x","arti":"y","sumber":"UJI-SUMBER-3"}
           ]},
          {"slug":"wirid_asing","nama":"?","waktu_default":"bebas","bacaan":[]},
          {"slug":"tahlil","nama":"Tahlil","waktu_default":"malam","bacaan":[]},
          {"slug":"sholawat","nama":"Sholawat","waktu_default":"bebas","target_default":0,"bacaan":[]}
        ]}
    """.trimIndent()

    @Test
    fun `bacaan lengkap membuka mode layar, bacaan tanpa sumber dibuang`() {
        val entri = DzikirKatalog.parse(sintetis)
        val istighfar = entri.first { it.slug == "istighfar" }
        assertEquals(1, istighfar.bacaan.size)
        assertEquals("UJI-SUMBER-1", istighfar.bacaan.single().sumber)
        assertEquals(3, istighfar.bacaan.single().jumlahAnjuran)
        assertEquals(5, istighfar.targetDefault)
        assertTrue(istighfar.bolehModeLayar)
    }

    @Test
    fun `slug atau waktu di luar skema dibuang, target 0 jadi null`() {
        val slugs = DzikirKatalog.parse(sintetis).map { it.slug }
        assertFalse("wirid_asing" in slugs)
        assertFalse("tahlil" in slugs)
        assertNull(DzikirKatalog.parse(sintetis).first { it.slug == "sholawat" }.targetDefault)
    }

    @Test
    fun `json tanpa entri atau bacaan null tidak crash`() {
        assertTrue(DzikirKatalog.parse("{}").isEmpty())
        val e = DzikirKatalog.parse("""{"entri":[{"slug":"tahlil","waktu_default":"bebas"}]}""").single()
        assertTrue(e.bacaan.isEmpty())
        assertEquals("tahlil", e.nama)
    }
}

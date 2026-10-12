package com.islami.Aha.data.dzikir

import com.islami.Aha.data.model.DzikirHabitEntity
import com.islami.Aha.data.research.ModeCutiPolicy
import com.islami.Aha.data.research.ResearchSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DzikirHabitValidateTest {

    private val katalog: List<DzikirKatalogEntry> by lazy {
        val f = listOf("src/main/assets/dzikir_katalog.json", "app/src/main/assets/dzikir_katalog.json")
            .map(::File).first { it.exists() }
        DzikirKatalog.parse(f.readText(Charsets.UTF_8))
    }

    private fun habit(slug: String, nama: String? = null) = DzikirHabitEntity(
        id = "h-$slug-${nama.orEmpty()}", slug = slug, namaDetailAsli = nama, modePencatatan = "centang",
        waktuDzikir = "bebas", targetHitungan = null, createdAt = 1L, ownerUid = null
    )

    @Test
    fun `semua 9 entri katalog bisa ditambahkan dalam mode centang tanpa target`() {
        assertEquals(9, katalog.size)
        katalog.forEach { e ->
            val nama = if (e.slug == ResearchSchema.Detail.DZIKIR_LAINNYA) "Hizib" else null
            assertNull(e.slug, DzikirHabitRepository.validate(DzikirHabitDraft(e.slug, namaDetailAsli = nama), katalog, emptyList()))
        }
    }

    @Test
    fun `mode layar ditolak selama bacaan kosong`() {
        val err = DzikirHabitRepository.validate(
            DzikirHabitDraft(ResearchSchema.Detail.DZIKIR_PAGI, modePencatatan = ResearchSchema.ModePencatatan.LAYAR),
            katalog, emptyList()
        )
        assertNotNull(err)
    }

    @Test
    fun `dzikir_lainnya wajib nama, boleh banyak asal namanya beda`() {
        val slug = ResearchSchema.Detail.DZIKIR_LAINNYA
        assertNotNull(DzikirHabitRepository.validate(DzikirHabitDraft(slug, namaDetailAsli = "  "), katalog, emptyList()))
        assertNotNull(DzikirHabitRepository.validate(DzikirHabitDraft(slug), katalog, emptyList()))
        val ada = listOf(habit(slug, "Hizib Nashr"))
        assertNull(DzikirHabitRepository.validate(DzikirHabitDraft(slug, namaDetailAsli = "Ratib"), katalog, ada))
        assertNotNull(DzikirHabitRepository.validate(DzikirHabitDraft(slug, namaDetailAsli = "hizib nashr"), katalog, ada))
        assertNotNull(DzikirHabitRepository.validate(DzikirHabitDraft(slug, namaDetailAsli = "x".repeat(101)), katalog, emptyList()))
    }

    @Test
    fun `slug katalog selain lainnya hanya satu habit`() {
        val ada = listOf(habit(ResearchSchema.Detail.ISTIGHFAR))
        assertNotNull(DzikirHabitRepository.validate(DzikirHabitDraft(ResearchSchema.Detail.ISTIGHFAR), katalog, ada))
        assertNull(DzikirHabitRepository.validate(DzikirHabitDraft(ResearchSchema.Detail.SHOLAWAT), katalog, ada))
    }

    @Test
    fun `target opsional, di luar rentang ditolak, slug asing ditolak`() {
        assertNull(DzikirHabitRepository.validate(DzikirHabitDraft(ResearchSchema.Detail.TAHLIL, targetHitungan = 7), katalog, emptyList()))
        assertNotNull(DzikirHabitRepository.validate(DzikirHabitDraft(ResearchSchema.Detail.TAHLIL, targetHitungan = 0), katalog, emptyList()))
        assertNotNull(DzikirHabitRepository.validate(DzikirHabitDraft("wirid_bebas"), katalog, emptyList()))
    }

    @Test
    fun `dokumen firestore tidak menulis kunci bernilai null`() {
        val doc = DzikirHabitRepository.toFirestore(habit(ResearchSchema.Detail.ISTIGHFAR))
        assertEquals(setOf("slug", "mode_pencatatan", "waktu_dzikir", "dibuat_pada"), doc.keys)
        val lain = DzikirHabitRepository.toFirestore(habit(ResearchSchema.Detail.DZIKIR_LAINNYA, "Ratib").copy(targetHitungan = 5))
        assertEquals("Ratib", lain["nama_detail_asli"])
        assertEquals(5L, lain["target_hitungan"])
    }

    @Test
    fun `peta Mode Cuti - dzikir tetap aktif, sholat dan puasa nonaktif`() {
        assertTrue(ModeCutiPolicy.aktif(ResearchSchema.Jenis.DZIKIR, modeCutiAktif = true))
        assertTrue(ModeCutiPolicy.aktif(ResearchSchema.Jenis.TILAWAH, modeCutiAktif = true))
        assertFalse(ModeCutiPolicy.aktif(ResearchSchema.Jenis.SHOLAT_WAJIB, modeCutiAktif = true))
        assertFalse(ModeCutiPolicy.aktif(ResearchSchema.Jenis.PUASA, modeCutiAktif = true))
        assertTrue(ModeCutiPolicy.aktif(ResearchSchema.Jenis.PUASA, modeCutiAktif = false))
        // Semua jenis di skema punya entri eksplisit.
        assertEquals(
            ResearchSchema.DETAIL_PER_JENIS.keys + ResearchSchema.Jenis.QADHA_SHOLAT + ResearchSchema.Jenis.IQRO,
            ModeCutiPolicy.CAKUPAN.keys
        )
    }
}

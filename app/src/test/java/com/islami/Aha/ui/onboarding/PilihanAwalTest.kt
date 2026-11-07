package com.islami.Aha.ui.onboarding

import com.islami.Aha.data.research.ResearchMapping
import com.islami.Aha.data.research.ResearchSchema
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class PilihanAwalTest {

    @Test
    fun `kode pilihan sama persis dengan skema dan firestore rules`() {
        assertEquals(ResearchSchema.Profil.PILIHAN_AWAL_VALUES, OPSI_PILIHAN_AWAL.map { it.kode })
        val rules = File("../firestore.rules").readText()
        val blok = rules.substringAfter("function pilihanAwalValues()").substringAfter("[").substringBefore("]")
        val kodeRules = Regex("\"([a-z_]+)\"").findAll(blok).map { it.groupValues[1] }.toList()
        assertEquals(ResearchSchema.Profil.PILIHAN_AWAL_VALUES, kodeRules)
    }

    @Test
    fun `habit sunnah yang dibuat dipetakan ke nama_detail yang sama dengan kodenya`() {
        OPSI_PILIHAN_AWAL.filter { it.sunnah != null }.forEach { opsi ->
            val h = opsi.sunnah!!
            val slot = ResearchMapping.fromSunnahHabit(h.nama, h.kategori.name)
            assertEquals(opsi.label, opsi.kode, slot?.namaDetail)
        }
    }

    @Test
    fun `Lewati (null) tetap null, Belum ada yang rutin tetap kosong, tidak pernah tertukar`() {
        val n = com.islami.Aha.data.research.ResearchSchema.Profil::normalisasiPilihanAwal
        org.junit.Assert.assertNull(n(null))
        assertEquals(emptyList<String>(), n(emptySet()))
        assertEquals(listOf("dhuha", "tahajud"), n(setOf("tahajud", "dhuha")))
        // Kode tak dikenal dibuang; bila semuanya tak dikenal: null (tidak menjawab), bukan [].
        assertEquals(listOf("dhuha"), n(setOf("dhuha", "kode_lama")))
        org.junit.Assert.assertNull(n(setOf("kode_lama")))
    }
}

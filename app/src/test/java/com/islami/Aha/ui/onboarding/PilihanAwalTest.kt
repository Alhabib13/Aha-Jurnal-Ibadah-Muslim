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
}

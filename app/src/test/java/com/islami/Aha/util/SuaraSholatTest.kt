package com.islami.Aha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuaraSholatTest {
    @Test
    fun slugDariNamaHabitFardhu() {
        assertEquals("subuh", SuaraSholat.slugDariNama("Sholat Subuh"))
        assertEquals("dzuhur", SuaraSholat.slugDariNama("Sholat Dzuhur"))
        assertEquals("ashar", SuaraSholat.slugDariNama("Sholat Ashar"))
        assertEquals("maghrib", SuaraSholat.slugDariNama("Sholat Maghrib"))
        assertEquals("isya", SuaraSholat.slugDariNama("sholat isya"))
    }

    @Test
    fun bukanLimaWaktuTidakPunyaSlug() {
        assertNull(SuaraSholat.slugDariNama("Sholat Dhuha"))
        assertNull(SuaraSholat.slugDariNama("Puasa Senin"))
    }

    @Test
    fun pilihanTidakDikenalJatuhKeNadaHp() {
        assertEquals(SuaraSholat.Pilihan.NADA_HP, SuaraSholat.Pilihan.from(null))
        assertEquals(SuaraSholat.Pilihan.NADA_HP, SuaraSholat.Pilihan.from("lama"))
        assertEquals(SuaraSholat.Pilihan.ADZAN, SuaraSholat.Pilihan.from("adzan"))
    }
}

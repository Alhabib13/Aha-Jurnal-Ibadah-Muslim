package com.islami.Aha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanduanPengingatTest {
    @Test
    fun `merek dengan penghemat baterai agresif mendapat panduan`() {
        listOf(
            "Xiaomi" to "Redmi", "Xiaomi" to "POCO", "Xiaomi" to "xiaomi", "OPPO" to "OPPO", "realme" to "realme",
            "vivo" to "vivo", "vivo" to "iQOO", "HUAWEI" to "HUAWEI", "HONOR" to "HONOR"
        ).forEach { (pabrik, merek) -> assertTrue("$pabrik/$merek", PanduanPengingat.berlakuUntuk(pabrik, merek)) }
    }

    @Test
    fun `merek lain tidak mendapat panduan`() {
        listOf("samsung" to "samsung", "Google" to "google", "motorola" to "motorola", null to null)
            .forEach { (pabrik, merek) -> assertFalse("$pabrik/$merek", PanduanPengingat.berlakuUntuk(pabrik, merek)) }
    }

    @Test
    fun `petunjuk baterai mengikuti keluarga merek`() {
        assertTrue(PanduanPengingat.petunjukBaterai("Xiaomi", "POCO").contains("Tanpa pembatasan"))
        assertTrue(PanduanPengingat.petunjukBaterai("OPPO", "OPPO").contains("latar belakang"))
        assertTrue(PanduanPengingat.petunjukBaterai("HUAWEI", "HUAWEI").contains("Peluncuran aplikasi"))
    }

    @Test
    fun `mode autostart MIUI dibaca sebagai status`() {
        assertEquals(PanduanPengingat.Status.AKTIF, PanduanPengingat.statusDariModeMiui(0))
        assertEquals(PanduanPengingat.Status.BELUM, PanduanPengingat.statusDariModeMiui(1))
        assertEquals(PanduanPengingat.Status.TIDAK_DIKETAHUI, PanduanPengingat.statusDariModeMiui(null))
        assertEquals(PanduanPengingat.Status.TIDAK_DIKETAHUI, PanduanPengingat.statusDariModeMiui(3))
    }

    @Test
    fun `autostart hanya dibaca di keluarga Xiaomi`() {
        assertTrue(PanduanPengingat.keluargaXiaomi("Xiaomi", "Redmi"))
        assertTrue(PanduanPengingat.keluargaXiaomi("Xiaomi", "POCO"))
        assertFalse(PanduanPengingat.keluargaXiaomi("OPPO", "OPPO"))
        assertFalse(PanduanPengingat.keluargaXiaomi(null, null))
    }

    @Test
    fun `langkah tercentang bila aktif, atau tak terbaca tapi sudah dibuka`() {
        val s = PanduanPengingat.Status.values()
        assertTrue(PanduanPengingat.beres(s[0], dibuka = false)) // AKTIF
        assertFalse(PanduanPengingat.beres(PanduanPengingat.Status.BELUM, dibuka = true))
        assertTrue(PanduanPengingat.beres(PanduanPengingat.Status.TIDAK_DIKETAHUI, dibuka = true))
        assertFalse(PanduanPengingat.beres(PanduanPengingat.Status.TIDAK_DIKETAHUI, dibuka = false))
    }

    @Test
    fun `keterangan status jujur soal apa yang terbaca`() {
        assertEquals("Sudah aktif", PanduanPengingat.keteranganStatus(PanduanPengingat.Status.AKTIF, false))
        assertTrue(PanduanPengingat.keteranganStatus(PanduanPengingat.Status.BELUM, true).startsWith("Belum aktif"))
        assertTrue(PanduanPengingat.keteranganStatus(PanduanPengingat.Status.TIDAK_DIKETAHUI, true).startsWith("Sudah dibuka"))
        assertEquals("Ketuk untuk membuka", PanduanPengingat.keteranganStatus(PanduanPengingat.Status.TIDAK_DIKETAHUI, false))
    }
}

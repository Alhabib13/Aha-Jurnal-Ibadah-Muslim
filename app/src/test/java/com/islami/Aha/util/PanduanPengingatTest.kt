package com.islami.Aha.util

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
}

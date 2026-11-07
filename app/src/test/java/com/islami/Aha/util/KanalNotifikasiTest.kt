package com.islami.Aha.util

import com.islami.Aha.util.KanalNotifikasi.Jenis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KanalNotifikasiTest {
    /**
     * ID channel PERMANEN setelah rilis: Android menyimpan setelan pengguna per ID. Tes ini sengaja
     * mengunci string-nya; mengubahnya berarti pengguna kehilangan setelan nada mereka.
     */
    @Test
    fun `ID channel terkunci`() {
        assertEquals(
            setOf(
                "sholat_wajib_subuh", "sholat_wajib_dzuhur", "sholat_wajib_ashar", "sholat_wajib_maghrib", "sholat_wajib_isya",
                "sholat_sunnah", "puasa_sahur", "dzikir_tilawah", "mode_cuti", "aha_konfirmasi"
            ),
            KanalNotifikasi.SEMUA
        )
        assertEquals("grup_sholat_wajib", KanalNotifikasi.GRUP_SHOLAT_WAJIB)
    }

    @Test
    fun `habit bawaan dipetakan ke channel jenisnya`() {
        assertEquals(Jenis.SHOLAT_WAJIB, KanalNotifikasi.jenisHabitBawaan("Sholat Fardhu"))
        assertEquals(Jenis.SHOLAT_SUNNAH, KanalNotifikasi.jenisHabitBawaan("Sholat Tarawih"))
        assertEquals(Jenis.PUASA, KanalNotifikasi.jenisHabitBawaan("Puasa Wajib"))
        assertEquals("sholat_wajib_subuh", KanalNotifikasi.idUntuk(Jenis.SHOLAT_WAJIB, "subuh"))
        assertEquals("puasa_sahur", KanalNotifikasi.idUntuk(Jenis.PUASA))
    }

    @Test
    fun `habit buatan pengguna dipetakan ke channel jenisnya`() {
        assertEquals(Jenis.SHOLAT_SUNNAH, KanalNotifikasi.jenisHabitSunnah("SHOLAT"))
        assertEquals(Jenis.PUASA, KanalNotifikasi.jenisHabitSunnah("PUASA"))
        assertEquals(Jenis.DZIKIR_TILAWAH, KanalNotifikasi.jenisHabitSunnah("DZIKIR"))
        // Tidak diketahui (habit terhapus sebelum alarm berbunyi): jatuh ke sholat sunnah.
        assertEquals(Jenis.SHOLAT_SUNNAH, KanalNotifikasi.jenisHabitSunnah(null))
    }

    @Test
    fun `channel versi lama dikenali untuk dimigrasikan, channel baru tidak`() {
        listOf("habit_reminder_channel", "habit_reminder_channel_v3", "sholat_subuh_v12", "sholat_isya_v2")
            .forEach { assertTrue(it, KanalNotifikasi.termasukChannelLama(it)) }
        (KanalNotifikasi.SEMUA + listOf("fcm_fallback_notification_channel", "sholat_jumat_v1"))
            .forEach { assertFalse(it, KanalNotifikasi.termasukChannelLama(it)) }
    }
}

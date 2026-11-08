package com.islami.Aha.util

import com.islami.Aha.data.research.ResearchSchema
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifikasiSesiTest {
    private val t0 = 1_000_000L
    private val tgl = "2026-10-18"

    @After
    fun tearDown() = NotifikasiSesi.hapus()

    @Test
    fun `pencatatan pertama untuk habit yang diingatkan = dari_notifikasi_dibuka, berikutnya buka_aplikasi`() {
        NotifikasiSesi.tandai("default_1", tgl, t0)
        assertEquals(ResearchSchema.Sumber.DARI_NOTIFIKASI_DIBUKA, NotifikasiSesi.sumberUntuk("default_1", tgl, t0 + 60_000))
        assertEquals(ResearchSchema.Sumber.BUKA_APLIKASI, NotifikasiSesi.sumberUntuk("default_1", tgl, t0 + 61_000))
    }

    @Test
    fun `habit lain tetap buka_aplikasi dan tidak menghabiskan penanda`() {
        NotifikasiSesi.tandai("default_1", tgl, t0)
        assertEquals(ResearchSchema.Sumber.BUKA_APLIKASI, NotifikasiSesi.sumberUntuk("default_2", tgl, t0))
        assertTrue(NotifikasiSesi.pakai("default_1", tgl, t0))
    }

    @Test
    fun `tanggal lain tidak mewarisi atribusi`() {
        NotifikasiSesi.tandai("default_5", "2026-10-17", t0)
        assertFalse(NotifikasiSesi.pakai("default_5", tgl, t0))
        assertTrue(NotifikasiSesi.pakai("default_5", "2026-10-17", t0))
    }

    @Test
    fun `hangus setelah 30 menit`() {
        NotifikasiSesi.tandai("default_1", tgl, t0)
        assertTrue(NotifikasiSesi.pakai("default_1", tgl, t0 + NotifikasiSesi.BERLAKU_MS))
        NotifikasiSesi.tandai("default_1", tgl, t0)
        assertFalse(NotifikasiSesi.pakai("default_1", tgl, t0 + NotifikasiSesi.BERLAKU_MS + 1))
        // Setelah kedaluwarsa penanda dibuang, tidak hidup lagi.
        assertFalse(NotifikasiSesi.pakai("default_1", tgl, t0 + 1))
    }

    @Test
    fun `hangus saat aplikasi ke latar belakang (hapus)`() {
        NotifikasiSesi.tandai("default_1", tgl, t0)
        NotifikasiSesi.hapus()
        assertFalse(NotifikasiSesi.pakai("default_1", tgl, t0))
    }

    @Test
    fun `penanda baru menggantikan yang lama`() {
        NotifikasiSesi.tandai("default_1", tgl, t0)
        NotifikasiSesi.tandai("default_4", tgl, t0 + 10)
        assertFalse(NotifikasiSesi.pakai("default_1", tgl, t0 + 20))
        assertTrue(NotifikasiSesi.pakai("default_4", tgl, t0 + 20))
    }
}

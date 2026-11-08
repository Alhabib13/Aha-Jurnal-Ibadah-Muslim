package com.islami.Aha.ui.home

import com.islami.Aha.util.AgeRange
import com.islami.Aha.util.StatusAktivitas
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LengkapiProfilTest {
    private fun perlu(
        genderKosong: Boolean = false,
        setuju: Boolean? = true,
        usia: AgeRange? = null,
        status: StatusAktivitas? = null,
        hariIni: Long = 100,
        ditunda: Long = 0,
        konfirmasiGender: Boolean = false
    ) = HomeViewModel.perluLengkapiProfil(genderKosong, setuju, usia, status, hariIni, ditunda, konfirmasiGender)

    @Test
    fun `konfirmasi jenis kelamin (pengguna v1-1-1) tampil walau tidak ikut penelitian`() {
        assertTrue(perlu(setuju = false, konfirmasiGender = true))
        assertTrue(perlu(setuju = null, konfirmasiGender = true))
        // "Nanti" tetap berlaku.
        assertFalse(perlu(konfirmasiGender = true, hariIni = 100, ditunda = 107))
    }

    @Test
    fun `tampil untuk peserta penelitian yang usia atau kesibukannya kosong`() {
        assertTrue(perlu())
        assertTrue(perlu(usia = AgeRange.AGE_19_25))
        assertTrue(perlu(status = StatusAktivitas.PEKERJA))
    }

    @Test
    fun `tidak tampil bila sudah lengkap atau tidak ikut penelitian`() {
        assertFalse(perlu(usia = AgeRange.AGE_19_25, status = StatusAktivitas.PEKERJA))
        assertFalse(perlu(setuju = false))
        assertFalse(perlu(setuju = null))
    }

    @Test
    fun `nanti menyembunyikan sampai hari tundaan`() {
        assertFalse(perlu(hariIni = 100, ditunda = 107))
        assertTrue(perlu(hariIni = 107, ditunda = 107))
    }

    @Test
    fun `peserta yang melewati jenis kelamin ditanya lagi walau usia lengkap, tanpa perlu login`() {
        assertTrue(perlu(genderKosong = true, usia = AgeRange.AGE_19_25, status = StatusAktivitas.PEKERJA))
        // Bukan peserta penelitian: tidak diganggu.
        assertFalse(perlu(genderKosong = true, setuju = false, usia = AgeRange.AGE_19_25, status = StatusAktivitas.PEKERJA))
    }
}

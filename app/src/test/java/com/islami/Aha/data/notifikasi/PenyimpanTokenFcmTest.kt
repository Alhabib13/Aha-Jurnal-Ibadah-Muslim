package com.islami.Aha.data.notifikasi

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PenyimpanTokenFcmTest {
    private val jamDasar = 1_800_000_000_000L
    private val sehari = 24 * 3600_000L

    private class LokalPalsu : PenyimpanTokenFcm.Lokal {
        val isi = mutableMapOf<String, PenyimpanTokenFcm.Tersimpan>()
        override fun baca(uid: String) = isi[uid]
        override fun tulis(uid: String, data: PenyimpanTokenFcm.Tersimpan) { isi[uid] = data }
        override fun hapus(uid: String) { isi.remove(uid) }
    }

    private class RemotePalsu(val log: MutableList<String> = mutableListOf()) : PenyimpanTokenFcm.Remote {
        var gagal = false
        override suspend fun simpan(uid: String, token: String) {
            if (gagal) throw IOException("offline")
            log += "simpan:$uid:$token"
        }
        override suspend fun hapus(uid: String) { log += "hapus:$uid" }
        override suspend fun hapusTokenPerangkat() { log += "hapusPerangkat" }
    }

    private var uid: String? = "alice"
    private var token: String? = "tok1"
    private var sekarang = jamDasar
    private val lokal = LokalPalsu()
    private val remote = RemotePalsu()
    private val p = PenyimpanTokenFcm(lokal, remote, { uid }, { token }, { sekarang })

    @Test
    fun `tanpa login tidak ada yang ditulis`() = runTest {
        uid = null
        assertFalse(p.segarkan())
        assertFalse(p.tokenBaru("tok2"))
        assertTrue(remote.log.isEmpty())
    }

    @Test
    fun `aplikasi dibuka pertama kali menulis token dan mencatatnya per akun`() = runTest {
        assertTrue(p.segarkan())
        assertEquals(listOf("simpan:alice:tok1"), remote.log)
        assertEquals(PenyimpanTokenFcm.Tersimpan("tok1", jamDasar), lokal.isi["alice"])
    }

    @Test
    fun `dibuka lagi dengan token sama sebelum 24 jam tidak menulis ulang, sesudahnya menulis`() = runTest {
        p.segarkan()
        sekarang = jamDasar + sehari - 1
        assertFalse(p.segarkan())
        sekarang = jamDasar + sehari
        assertTrue(p.segarkan())
        assertEquals(2, remote.log.size)
    }

    @Test
    fun `onNewToken dengan token berbeda selalu ditulis`() = runTest {
        p.segarkan()
        assertTrue(p.tokenBaru("tok2"))
        assertEquals("simpan:alice:tok2", remote.log.last())
        assertEquals("tok2", lokal.isi["alice"]?.token)
    }

    @Test
    fun `gagal kirim tidak dicatat, dicoba lagi berikutnya`() = runTest {
        remote.gagal = true
        assertFalse(p.segarkan())
        assertNull(lokal.isi["alice"])
        remote.gagal = false
        assertTrue(p.segarkan())
    }

    @Test
    fun `token kosong atau gagal diambil tidak ditulis`() = runTest {
        token = null
        assertFalse(p.segarkan())
        assertFalse(p.tokenBaru(""))
        assertTrue(remote.log.isEmpty())
    }

    @Test
    fun `akun lain di HP yang sama ditulis terpisah`() = runTest {
        p.segarkan()
        uid = "bob"
        assertTrue(p.segarkan())
        assertEquals("simpan:bob:tok1", remote.log.last())
    }

    @Test
    fun `logout menghapus token di server sebelum signOut, lalu token perangkat`() = runTest {
        p.segarkan()
        p.hapusSaatLogout { remote.log += "signOut"; uid = null }
        assertEquals(listOf("simpan:alice:tok1", "hapus:alice", "signOut", "hapusPerangkat"), remote.log)
        assertNull(lokal.isi["alice"])
    }

    @Test
    fun `logout tetap signOut walau penghapusan di server gagal`() = runTest {
        val remoteGagal = object : PenyimpanTokenFcm.Remote {
            val log = mutableListOf<String>()
            override suspend fun simpan(uid: String, token: String) {}
            override suspend fun hapus(uid: String) { throw IOException("offline") }
            override suspend fun hapusTokenPerangkat() { log += "hapusPerangkat" }
        }
        var keluar = false
        PenyimpanTokenFcm(lokal, remoteGagal, { uid }, { token }, { sekarang }).hapusSaatLogout { keluar = true }
        assertTrue(keluar)
        assertEquals(listOf("hapusPerangkat"), remoteGagal.log)
    }

    @Test
    fun `unggah jadwal yang dipicu di tengah logout menunggu sampai signOut selesai`() = runTest {
        val urutan = mutableListOf<String>()
        val tahan = kotlinx.coroutines.CompletableDeferred<Unit>()
        val remoteLambat = object : PenyimpanTokenFcm.Remote {
            override suspend fun simpan(uid: String, token: String) {}
            override suspend fun hapus(uid: String) { urutan += "hapusServer"; tahan.await() }
            override suspend fun hapusTokenPerangkat() {}
        }
        val q = PenyimpanTokenFcm(lokal, remoteLambat, { uid }, { token }, { sekarang })
        val logout = launch { q.hapusSaatLogout { urutan += "signOut"; uid = null } }
        testScheduler.runCurrent()
        // Pemicu jadwal datang saat penghapusan server masih berjalan.
        val sinkron = launch { q.eksklusif { urutan += "sinkron(uid=$uid)" } }
        testScheduler.runCurrent()
        assertEquals(listOf("hapusServer"), urutan)
        tahan.complete(Unit)
        logout.join(); sinkron.join()
        // Jadwal tidak tertulis ulang untuk akun yang sedang keluar.
        assertEquals(listOf("hapusServer", "signOut", "sinkron(uid=null)"), urutan)
    }

    @Test
    fun `lupakan membuat token ditulis ulang`() = runTest {
        p.segarkan()
        p.lupakan("alice")
        assertTrue(p.segarkan())
    }
}

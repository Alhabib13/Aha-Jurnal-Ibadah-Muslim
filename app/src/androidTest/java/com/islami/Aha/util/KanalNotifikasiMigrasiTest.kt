package com.islami.Aha.util

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Migrasi dari channel versi lama ke channel permanen v1.2.0 (Android 8+). */
@RunWith(AndroidJUnit4::class)
class KanalNotifikasiMigrasiTest {
    private lateinit var context: Context
    private lateinit var nm: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        nm = context.getSystemService(NotificationManager::class.java)
        // Keadaan perangkat yang memasang versi lama: channel umum berversi, channel per waktu
        // berversi, dan grup "waktu_sholat".
        nm.createNotificationChannelGroup(NotificationChannelGroup("waktu_sholat", "Waktu sholat"))
        nm.createNotificationChannel(NotificationChannel("habit_reminder_channel_v2", "Pengingat Ibadah", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel("sholat_subuh_v7", "Waktu Subuh", NotificationManager.IMPORTANCE_HIGH).apply { group = "waktu_sholat" })
        nm.createNotificationChannel(NotificationChannel("sholat_isya_v3", "Waktu Isya", NotificationManager.IMPORTANCE_HIGH).apply { group = "waktu_sholat" })
        // Build uji sebelum channel puasa dipecah.
        nm.createNotificationChannel(NotificationChannel("puasa_sahur", "Puasa dan sahur", NotificationManager.IMPORTANCE_HIGH))
    }

    @Test
    fun channelLamaDanGrupLamaHilang_channelBaruLengkap() {
        KanalNotifikasi.pastikan(context)
        val ada = nm.notificationChannels.map { it.id }.toSet()
        listOf("habit_reminder_channel_v2", "sholat_subuh_v7", "sholat_isya_v3", "puasa_sahur").forEach {
            assertFalse("channel lama $it masih ada", it in ada)
        }
        assertFalse(nm.notificationChannelGroups.any { it.id == "waktu_sholat" })
        (KanalNotifikasi.SEMUA - KanalNotifikasi.KONFIRMASI).forEach { assertTrue("channel $it belum dibuat", it in ada) }
        assertTrue(nm.notificationChannelGroups.any { it.id == KanalNotifikasi.GRUP_SHOLAT_WAJIB })
        KanalNotifikasi.WAKTU_SHOLAT.forEach {
            assertEquals(KanalNotifikasi.GRUP_SHOLAT_WAJIB, nm.getNotificationChannel(KanalNotifikasi.sholatWajib(it)).group)
        }
    }

    @Test
    fun sholatWajibDanSahurDiJalurAlarm_sunnahDanPuasaSunnahDiJalurNotifikasi() {
        KanalNotifikasi.pastikan(context)
        val subuh = nm.getNotificationChannel("sholat_wajib_subuh")
        assertEquals(AudioAttributes.USAGE_ALARM, subuh.audioAttributes.usage)
        assertNotNull(subuh.sound)
        val sahur = nm.getNotificationChannel(KanalNotifikasi.SAHUR)
        assertEquals(AudioAttributes.USAGE_ALARM, sahur.audioAttributes.usage)
        assertTrue(sahur.importance >= NotificationManager.IMPORTANCE_HIGH)
        assertEquals(AudioAttributes.USAGE_NOTIFICATION, nm.getNotificationChannel(KanalNotifikasi.SHOLAT_SUNNAH).audioAttributes.usage)
        // Pengingat puasa sunnah yang disetel malam hari tidak berbunyi seperti alarm.
        assertEquals(AudioAttributes.USAGE_NOTIFICATION, nm.getNotificationChannel(KanalNotifikasi.PUASA_SUNNAH).audioAttributes.usage)
    }

    @Test
    fun berulangKali_aman() {
        KanalNotifikasi.pastikan(context)
        val sebelum = nm.notificationChannels.map { it.id }.toSet()
        KanalNotifikasi.pastikan(context)
        assertEquals(sebelum, nm.notificationChannels.map { it.id }.toSet())
    }
}

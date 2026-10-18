package com.islami.Aha.util

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.islami.Aha.data.research.ResearchSchema

/**
 * Atribusi "dari_notifikasi_dibuka": pengguna mengetuk BADAN notifikasi pengingat, aplikasi
 * terbuka, lalu ibadah yang diingatkan itu dicatat dalam sesi yang sama.
 *
 * Penanda (id habit di notifikasi + tanggal pengingat) dipasang dari intent pembuka aplikasi,
 * dipakai SEKALI oleh pencatatan pertama untuk habit & tanggal itu, dan hangus saat aplikasi
 * ke latar belakang atau setelah [BERLAKU_MS], mana yang lebih dulu. Hanya di memori.
 */
object NotifikasiSesi {
    const val EXTRA_HABIT_ID = "notif_dibuka_habit_id"
    const val EXTRA_TANGGAL = "notif_dibuka_tanggal"
    const val BERLAKU_MS = 30 * 60_000L

    private data class Penanda(val habitId: String, val tanggal: String, val sejakMs: Long)

    private var penanda: Penanda? = null
    private var aktivitasTampil = 0

    /** Dipanggil sekali dari Application.onCreate: penanda hangus saat aplikasi ke latar belakang. */
    fun pasang(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                aktivitasTampil++
            }

            override fun onActivityStopped(activity: Activity) {
                aktivitasTampil = (aktivitasTampil - 1).coerceAtLeast(0)
                // Pergantian konfigurasi (rotasi) bukan berarti ke latar belakang.
                if (aktivitasTampil == 0 && !activity.isChangingConfigurations) hapus()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    /** Pasang penanda dari intent pembuka aplikasi; extra dibuang agar tidak terbaca dua kali. */
    fun dariIntent(intent: Intent?, nowMs: Long = System.currentTimeMillis()) {
        val habitId = intent?.getStringExtra(EXTRA_HABIT_ID) ?: return
        val tanggal = intent.getStringExtra(EXTRA_TANGGAL) ?: DateUtils.getTodayKey()
        tandai(habitId, tanggal, nowMs)
        intent.removeExtra(EXTRA_HABIT_ID)
        intent.removeExtra(EXTRA_TANGGAL)
    }

    @Synchronized
    fun tandai(habitId: String, tanggal: String, nowMs: Long = System.currentTimeMillis()) {
        penanda = Penanda(habitId, tanggal, nowMs)
    }

    @Synchronized
    fun hapus() {
        penanda = null
    }

    /**
     * true (dan penanda dipakai habis) bila pencatatan [habitId] pada [tanggal] adalah yang
     * pertama sejak aplikasi dibuka dari notifikasinya, dalam [BERLAKU_MS].
     */
    @Synchronized
    fun pakai(habitId: String, tanggal: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val p = penanda ?: return false
        if (nowMs - p.sejakMs !in 0..BERLAKU_MS) {
            penanda = null
            return false
        }
        if (p.habitId != habitId || p.tanggal != tanggal) return false
        penanda = null
        return true
    }

    /** sumber_pencatatan untuk pencatatan "dikerjakan" dari dalam aplikasi. */
    fun sumberUntuk(habitId: String, tanggal: String, nowMs: Long = System.currentTimeMillis()): String =
        if (pakai(habitId, tanggal, nowMs)) ResearchSchema.Sumber.DARI_NOTIFIKASI_DIBUKA
        else ResearchSchema.Sumber.BUKA_APLIKASI
}

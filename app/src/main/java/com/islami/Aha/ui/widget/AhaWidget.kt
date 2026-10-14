package com.islami.Aha.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.islami.Aha.MainActivity
import com.islami.Aha.R
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.repository.DefaultHabitCompletionRepository
import com.islami.Aha.util.DateUtils
import com.islami.Aha.util.TertinggalStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ---------------------------------------------------------------- data

enum class StatusWaktu { SELESAI, TERTINGGAL, SEKARANG, NANTI }

data class WaktuWidget(val nama: String, val jam: LocalTime?, val status: StatusWaktu)

data class DataWidget(
    val waktu: List<WaktuWidget>,
    val berikutnyaNama: String?,
    val berikutnyaJam: String?,
    val besok: Boolean,
    val selesai: Int
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun habitDao(): HabitDao
    fun habitCompletionDao(): HabitCompletionDao
}

private fun parseJam(time: String): LocalTime? = runCatching {
    val (h, m) = time.split(":").map { it.trim().toInt() }
    LocalTime.of(h, m)
}.getOrNull()

private fun fmt(t: LocalTime) = "%02d.%02d".format(t.hour, t.minute)

internal suspend fun muatDataWidget(context: Context, sekarang: LocalDateTime = LocalDateTime.now()): DataWidget {
    val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val fardhu = ep.habitDao().getFardhuHabits()
    val selesaiKeys = ep.habitCompletionDao().getRecordsForDate(DateUtils.getTodayKey()).map { it.habitKey }.toSet()
    val tertinggal = TertinggalStore.hariIni(context)
    val jamSekarang = sekarang.toLocalTime()

    val urut = fardhu.map { it to parseJam(it.time) }
    // Waktu yang sedang berjalan = waktu terakhir yang jamnya sudah lewat.
    val idxSekarang = urut.indexOfLast { (_, jam) -> jam != null && !jam.isAfter(jamSekarang) }
    val waktu = urut.mapIndexed { i, (h, jam) ->
        val status = when {
            DefaultHabitCompletionRepository.habitKey(h.id) in selesaiKeys -> StatusWaktu.SELESAI
            h.id in tertinggal -> StatusWaktu.TERTINGGAL
            i == idxSekarang -> StatusWaktu.SEKARANG
            else -> StatusWaktu.NANTI
        }
        WaktuWidget(h.name.removePrefix("Sholat ").trim(), jam, status)
    }
    val berikutnya = waktu.firstOrNull { it.jam != null && it.jam.isAfter(jamSekarang) }
    val besok = berikutnya == null
    val target = berikutnya ?: waktu.firstOrNull { it.jam != null }
    return DataWidget(
        waktu = waktu,
        berikutnyaNama = target?.nama,
        berikutnyaJam = target?.jam?.let(::fmt),
        besok = besok,
        selesai = waktu.count { it.status == StatusWaktu.SELESAI }
    )
}

// ---------------------------------------------------------------- tampilan

private val Putih = ColorProvider(Color(0xFFEEF2F5))
private val Redup = ColorProvider(Color(0xFFA7B3BF))
private val Hijau = ColorProvider(Color(0xFF2CC795))
private val Emas = ColorProvider(Color(0xFFE2B65A))
private val TeksDiHijau = ColorProvider(Color(0xFF062B1F))
private val Pink = ColorProvider(Color(0xFFF48FB1))

@Composable
private fun Lingkaran(status: StatusWaktu, ukuran: Int) {
    val bg = when (status) {
        StatusWaktu.SELESAI -> R.drawable.widget_lingkaran_selesai
        StatusWaktu.TERTINGGAL -> R.drawable.widget_lingkaran_tertinggal
        StatusWaktu.SEKARANG -> R.drawable.widget_lingkaran_sekarang
        StatusWaktu.NANTI -> R.drawable.widget_lingkaran_kosong
    }
    Box(
        modifier = GlanceModifier.size(ukuran.dp).background(ImageProvider(bg)),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            StatusWaktu.SELESAI -> Text("✓", style = TextStyle(color = TeksDiHijau, fontSize = (ukuran / 2).sp, fontWeight = FontWeight.Bold))
            StatusWaktu.TERTINGGAL -> Text("!", style = TextStyle(color = Pink, fontSize = (ukuran / 2).sp, fontWeight = FontWeight.Bold))
            else -> {}
        }
    }
}

@Composable
private fun KontenSedang(d: DataWidget) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(LocalContext.current, MainActivity::class.java)))
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(if (d.besok) "Berikutnya · besok" else "Berikutnya", style = TextStyle(color = Redup, fontSize = 12.sp))
                Text(
                    if (d.berikutnyaNama != null) "${d.berikutnyaNama} ${d.berikutnyaJam}" else "Jadwal belum ada",
                    style = TextStyle(color = Putih, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text("${d.selesai} dari ${d.waktu.size} sholat tercatat", style = TextStyle(color = Hijau, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            }
            Text("Aha", style = TextStyle(color = Redup, fontSize = 12.sp))
        }
        Spacer(GlanceModifier.height(10.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            d.waktu.forEach { w ->
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Lingkaran(w.status, 32)
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        w.nama,
                        style = TextStyle(
                            color = if (w.status == StatusWaktu.SEKARANG) Emas else Redup,
                            fontSize = 11.sp,
                            fontWeight = if (w.status == StatusWaktu.SEKARANG) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun KontenKecil(d: DataWidget) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(LocalContext.current, MainActivity::class.java)))
    ) {
        Text(if (d.besok) "Besok" else "Berikutnya", style = TextStyle(color = Redup, fontSize = 12.sp))
        Spacer(GlanceModifier.defaultWeight())
        Text(d.berikutnyaNama ?: "—", style = TextStyle(color = Putih, fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        Text(d.berikutnyaJam ?: "", style = TextStyle(color = Putih, fontSize = 24.sp))
        Spacer(GlanceModifier.defaultWeight())
        Row {
            d.waktu.forEachIndexed { i, w ->
                if (i > 0) Spacer(GlanceModifier.width(4.dp))
                Lingkaran(w.status, 14)
            }
        }
    }
}

class AhaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = muatDataWidget(context)
        provideContent { KontenSedang(data) }
    }
}

class AhaWidgetKecil : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = muatDataWidget(context)
        provideContent { KontenKecil(data) }
    }
}

class AhaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AhaWidget()
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Ikut perbarui ukuran lain & jadwalkan pergantian waktu berikutnya.
        AhaWidgetUpdater.requestUpdate(context)
    }
}

class AhaWidgetKecilReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AhaWidgetKecil()
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Ikut perbarui ukuran lain & jadwalkan pergantian waktu berikutnya.
        AhaWidgetUpdater.requestUpdate(context)
    }
}

// ---------------------------------------------------------------- pembaruan

object AhaWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val REQ_PERGANTIAN = 778_001

    private fun adaWidget(context: Context): Boolean {
        val m = AppWidgetManager.getInstance(context)
        return listOf(AhaWidgetReceiver::class.java, AhaWidgetKecilReceiver::class.java).any {
            m.getAppWidgetIds(ComponentName(context, it)).isNotEmpty()
        }
    }

    /** Gambar ulang semua widget Aha (dipanggil setelah ada catatan baru). */
    fun requestUpdate(context: Context) {
        val app = context.applicationContext
        if (!runCatching { adaWidget(app) }.getOrDefault(false)) return
        scope.launch {
            runCatching {
                AhaWidget().updateAll(app)
                AhaWidgetKecil().updateAll(app)
            }
            jadwalkanPergantianWaktu(app)
        }
    }

    /**
     * Alarm ringan (tidak membangunkan HP) di pergantian waktu sholat berikutnya, supaya
     * "Berikutnya" dan lingkaran emas tetap benar walau pengingat dimatikan.
     */
    fun jadwalkanPergantianWaktu(context: Context) {
        val app = context.applicationContext
        scope.launch {
            runCatching {
                if (!adaWidget(app)) return@runCatching
                val d = muatDataWidget(app)
                val sekarang = LocalDateTime.now()
                val jamBerikut = d.waktu.mapNotNull { it.jam }.firstOrNull { it.isAfter(sekarang.toLocalTime()) }
                val target = if (jamBerikut != null) LocalDate.now().atTime(jamBerikut)
                else LocalDate.now().plusDays(1).atStartOfDay().plusMinutes(1)
                val millis = target.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                // Kirim ke ukuran yang benar-benar terpasang; onUpdate-nya memperbarui semuanya.
                val m = AppWidgetManager.getInstance(app)
                val penerima = listOf(AhaWidgetReceiver::class.java, AhaWidgetKecilReceiver::class.java)
                    .first { m.getAppWidgetIds(ComponentName(app, it)).isNotEmpty() }
                val intent = Intent(app, penerima).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, m.getAppWidgetIds(ComponentName(app, penerima)))
                }
                val pi = PendingIntent.getBroadcast(
                    app, REQ_PERGANTIAN, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                (app.getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(AlarmManager.RTC, millis, pi)
            }
        }
    }
}

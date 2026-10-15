package com.islami.Aha.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import com.islami.Aha.data.research.ModeCutiPolicy
import com.islami.Aha.data.research.ResearchSchema
import com.islami.Aha.util.DateUtils
import com.islami.Aha.util.TertinggalStore
import com.islami.Aha.util.UserPreferencesManager
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
    val selesai: Int,
    /** Mode Cuti: sholat tidak dihitung, jadi widget tidak menampilkan hitungan. */
    val modeCuti: Boolean = false
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
    val modeCuti = !ModeCutiPolicy.aktif(ResearchSchema.Jenis.SHOLAT_WAJIB, UserPreferencesManager.isHaidhMode.value)

    val urut = fardhu.map { it to parseJam(it.time) }
    // Waktu yang sedang berjalan = waktu terakhir yang jamnya sudah lewat.
    val idxSekarang = urut.indexOfLast { (_, jam) -> jam != null && !jam.isAfter(jamSekarang) }
    val waktu = urut.mapIndexed { i, (h, jam) ->
        val status = when {
            modeCuti -> StatusWaktu.NANTI
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
        selesai = waktu.count { it.status == StatusWaktu.SELESAI },
        modeCuti = modeCuti
    )
}

// ---------------------------------------------------------------- tampilan

private val Putih = ColorProvider(Color(0xFFFFFFFF))
private val PutihRedup = ColorProvider(Color(0xCCFFFFFF))
private val PutihSamar = ColorProvider(Color(0x99FFFFFF))
private val Emas = ColorProvider(Color(0xFFF2C66D))
private val HijauTua = ColorProvider(Color(0xFF0D7C5F))
private val Pink = ColorProvider(Color(0xFFFFC1D6))

/** Ambang tampilan. Glance memilih ukuran terbesar yang muat di sel widget. */
private val UKURAN_KECIL = DpSize(110.dp, 110.dp)
private val UKURAN_PENDEK = DpSize(250.dp, 50.dp)
private val UKURAN_SEDANG = DpSize(250.dp, 110.dp)
private val UKURAN_TINGGI = DpSize(250.dp, 220.dp)

@Composable
private fun bukaAplikasi() = GlanceModifier.clickable(
    actionStartActivity(Intent(LocalContext.current, MainActivity::class.java))
)

@Composable
private fun Latar(content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .cornerRadius(24.dp)
            .then(bukaAplikasi())
    ) { content() }
}

@Composable
private fun Logo(ukuran: Int) {
    Box(
        modifier = GlanceModifier.size(ukuran.dp).background(ImageProvider(R.drawable.widget_logo_badge)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.logo),
            contentDescription = "Aha",
            modifier = GlanceModifier.size((ukuran * 0.68f).toInt().dp)
        )
    }
}

@Composable
private fun Chip(teks: String) {
    Box(
        modifier = GlanceModifier
            .background(ImageProvider(R.drawable.widget_chip))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(teks, style = TextStyle(color = Putih, fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

private fun teksProgres(d: DataWidget) =
    if (d.modeCuti) "Mode Cuti aktif" else "${d.selesai}/${d.waktu.size} tercatat"

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
            StatusWaktu.SELESAI -> Text("✓", style = TextStyle(color = HijauTua, fontSize = (ukuran / 2).sp, fontWeight = FontWeight.Bold))
            StatusWaktu.TERTINGGAL -> Text("!", style = TextStyle(color = Pink, fontSize = (ukuran / 2).sp, fontWeight = FontWeight.Bold))
            else -> {}
        }
    }
}

@Composable
private fun BlokBerikutnya(d: DataWidget, ukuranNama: Int) {
    Column {
        Text(
            if (d.besok) "Sholat berikutnya · besok" else "Sholat berikutnya",
            style = TextStyle(color = PutihRedup, fontSize = 11.sp),
            maxLines = 1
        )
        if (d.berikutnyaNama != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(d.berikutnyaNama, style = TextStyle(color = Putih, fontSize = ukuranNama.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Spacer(GlanceModifier.width(8.dp))
                Text(d.berikutnyaJam.orEmpty(), style = TextStyle(color = Emas, fontSize = ukuranNama.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            }
        } else {
            Text("Jadwal belum ada", style = TextStyle(color = Putih, fontSize = 16.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun DeretTitik(d: DataWidget, ukuran: Int, jarak: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        d.waktu.forEachIndexed { i, w ->
            if (i > 0) Spacer(GlanceModifier.width(jarak.dp))
            Lingkaran(w.status, ukuran)
        }
    }
}

/** 2x2 / sempit: logo, sholat berikutnya, deret titik. */
@Composable
private fun TampilanKecil(d: DataWidget) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Logo(26)
            Spacer(GlanceModifier.defaultWeight())
            Text(if (d.besok) "Besok" else "Berikutnya", style = TextStyle(color = PutihRedup, fontSize = 11.sp))
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(d.berikutnyaNama ?: "—", style = TextStyle(color = Putih, fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        Text(d.berikutnyaJam.orEmpty(), style = TextStyle(color = Emas, fontSize = 22.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        Spacer(GlanceModifier.defaultWeight())
        DeretTitik(d, 12, 5)
    }
}

/** 4x1 / pendek: satu baris. */
@Composable
private fun TampilanPendek(d: DataWidget) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Logo(32)
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(if (d.besok) "Berikutnya · besok" else "Berikutnya", style = TextStyle(color = PutihRedup, fontSize = 11.sp), maxLines = 1)
            Text(
                listOfNotNull(d.berikutnyaNama, d.berikutnyaJam).joinToString(" ").ifBlank { "Jadwal belum ada" },
                style = TextStyle(color = Putih, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
        DeretTitik(d, 14, 5)
    }
}

/** 4x2: kepala, sholat berikutnya, lima waktu dengan jamnya. */
@Composable
private fun TampilanSedang(d: DataWidget) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Logo(30)
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) { BlokBerikutnya(d, 18) }
            Chip(teksProgres(d))
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            d.waktu.forEach { w ->
                val sekarang = w.status == StatusWaktu.SEKARANG
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Lingkaran(w.status, 26)
                    Spacer(GlanceModifier.height(3.dp))
                    Text(
                        w.nama,
                        style = TextStyle(
                            color = if (sekarang) Emas else Putih,
                            fontSize = 11.sp,
                            fontWeight = if (sekarang) FontWeight.Bold else FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                    Text(w.jam?.let(::fmt) ?: "--.--", style = TextStyle(color = PutihSamar, fontSize = 10.sp), maxLines = 1)
                }
            }
        }
    }
}

/** 4x3 ke atas: daftar lima waktu ke bawah. */
@Composable
private fun TampilanTinggi(d: DataWidget) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(14.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Logo(34)
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) { BlokBerikutnya(d, 20) }
        }
        Spacer(GlanceModifier.height(8.dp))
        Chip(teksProgres(d))
        Spacer(GlanceModifier.height(6.dp))
        d.waktu.forEach { w ->
            val sekarang = w.status == StatusWaktu.SEKARANG
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .then(if (sekarang) GlanceModifier.background(ImageProvider(R.drawable.widget_baris)) else GlanceModifier)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Lingkaran(w.status, 20)
                Spacer(GlanceModifier.width(10.dp))
                Text(
                    w.nama,
                    style = TextStyle(color = if (sekarang) Emas else Putih, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1
                )
                Text(w.jam?.let(::fmt) ?: "--.--", style = TextStyle(color = if (sekarang) Emas else PutihRedup, fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun KontenWidget(d: DataWidget) {
    val ukuran = LocalSize.current
    Latar {
        when {
            ukuran.width < 250.dp -> TampilanKecil(d)
            ukuran.height < 110.dp -> TampilanPendek(d)
            ukuran.height < 220.dp -> TampilanSedang(d)
            else -> TampilanTinggi(d)
        }
    }
}

class AhaWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(UKURAN_KECIL, UKURAN_PENDEK, UKURAN_SEDANG, UKURAN_TINGGI))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = muatDataWidget(context)
        provideContent { KontenWidget(data) }
    }
}

class AhaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AhaWidget()
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Jadwalkan pergantian waktu berikutnya.
        AhaWidgetUpdater.requestUpdate(context)
    }
}

// ---------------------------------------------------------------- pembaruan

object AhaWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val REQ_PERGANTIAN = 778_001

    private fun idWidget(context: Context): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, AhaWidgetReceiver::class.java))

    /** Gambar ulang widget Aha (dipanggil setelah ada catatan baru). */
    fun requestUpdate(context: Context) {
        val app = context.applicationContext
        if (runCatching { idWidget(app).isEmpty() }.getOrDefault(true)) return
        scope.launch {
            runCatching { AhaWidget().updateAll(app) }
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
                val ids = idWidget(app)
                if (ids.isEmpty()) return@runCatching
                val d = muatDataWidget(app)
                val sekarang = LocalDateTime.now()
                val jamBerikut = d.waktu.mapNotNull { it.jam }.firstOrNull { it.isAfter(sekarang.toLocalTime()) }
                val target = if (jamBerikut != null) LocalDate.now().atTime(jamBerikut)
                else LocalDate.now().plusDays(1).atStartOfDay().plusMinutes(1)
                val millis = target.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val intent = Intent(app, AhaWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
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

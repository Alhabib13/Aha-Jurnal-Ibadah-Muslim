package com.islami.Aha.ui.statistic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.repository.FeatureConfigRepository
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.ui.addhabit.SunnahCategoryType
import com.islami.Aha.ui.shared.SunnahHabitSharedViewModel
import com.islami.Aha.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import com.islami.Aha.data.quran.QuranCalculator
import com.islami.Aha.data.quran.QuranData
import com.islami.Aha.data.research.ResearchSchema
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private data class StatCombineData(
    val habits: List<com.islami.Aha.data.model.Habit>,
    val sunnahHabits: List<com.islami.Aha.domain.model.SunnahHabit>,
    val config: com.islami.Aha.data.repository.AppFeatureConfig,
    val haidhDates: Set<String>,
    val researchTick: String
)

data class PastDayHabitItem(
    val habitKey: String, // e.g. "default_1" or "sunnah_abc"
    val name: String,
    val isCompleted: Boolean,
    val category: String,
    val isSunnah: Boolean
)

data class StatisticUiState(
    val isLoading: Boolean = true,
    /** "Kamis, 15 Oktober 2026 · 4 Jumadil Awal 1448 H" */
    val judulTanggal: String = "",
    val todayCompleted: Int = 0,
    val todayTotal: Int = 0,
    /** "5 sholat fardhu + 2 dzikir pilihanmu"; kosong bila tidak ada ibadah harian. */
    val rincianHariIni: String = "",
    val modeCuti: Boolean = false,
    val currentStreak: Int = 0,
    val bulan: YearMonth = YearMonth.now(),
    val labelBulan: String = "",
    val labelHijriBulan: String = "",
    val bisaMaju: Boolean = false,
    val kalender: List<HariKalender?> = emptyList(),
    val tanggalDipilih: String = "",
    val judulTanggalDipilih: String = "",
    val chipsDipilih: List<ChipCatatan> = emptyList(),
    val tanggalDipilihCuti: Boolean = false,
    val pekan: RingkasanPekan = RingkasanPekan(),
    val punyaDzikir: Boolean = false,
    val punyaSunnah: Boolean = false,
    /** Juz posisi bacaan harian; null = belum ada sesi. */
    val khatamJuz: Int? = null,
    val khatamKeterangan: String = ""
) {
    val tanggalDipilihHariIni: Boolean get() = tanggalDipilih == DateUtils.getTodayKey()
}

@HiltViewModel
class StatisticViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val habitCompletionDao: HabitCompletionDao,
    private val habitDao: HabitDao,
    private val sunnahHabitSharedViewModel: SunnahHabitSharedViewModel,
    private val featureConfigRepository: FeatureConfigRepository,
    private val completionSyncRepository: com.islami.Aha.data.repository.CompletionSyncRepository,
    private val researchLogRepository: com.islami.Aha.data.research.ResearchLogRepository,
    private val activityStats: com.islami.Aha.data.research.ActivityStatsRepository,
    private val tilawahState: com.islami.Aha.data.research.TilawahStateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticUiState())
    val uiState: StateFlow<StatisticUiState> = _uiState.asStateFlow()

    private var statsJob: kotlinx.coroutines.Job? = null

    private val _selectedPastDate = MutableStateFlow<String?>(null)
    val selectedPastDate: StateFlow<String?> = _selectedPastDate.asStateFlow()

    private val _pastDayHabits = MutableStateFlow<List<PastDayHabitItem>>(emptyList())
    val pastDayHabits: StateFlow<List<PastDayHabitItem>> = _pastDayHabits.asStateFlow()

    private val _bulan = MutableStateFlow(YearMonth.now())
    private val _tanggal = MutableStateFlow(DateUtils.getTodayKey())
    /** Dinaikkan setelah catatan hari lampau diubah lewat lembar (tabel itu tidak punya Flow). */
    private val _muatUlang = MutableStateFlow(0)

    private val idLocale = Locale.forLanguageTag("id-ID")

    init {
        featureConfigRepository.refresh()
        loadStatistics()
    }

    fun bulanSebelumnya() { _bulan.update { it.minusMonths(1) } }

    fun bulanBerikutnya() { _bulan.update { b -> if (b < YearMonth.now()) b.plusMonths(1) else b } }

    fun pilihTanggal(tanggal: String) {
        if (tanggal > DateUtils.getTodayKey()) return
        _tanggal.value = tanggal
    }

    private fun loadStatistics() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            val sumber = combine(
                habitDao.getHabits(),
                sunnahHabitSharedViewModel.sunnahHabits,
                featureConfigRepository.featureConfig,
                com.islami.Aha.util.UserPreferencesManager.haidhDates,
                activityStats.observeChanges()
            ) { habits, sunnahHabits, config, haidhDates, researchTick ->
                StatCombineData(habits, sunnahHabits, config, haidhDates, researchTick)
            }
            combine(sumber, _bulan, _tanggal, tilawahState.version, _muatUlang) { data, bulan, tanggal, _, _ ->
                Triple(data, bulan, tanggal)
            }.collectLatest { (data, bulan, tanggal) -> hitung(data, bulan, tanggal) }
        }
    }

    private suspend fun hitung(data: StatCombineData, bulan: YearMonth, tanggal: String) {
        val habits = data.habits
        val sunnahHabits = data.sunnahHabits
        val config = data.config
        val haidhDates = data.haidhDates
        val todayKey = DateUtils.getTodayKey()
        val isTodayHaidh = haidhDates.contains(todayKey)
        val visibleHabits = habits.filter {
            if (it.category == "Puasa Wajib" && (!DateUtils.isRamadanMonth() || !config.puasaWajibRamadanEnabled)) return@filter false
            if (it.category == "Sholat Tarawih" && (!DateUtils.isRamadanMonth() || !config.sholatTarawihEnabled)) return@filter false
            if (isTodayHaidh && (it.category.startsWith("Sholat") || it.category.startsWith("Puasa"))) return@filter false
            true
        }
        val visibleSunnahHabits = sunnahHabits.filter {
            !(isTodayHaidh && (it.category == SunnahCategoryType.SHOLAT || it.category == SunnahCategoryType.PUASA))
        }

        // Hari ini: sholat/puasa harian + sunnah + dzikir pilihan. Tilawah/iqro di luar hitungan.
        val dzikir = activityStats.getDzikirProgress(todayKey, isTodayHaidh)
        val fardhuTotal = visibleHabits.count { it.category == "Sholat Fardhu" }
        val lainTotal = visibleHabits.size - fardhuTotal + visibleSunnahHabits.size
        val todayCompleted = visibleHabits.count { it.isCompleted } + visibleSunnahHabits.count { it.isCompletedToday } + dzikir.done
        val todayTotal = visibleHabits.size + visibleSunnahHabits.size + dzikir.total
        val rincian = listOfNotNull(
            fardhuTotal.takeIf { it > 0 }?.let { "$it sholat fardhu" },
            lainTotal.takeIf { it > 0 }?.let { "$it sunnah & puasa" },
            dzikir.total.takeIf { it > 0 }?.let { "$it dzikir pilihanmu" }
        ).joinToString(" + ")

        val namaHabit = habits.associate { "default_${it.id}" to it.name } +
            sunnahHabits.associate { "sunnah_${it.id}" to it.name }
        val urutanKunci = habits.map { "default_${it.id}" } + sunnahHabits.map { "sunnah_${it.id}" }

        // Kalender bulan.
        val awalBulan = bulan.atDay(1).toString()
        val akhirBulan = bulan.atEndOfMonth().toString()
        val jumlahPerTanggal = activityStats.getDailyCountsBetween(awalBulan, akhirBulan).associate { it.dateKey to it.count }
        val kalender = susunBulan(bulan, jumlahPerTanggal, haidhDates, LocalDate.now(), todayTotal)

        // Detail tanggal terpilih.
        val chips = susunCatatanHari(
            activityStats.getCompletionRecordsBetween(tanggal, tanggal),
            namaHabit,
            urutanKunci,
            activityStats.getAktivitasLainBetween(tanggal, tanggal)
        )

        // 7 hari terakhir.
        val tujuhHari = DateUtils.getDateKeysLastDays(7)
        val pekan = if (tujuhHari.isEmpty()) RingkasanPekan() else susunRingkasanPekan(
            activityStats.getCompletionRecordsBetween(tujuhHari.first(), tujuhHari.last()),
            namaHabit,
            activityStats.getAktivitasLainBetween(tujuhHari.first(), tujuhHari.last())
        )

        // Perjalanan khatam: posisi bacaan harian.
        val posisi = tilawahState.penanda(ResearchSchema.Detail.BACAAN_HARIAN)
        val khatamJuz = posisi?.let { QuranCalculator.juzOf(it) }
        val khatamKeterangan = posisi?.let {
            "Bacaan harian sampai ${com.islami.Aha.ui.home.components.namaSurah(it.surah)} ${it.ayat} · " +
                "halaman ${QuranCalculator.halamanOf(it)} dari ${QuranData.TOTAL_HALAMAN}"
        } ?: "Belum ada sesi bacaan harian. Catat di Beranda, tab Tilawah."

        val completionDates = activityStats.getDistinctCompletionDatesDesc()
        val tglDipilih = runCatching { LocalDate.parse(tanggal) }.getOrNull()

        _uiState.update {
            it.copy(
                isLoading = false,
                judulTanggal = "${getCurrentDateFormatted()} · ${DateUtils.getHijriDateFormatted()}",
                todayCompleted = todayCompleted,
                todayTotal = todayTotal,
                rincianHariIni = rincian,
                modeCuti = isTodayHaidh,
                currentStreak = calculateCurrentStreak(completionDates, haidhDates),
                bulan = bulan,
                labelBulan = bulan.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", idLocale)),
                labelHijriBulan = DateUtils.hijriRentang(bulan.atDay(1), bulan.atEndOfMonth()),
                bisaMaju = bulan < YearMonth.now(),
                kalender = kalender,
                tanggalDipilih = tanggal,
                judulTanggalDipilih = tglDipilih?.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", idLocale)) ?: tanggal,
                chipsDipilih = chips,
                tanggalDipilihCuti = haidhDates.contains(tanggal),
                pekan = pekan,
                punyaDzikir = activityStats.hasDzikirHabits(),
                punyaSunnah = sunnahHabits.isNotEmpty(),
                khatamJuz = khatamJuz,
                khatamKeterangan = khatamKeterangan
            )
        }
    }

    private fun getCurrentDateFormatted(): String =
        SimpleDateFormat("EEEE, d MMMM yyyy", idLocale).format(Date())

    private fun calculateCurrentStreak(distinctDatesDesc: List<String>, haidhDates: Set<String>): Int {
        if (distinctDatesDesc.isEmpty() && haidhDates.isEmpty()) return 0
        val dateSet = distinctDatesDesc.toSet()
        var cursor = LocalDate.now()
        var streak = 0
        if (!dateSet.contains(cursor.toString()) && !haidhDates.contains(cursor.toString())) {
            cursor = cursor.minusDays(1)
        }
        while (dateSet.contains(cursor.toString()) || haidhDates.contains(cursor.toString())) {
            if (dateSet.contains(cursor.toString())) streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun onDayClicked(dateKey: String) {
        _selectedPastDate.value = dateKey
        loadPastDayHabits(dateKey)
    }

    fun dismissPastDayPopup() {
        _selectedPastDate.value = null
        _pastDayHabits.value = emptyList()
    }

    private fun loadPastDayHabits(dateKey: String) {
        viewModelScope.launch {
            val fardhuHabits = habitDao.getHabits().firstOrNull() ?: emptyList()
            val sunnahHabitsList = sunnahHabitSharedViewModel.sunnahHabits.value
            val completions = habitCompletionDao.getRecordsForDate(dateKey)
            val completedKeys = completions.map { it.habitKey }.toSet()
            
            val haidhDates = com.islami.Aha.util.UserPreferencesManager.getHaidhDates()
            val isHaidh = haidhDates.contains(dateKey)

            val isJumatEnabled = com.islami.Aha.util.UserPreferencesManager.isJumatEnabled.value
            val isFriday = runCatching {
                val parsed = java.time.LocalDate.parse(dateKey)
                parsed.dayOfWeek == java.time.DayOfWeek.FRIDAY
            }.getOrDefault(false)

            val items = mutableListOf<PastDayHabitItem>()
            fardhuHabits.forEach { habit ->
                if (isHaidh && (habit.category.startsWith("Sholat") || habit.category.startsWith("Puasa"))) return@forEach
                val key = "default_${habit.id}"
                var displayTitle = habit.name
                if (isJumatEnabled && isFriday && habit.name == "Sholat Dzuhur") {
                    displayTitle = "Sholat Jumat"
                }
                
                items.add(PastDayHabitItem(
                    habitKey = key,
                    name = displayTitle,
                    isCompleted = completedKeys.contains(key),
                    category = habit.category,
                    isSunnah = false
                ))
            }
            sunnahHabitsList.forEach { sunnah ->
                if (isHaidh && (sunnah.category == com.islami.Aha.ui.addhabit.SunnahCategoryType.SHOLAT || sunnah.category == com.islami.Aha.ui.addhabit.SunnahCategoryType.PUASA)) return@forEach
                val key = "sunnah_${sunnah.id}"
                items.add(PastDayHabitItem(
                    habitKey = key,
                    name = sunnah.name,
                    isCompleted = completedKeys.contains(key),
                    category = sunnah.category.name,
                    isSunnah = true
                ))
            }
            // Urutan habit dipertahankan (Subuh..Isya, lalu sunnah); fardhu didahulukan.
            _pastDayHabits.value = items.sortedBy { if (it.category == "Sholat Fardhu") 0 else 1 }
        }
    }

    fun togglePastDayHabitCompletion(item: PastDayHabitItem) {
        val dateKey = _selectedPastDate.value ?: return
        viewModelScope.launch {
            val willComplete = !item.isCompleted
            val record = com.islami.Aha.data.model.HabitCompletionRecord(
                habitKey = item.habitKey,
                dateKey = dateKey,
                category = item.category,
                source = if (item.isSunnah) "SUNNAH" else "DEFAULT"
            )
            // tanggal = hari ibadahnya; jam_pencatatan = sekarang (pencatatan susulan, keputusan F).
            val researchSlot = if (item.isSunnah) {
                com.islami.Aha.data.research.ResearchLogRepository.slotForSunnahHabit(
                    item.habitKey.removePrefix("sunnah_"), item.name, item.category
                )
            } else {
                com.islami.Aha.data.research.ResearchLogRepository.slotForDefaultHabit(item.name, item.category)
            }
            if (willComplete) {
                habitCompletionDao.insert(record)
                researchLogRepository.recordDikerjakan(researchSlot, dateKey)
                researchLogRepository.scheduleUpload()
                // Sync to Cloud
                launch { 
                    val status = completionSyncRepository.syncAdd(record)
                    if (status.shouldRetry) scheduleSyncCompletionsWork()
                }
            } else {
                habitCompletionDao.deleteByHabitAndDate(item.habitKey, dateKey)
                researchLogRepository.remove(researchSlot, dateKey)
                researchLogRepository.scheduleUpload()
                // Sync to Cloud
                launch { 
                    val status = completionSyncRepository.syncDelete(item.habitKey, dateKey)
                    if (status.shouldRetry) scheduleSyncCompletionsWork()
                }
            }
            // Reload the list to reflect changes
            loadPastDayHabits(dateKey)
            // Muat ulang statistik karena catatan hari lampau berubah
            _muatUlang.update { it + 1 }
        }
    }

    private fun scheduleSyncCompletionsWork() {
        val request = androidx.work.OneTimeWorkRequestBuilder<com.islami.Aha.worker.SyncCompletionsWorker>()
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_habit_completions",
            androidx.work.ExistingWorkPolicy.KEEP,
            request
        )
    }
}

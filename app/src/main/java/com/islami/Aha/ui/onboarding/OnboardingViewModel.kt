package com.islami.Aha.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.islami.Aha.data.dzikir.DzikirHabitDraft
import com.islami.Aha.data.dzikir.DzikirHabitRepository
import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.data.research.ResearchLogRepository
import com.islami.Aha.domain.model.SunnahHabit
import com.islami.Aha.util.AgeRange
import com.islami.Aha.util.GenderProfile
import com.islami.Aha.util.StatusAktivitas
import com.islami.Aha.util.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Menyimpan pilihan onboarding. Semuanya lokal; bila pengguna login, profil & usia didorong
 * ke users/{uid}/meta/profile oleh alur login di HomeViewModel, dan (bila setuju penelitian)
 * profil penelitian ke users/{uid} oleh worker unggah log.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val researchLogRepository: ResearchLogRepository,
    private val userHabitRepository: UserHabitRepository,
    private val dzikirHabitRepository: DzikirHabitRepository
) : ViewModel() {

    fun simpanProfil(profil: GenderProfile) {
        UserPreferencesManager.setGender(profil)
        UserPreferencesManager.setHasSeenPrompt()
    }

    fun simpanUsia(usia: AgeRange, izinWali: Boolean, status: StatusAktivitas) {
        UserPreferencesManager.setAgeRange(usia, izinWali)
        UserPreferencesManager.setStatusAktivitas(status)
    }

    fun simpanPersetujuan(setuju: Boolean) {
        researchLogRepository.setResearchConsent(setuju)
    }

    /**
     * Simpan snapshot pilihan awal (kosong = "belum ada yang rutin"), lalu buat habit sunnah
     * dan dzikir yang dicentang, seperti lewat Add Habit (tanpa pengingat). Habit yang sudah
     * ada tidak dibuat dua kali. Tetap jalan walau layar onboarding sudah ditutup.
     */
    fun simpanPilihanAwal(pilihan: Set<String>) {
        UserPreferencesManager.setPilihanAwal(pilihan)
        researchLogRepository.scheduleUpload()
        val opsi = OPSI_PILIHAN_AWAL.filter { it.kode in pilihan }
        if (opsi.isEmpty()) return
        viewModelScope.launch(NonCancellable) {
            runCatching {
                val namaAda = userHabitRepository.getAllHabits().first().map { it.name.lowercase() }.toSet()
                opsi.mapNotNull { it.sunnah }.filter { it.nama.lowercase() !in namaAda }.forEach { h ->
                    userHabitRepository.insertHabit(
                        SunnahHabit(name = h.nama, category = h.kategori, frequencyLabel = h.frekuensi)
                    )
                }
                val slugAda = dzikirHabitRepository.getAll().map { it.slug }.toSet()
                val katalog = dzikirHabitRepository.katalog.map { it.slug }.toSet()
                val drafts = opsi.filter { it.kelompok == KelompokPilihan.DZIKIR }
                    .map { it.kode }
                    .filter { it in katalog && it !in slugAda }
                    .map { DzikirHabitDraft(slug = it) }
                if (drafts.isNotEmpty()) dzikirHabitRepository.addAll(drafts)
            }.onFailure { com.islami.Aha.util.logCrashlyticsSafe(it) }
        }
    }
}

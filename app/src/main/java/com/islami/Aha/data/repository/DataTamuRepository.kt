package com.islami.Aha.data.repository

import com.islami.Aha.data.local.DzikirHabitDao
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.research.ResearchLogRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data tamu (akun anonim) di HP, untuk peringatan sebelum tamu masuk ke akun yang SUDAH ADA
 * (README, keputusan 2): habit apa saja yang akan hilang dari HP.
 */
@Singleton
class DataTamuRepository @Inject constructor(
    private val userHabitDao: UserHabitDao,
    private val dzikirHabitDao: DzikirHabitDao,
    private val habitCompletionDao: HabitCompletionDao,
    private val researchLogRepository: ResearchLogRepository,
    private val completionSyncRepository: CompletionSyncRepository
) {
    data class Ringkasan(val namaHabit: List<String>, val jumlahHabitDzikir: Int, val jumlahCatatan: Int) {
        val kosong: Boolean get() = namaHabit.isEmpty() && jumlahHabitDzikir == 0 && jumlahCatatan == 0

        /** Baris-baris untuk dialog peringatan. */
        fun baris(): List<String> = buildList {
            addAll(namaHabit)
            if (jumlahHabitDzikir > 0) add("$jumlahHabitDzikir habit dzikir")
            if (jumlahCatatan > 0) add("$jumlahCatatan catatan ibadah")
        }
    }

    suspend fun ringkasan(): Ringkasan = Ringkasan(
        namaHabit = userHabitDao.getAllHabitsSnapshot().map { it.name },
        jumlahHabitDzikir = dzikirHabitDao.getAll().size,
        jumlahCatatan = habitCompletionDao.getTotalCompletionCount()
    )

    /**
     * Sebelum pindah UID: kirim yang masih tertunda selagi masih masuk sebagai tamu, supaya data
     * selama menjadi tamu tetap di dataset dengan UID anonimnya. Gagal (offline) = data itu hilang.
     */
    suspend fun kirimSebelumPindah() {
        runCatching { researchLogRepository.syncPending() }
        runCatching { completionSyncRepository.syncPendingRecords() }
    }
}

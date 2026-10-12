package com.islami.Aha.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.islami.Aha.data.model.ResearchLogEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Migrasi diuji pada database yang BERISI DATA, bukan database kosong.
 * Syarat lulus: setiap baris & kolom tabel lama identik, dan keluaran yang dibaca
 * heatmap/streak (query DAO + algoritma streak) sama persis sebelum dan sesudah.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val allMigrations = arrayOf(
        AppDatabase.MIGRATION_5_6,
        AppDatabase.MIGRATION_6_7,
        AppDatabase.MIGRATION_7_8,
        AppDatabase.MIGRATION_8_9,
        AppDatabase.MIGRATION_9_10,
        AppDatabase.MIGRATION_10_11,
        AppDatabase.MIGRATION_11_12,
        AppDatabase.MIGRATION_12_13,
        AppDatabase.MIGRATION_13_14,
        AppDatabase.MIGRATION_14_15,
        AppDatabase.MIGRATION_15_16
    )

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private val today: LocalDate = LocalDate.now()
    private fun day(daysAgo: Long) = today.minusDays(daysAgo).toString()

    // Pola 90 hari: 0-10 isi, 11-15 cuti (kosong), 16-30 isi, 31 bolong, 32-89 isi kecuali tiap hari ke-7.
    private val haidhDates = (11L..15L).map(::day).toSet()
    private val filledDaysAgo = (0L..10L) + (16L..30L) + (32L..89L).filter { it % 7L != 0L }
    private val expectedCurrentStreak = 11 + 15 // 0-10 lalu menyeberang cuti ke 16-30, putus di 31

    private val tablesV13 = listOf(
        "habits" to "id",
        "user_habits" to "id",
        "habit_completion_records" to "id",
        "hadith_contents" to "id",
        "surah_verses" to "id",
        "deleted_sync_records" to "id"
    )

    // ---------- 13 -> 14 pada data realistis ----------

    @Test
    fun migrate13To14_keepsEveryRowAndHeatmapStreakInputs() {
        val name = "migration-13-14"
        helper.createDatabase(name, 13).apply {
            seedHabits(this)
            seedUserHabits(this, withSyncColumns = true)
            seedCompletions(this, withIsSynced = true)
            seedContent(this)
            insert(this, "deleted_sync_records", "recordId" to "2026-01-01_default_1",
                "recordType" to "HABIT_COMPLETION", "deletedAt" to 1_700_000_000_000L)
            insert(this, "deleted_sync_records", "recordId" to "abc-123",
                "recordType" to "SUNNAH_HABIT", "deletedAt" to 1_700_000_100_000L)
            close()
        }

        val before = helper.runMigrationsAndValidate(name, 13, false).use { db ->
            Snapshot(tablesV13.associate { (t, pk) -> t to dumpTable(db, t, pk) }, heatmapInputs(db))
        }
        assertTrue("data uji harus berisi", before.tables.getValue("habit_completion_records").size > 150)

        val after = helper.runMigrationsAndValidate(name, 14, true, AppDatabase.MIGRATION_13_14).use { db ->
            Snapshot(tablesV13.associate { (t, pk) -> t to dumpTable(db, t, pk) }, heatmapInputs(db))
        }

        tablesV13.forEach { (t, _) ->
            assertEquals("isi tabel $t berubah", before.tables.getValue(t), after.tables.getValue(t))
        }
        assertEquals(before.heatmap, after.heatmap)

        // Lewat Room asli (builder & DAO yang dipakai aplikasi).
        withRoom(name) { room ->
            assertEquals(before.heatmap, heatmapInputsViaDao(room))
            val streak = currentStreakLikeStatisticViewModel(
                runBlocking { room.habitCompletionDao().getDistinctCompletionDatesDesc() }, haidhDates
            )
            assertEquals(expectedCurrentStreak, streak)
            assertEquals(
                currentStreakLikeStatisticViewModel(before.heatmap.distinctDatesDesc, haidhDates),
                streak
            )
            assertTrue(runBlocking { room.researchLogDao().getAll() }.isEmpty())
            assertResearchTableUsable(room)
        }
    }

    // ---------- rantai untuk pengguna yang melompati versi ----------

    @Test
    fun migrate9To14_keepsData() {
        val name = "migration-9-14"
        helper.createDatabase(name, 9).apply {
            seedHabits(this)
            seedUserHabits(this, withSyncColumns = false)
            seedCompletions(this, withIsSynced = false)
            close()
        }
        val cols = mapOf(
            "habits" to "id,name,category,icon,description,isCompleted,streak,time,isReminderEnabled,isCustom",
            "user_habits" to "id,name,category,frequencyLabel,rakaat,reminderEnabled,reminderTime,completedDateKey,createdAt",
            "habit_completion_records" to "id,habitKey,dateKey,category,source"
        )
        val before = helper.runMigrationsAndValidate(name, 9, false).use { db ->
            cols.mapValues { (t, c) -> dumpQuery(db, "SELECT $c FROM $t ORDER BY id") } to heatmapInputs(db)
        }
        val after = helper.runMigrationsAndValidate(name, 14, true, *allMigrations).use { db ->
            // Kolom baru dari migrasi lama terisi default yang benar.
            dumpQuery(db, "SELECT DISTINCT isSynced FROM habit_completion_records").let {
                assertEquals(listOf(listOf<Any?>(0L)), it)
            }
            assertEquals(0L, scalar(db, "SELECT COUNT(*) FROM user_habits WHERE updatedAt != createdAt OR lastSyncedAt != 0"))
            cols.mapValues { (t, c) -> dumpQuery(db, "SELECT $c FROM $t ORDER BY id") } to heatmapInputs(db)
        }
        assertEquals(before.first, after.first)
        assertEquals(before.second, after.second)
        withRoom(name) { room ->
            assertEquals(before.second, heatmapInputsViaDao(room))
            assertResearchTableUsable(room)
        }
    }

    @Test
    fun migrate12To14_keepsData() {
        val name = "migration-12-14"
        val tables12 = tablesV13.filter { it.first != "deleted_sync_records" }
        helper.createDatabase(name, 12).apply {
            seedHabits(this)
            seedUserHabits(this, withSyncColumns = true)
            seedCompletions(this, withIsSynced = true)
            seedContent(this)
            close()
        }
        val before = helper.runMigrationsAndValidate(name, 12, false).use { db ->
            tables12.associate { (t, pk) -> t to dumpTable(db, t, pk) } to heatmapInputs(db)
        }
        val after = helper.runMigrationsAndValidate(
            name, 14, true, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14
        ).use { db ->
            assertEquals(0L, scalar(db, "SELECT COUNT(*) FROM deleted_sync_records"))
            tables12.associate { (t, pk) -> t to dumpTable(db, t, pk) } to heatmapInputs(db)
        }
        assertEquals(before.first, after.first)
        assertEquals(before.second, after.second)
        withRoom(name) { room -> assertEquals(before.second, heatmapInputsViaDao(room)) }
    }

    // ---------- 14 -> 15: kolom dzikir di research_logs ----------

    @Test
    fun migrate14To15_keepsEveryRowAndAddsNullDzikirColumns() {
        val name = "migration-14-15"
        val tables = tablesV13 + ("research_logs" to "slotKey")
        helper.createDatabase(name, 14).apply {
            seedHabits(this)
            seedUserHabits(this, withSyncColumns = true)
            seedCompletions(this, withIsSynced = true)
            seedContent(this)
            insert(this, "deleted_sync_records", "recordId" to "x_research", "recordType" to "RESEARCH_LOG",
                "deletedAt" to 1_700_000_000_000L)
            // Baris research_logs v14 (sholat, dzikir lama, tilawah rentang, iqro).
            insert(this, "research_logs", "slotKey" to "${day(0)}_sholat_wajib_subuh", "tanggal" to day(0),
                "jenisIbadah" to "sholat_wajib", "namaDetail" to "subuh", "namaDetailAsli" to "Sholat Subuh",
                "status" to "dikerjakan", "jumlah" to null, "jilid" to null, "jamPencatatan" to 1_791_600_000_000L,
                "sumberPencatatan" to "tidak_diketahui", "ownerUid" to "uidA", "isSynced" to 1L, "updatedAt" to 5L,
                "jumlahAyat" to null, "juz" to null, "offsetWaktu" to "+07:00", "selisihMenitDariAdzan" to 12L)
            insert(this, "research_logs", "slotKey" to "${day(0)}_tilawah_bacaan_harian_s1", "tanggal" to day(0),
                "jenisIbadah" to "tilawah", "namaDetail" to "bacaan_harian", "namaDetailAsli" to null,
                "status" to "dikerjakan", "jumlah" to 2L, "jilid" to null, "jamPencatatan" to 1_791_600_100_000L,
                "sumberPencatatan" to "tidak_diketahui", "ownerUid" to null, "isSynced" to 0L, "updatedAt" to 6L,
                "jumlahAyat" to 12L, "juz" to 1L, "offsetWaktu" to "+08:00", "selisihMenitDariAdzan" to null)
            insert(this, "research_logs", "slotKey" to "${day(1)}_iqro", "tanggal" to day(1),
                "jenisIbadah" to "iqro", "namaDetail" to null, "namaDetailAsli" to null,
                "status" to "dikerjakan", "jumlah" to 7L, "jilid" to 2L, "jamPencatatan" to 1_791_500_000_000L,
                "sumberPencatatan" to "tidak_diketahui", "ownerUid" to "uidA", "isSynced" to 1L, "updatedAt" to 7L,
                "jumlahAyat" to null, "juz" to null, "offsetWaktu" to "+09:00", "selisihMenitDariAdzan" to null)
            close()
        }
        val before = helper.runMigrationsAndValidate(name, 14, false).use { db ->
            tables.associate { (t, pk) -> t to dumpTable(db, t, pk) } to heatmapInputs(db)
        }
        val v14Cols = "slotKey,tanggal,jenisIbadah,namaDetail,namaDetailAsli,status,jumlah,jilid,jamPencatatan," +
            "sumberPencatatan,ownerUid,isSynced,updatedAt,jumlahAyat,juz,offsetWaktu,selisihMenitDariAdzan"
        val after = helper.runMigrationsAndValidate(name, 15, true, AppDatabase.MIGRATION_14_15).use { db ->
            assertEquals(0L, scalar(db,
                "SELECT COUNT(*) FROM research_logs WHERE modePencatatan IS NOT NULL OR waktuDzikir IS NOT NULL " +
                    "OR targetHitungan IS NOT NULL OR durasiDetik IS NOT NULL"))
            val old = tablesV13.associate { (t, pk) -> t to dumpTable(db, t, pk) } +
                ("research_logs" to dumpQuery(db, "SELECT $v14Cols FROM research_logs ORDER BY slotKey"))
            old to heatmapInputs(db)
        }
        assertEquals(3, before.first.getValue("research_logs").size)
        tables.forEach { (t, _) -> assertEquals("isi tabel $t berubah", before.first.getValue(t), after.first.getValue(t)) }
        assertEquals(before.second, after.second)
        withRoom(name) { room ->
            assertEquals(before.second, heatmapInputsViaDao(room))
            assertEquals(3, runBlocking { room.researchLogDao().getAll() }.size)
        }
    }

    @Test
    fun migrate13To15_chain() {
        val name = "migration-13-15"
        helper.createDatabase(name, 13).apply {
            seedHabits(this); seedUserHabits(this, withSyncColumns = true); seedCompletions(this, withIsSynced = true)
            close()
        }
        val before = helper.runMigrationsAndValidate(name, 13, false).use { db ->
            tablesV13.take(3).associate { (t, pk) -> t to dumpTable(db, t, pk) }
        }
        val after = helper.runMigrationsAndValidate(
            name, 15, true, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15
        ).use { db -> tablesV13.take(3).associate { (t, pk) -> t to dumpTable(db, t, pk) } }
        assertEquals(before, after)
    }

    // ---------- 15 -> 16: tabel dzikir_habits baru, tabel lain tidak berubah ----------

    @Test
    fun migrate15To16_addsEmptyDzikirHabits_researchLogsUntouched() {
        val name = "migration-15-16"
        val tables = tablesV13 + ("research_logs" to "slotKey")
        helper.createDatabase(name, 15).apply {
            seedHabits(this); seedUserHabits(this, withSyncColumns = true); seedCompletions(this, withIsSynced = true)
            seedContent(this)
            // Baris dari kartu dzikir lama (id habit kartu-pagi) + tilawah.
            insert(this, "research_logs", "slotKey" to "${day(0)}_dzikir_dzikir_pagi_kartu-pagi_1791600000000",
                "tanggal" to day(0), "jenisIbadah" to "dzikir", "namaDetail" to "dzikir_pagi", "namaDetailAsli" to null,
                "status" to "dikerjakan", "jumlah" to null, "jilid" to null, "jamPencatatan" to 1_791_600_000_000L,
                "sumberPencatatan" to "tidak_diketahui", "ownerUid" to "uidA", "isSynced" to 1L, "updatedAt" to 5L,
                "jumlahAyat" to null, "juz" to null, "offsetWaktu" to "+07:00", "selisihMenitDariAdzan" to 3L,
                "modePencatatan" to "centang", "waktuDzikir" to "pagi", "targetHitungan" to null, "durasiDetik" to null)
            insert(this, "research_logs", "slotKey" to "${day(1)}_tilawah_hafalan_s9", "tanggal" to day(1),
                "jenisIbadah" to "tilawah", "namaDetail" to "hafalan", "namaDetailAsli" to null,
                "status" to "dikerjakan", "jumlah" to 1L, "jilid" to null, "jamPencatatan" to 1_791_500_000_000L,
                "sumberPencatatan" to "tidak_diketahui", "ownerUid" to null, "isSynced" to 0L, "updatedAt" to 6L,
                "jumlahAyat" to 5L, "juz" to 30L, "offsetWaktu" to "+07:00", "selisihMenitDariAdzan" to null,
                "modePencatatan" to null, "waktuDzikir" to null, "targetHitungan" to null, "durasiDetik" to null)
            close()
        }
        val before = helper.runMigrationsAndValidate(name, 15, false).use { db ->
            tables.associate { (t, pk) -> t to dumpTable(db, t, pk) }
        }
        val after = helper.runMigrationsAndValidate(name, 16, true, AppDatabase.MIGRATION_15_16).use { db ->
            assertEquals(0L, scalar(db, "SELECT COUNT(*) FROM dzikir_habits"))
            tables.associate { (t, pk) -> t to dumpTable(db, t, pk) }
        }
        assertEquals(2, before.getValue("research_logs").size)
        tables.forEach { (t, _) -> assertEquals("isi tabel $t berubah", before.getValue(t), after.getValue(t)) }
        withRoom(name) { room ->
            assertEquals(2, runBlocking { room.researchLogDao().getAll() }.size)
            assertTrue(runBlocking { room.dzikirHabitDao().getAll() }.isEmpty())
        }
    }

    @Test
    fun migrate13To16_chain() {
        val name = "migration-13-16"
        helper.createDatabase(name, 13).apply {
            seedHabits(this); seedUserHabits(this, withSyncColumns = true); seedCompletions(this, withIsSynced = true)
            close()
        }
        val before = helper.runMigrationsAndValidate(name, 13, false).use { db ->
            tablesV13.take(3).associate { (t, pk) -> t to dumpTable(db, t, pk) }
        }
        val after = helper.runMigrationsAndValidate(
            name, 16, true, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16
        ).use { db -> tablesV13.take(3).associate { (t, pk) -> t to dumpTable(db, t, pk) } }
        assertEquals(before, after)
    }

    // ---------- data uji ----------

    private val fardhu = listOf("Sholat Subuh", "Sholat Dzuhur", "Sholat Ashar", "Sholat Maghrib", "Sholat Isya")

    private fun seedHabits(db: SupportSQLiteDatabase) {
        val times = listOf("04:30", "11:55", "15:10", "18:00", "19:15")
        fardhu.forEachIndexed { i, n ->
            insert(db, "habits", "id" to (i + 1L), "name" to n, "category" to "Sholat Fardhu",
                "icon" to "sun", "description" to "", "isCompleted" to (if (i < 2) 1L else 0L),
                "streak" to 0L, "time" to times[i], "isReminderEnabled" to 1L, "isCustom" to 0L)
        }
        insert(db, "habits", "id" to 6L, "name" to "Puasa Ramadan", "category" to "Puasa Wajib",
            "icon" to "plate", "description" to "Puasa wajib di bulan Ramadan", "isCompleted" to 0L,
            "streak" to 0L, "time" to "", "isReminderEnabled" to 0L, "isCustom" to 0L)
        insert(db, "habits", "id" to 7L, "name" to "Sholat Tarawih", "category" to "Sholat Tarawih",
            "icon" to "moon", "description" to "", "isCompleted" to 0L,
            "streak" to 0L, "time" to "20:00", "isReminderEnabled" to 1L, "isCustom" to 0L)
    }

    private val sunnah = listOf(
        Triple("11111111-aaaa-4bbb-8ccc-000000000001", "Dhuha", "SHOLAT"),
        Triple("11111111-aaaa-4bbb-8ccc-000000000002", "Senin Kamis", "PUASA"),
        Triple("11111111-aaaa-4bbb-8ccc-000000000003", "Sholat Hajat ☪ buatan sendiri", "SHOLAT"),
        Triple("11111111-aaaa-4bbb-8ccc-000000000004", "Qadha", "SHOLAT")
    )

    private fun seedUserHabits(db: SupportSQLiteDatabase, withSyncColumns: Boolean) {
        sunnah.forEachIndexed { i, (id, n, c) ->
            val created = 1_690_000_000_000L + i * 1000L
            val values = mutableListOf<Pair<String, Any?>>(
                "id" to id, "name" to n, "category" to c, "frequencyLabel" to "Setiap hari",
                "rakaat" to (if (c == "SHOLAT") 2L else null), "reminderEnabled" to (i % 2).toLong(),
                "reminderTime" to (if (i % 2 == 1) "05:30" else null),
                "completedDateKey" to (if (i == 0) day(0) else null), "createdAt" to created
            )
            if (withSyncColumns) {
                values += "updatedAt" to created + 500L
                values += "lastSyncedAt" to (if (i % 2 == 0) created + 900L else 0L)
            }
            insert(db, "user_habits", *values.toTypedArray())
        }
    }

    private fun seedCompletions(db: SupportSQLiteDatabase, withIsSynced: Boolean) {
        var n = 0L
        filledDaysAgo.forEach { ago ->
            val date = day(ago)
            val fardhuCount = 1 + (ago % 5).toInt()
            (1..fardhuCount).forEach { id ->
                n++
                val values = mutableListOf<Pair<String, Any?>>(
                    "habitKey" to "default_$id", "dateKey" to date,
                    "category" to "Sholat Fardhu", "source" to "DEFAULT"
                )
                if (withIsSynced) values += "isSynced" to (n % 3 != 0L).let { if (it) 1L else 0L }
                insert(db, "habit_completion_records", *values.toTypedArray())
            }
            if (ago % 3 == 0L) {
                val (id, _, c) = sunnah[(ago % sunnah.size).toInt()]
                val values = mutableListOf<Pair<String, Any?>>(
                    "habitKey" to "sunnah_$id", "dateKey" to date,
                    "category" to (if (c == "SHOLAT") "Sholat Sunnah" else "Puasa Sunnah"), "source" to "SUNNAH"
                )
                if (withIsSynced) values += "isSynced" to 0L
                insert(db, "habit_completion_records", *values.toTypedArray())
            }
        }
    }

    private fun seedContent(db: SupportSQLiteDatabase) {
        insert(db, "hadith_contents", "id" to "h1", "text" to "Amal yang paling dicintai Allah...", "source" to "HR. Bukhari")
        insert(db, "hadith_contents", "id" to "h2", "text" to "Sholat berjamaah 27 derajat", "source" to "HR. Muslim")
        insert(db, "surah_verses", "id" to "s1", "surahName" to "Al-Baqarah", "ayahNumber" to 286L, "translation" to "Allah tidak membebani...")
        insert(db, "surah_verses", "id" to "s2", "surahName" to "Al-Insyirah", "ayahNumber" to 6L, "translation" to "Sesungguhnya bersama kesulitan...")
    }

    // ---------- pembanding ----------

    private data class Snapshot(val tables: Map<String, List<List<Any?>>>, val heatmap: HeatmapInputs)

    /** Semua yang dibaca layar statistik/profil dari habit_completion_records. */
    private data class HeatmapInputs(
        val dailyCounts: List<Pair<String, Int>>,
        val categoryCounts: List<Pair<String, Int>>,
        val distinctDatesDesc: List<String>,
        val total: Int,
        val todayRecords: List<String>
    )

    // SQL sama persis dengan anotasi di HabitCompletionDao.
    private fun heatmapInputs(db: SupportSQLiteDatabase): HeatmapInputs {
        val start = day(89); val end = day(0)
        return HeatmapInputs(
            dailyCounts = dumpQuery(db, "SELECT dateKey, COUNT(*) as count FROM habit_completion_records WHERE dateKey BETWEEN '$start' AND '$end' GROUP BY dateKey")
                .map { it[0] as String to (it[1] as Long).toInt() }.sortedBy { it.first },
            categoryCounts = dumpQuery(db, "SELECT category, COUNT(*) as count FROM habit_completion_records WHERE dateKey BETWEEN '$start' AND '$end' GROUP BY category ORDER BY count DESC")
                .map { it[0] as String to (it[1] as Long).toInt() }.sortedBy { it.first },
            distinctDatesDesc = dumpQuery(db, "SELECT DISTINCT dateKey FROM habit_completion_records ORDER BY dateKey DESC").map { it[0] as String },
            total = scalar(db, "SELECT COUNT(*) FROM habit_completion_records").toInt(),
            todayRecords = dumpQuery(db, "SELECT habitKey FROM habit_completion_records WHERE dateKey = '$end'").map { it[0] as String }.sorted()
        )
    }

    private fun heatmapInputsViaDao(room: AppDatabase): HeatmapInputs = runBlocking {
        val dao = room.habitCompletionDao()
        val start = day(89); val end = day(0)
        HeatmapInputs(
            dailyCounts = dao.getDailyCountsBetween(start, end).map { it.dateKey to it.count }.sortedBy { it.first },
            categoryCounts = dao.getCategoryCountsBetween(start, end).map { it.category to it.count }.sortedBy { it.first },
            distinctDatesDesc = dao.getDistinctCompletionDatesDesc(),
            total = dao.getTotalCompletionCount(),
            todayRecords = dao.getRecordsForDate(end).map { it.habitKey }.sorted()
        )
    }

    /** Salinan persis StatisticViewModel/ProfileViewModel.calculateCurrentStreak. */
    private fun currentStreakLikeStatisticViewModel(distinctDatesDesc: List<String>, haidhDates: Set<String>): Int {
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

    private fun assertResearchTableUsable(room: AppDatabase) = runBlocking {
        val dao = room.researchLogDao()
        val row = ResearchLogEntity(
            slotKey = "${day(0)}_sholat_wajib_subuh", tanggal = day(0), jenisIbadah = "sholat_wajib",
            namaDetail = "subuh", namaDetailAsli = "Sholat Subuh", status = "dikerjakan",
            jumlah = null, jilid = null, jamPencatatan = 1L, sumberPencatatan = "tidak_diketahui",
            ownerUid = null, updatedAt = 1L, offsetWaktu = "+07:00"
        )
        dao.upsert(row)
        assertEquals(row, dao.getBySlotKey(row.slotKey))
        // Log penelitian tidak boleh ikut terhitung heatmap.
        val totalBefore = room.habitCompletionDao().getTotalCompletionCount()
        dao.upsert(row.copy(slotKey = "${day(0)}_sholat_wajib_isya", namaDetail = "isya",
            status = "tidak", jamPencatatan = null, sumberPencatatan = null))
        assertEquals(totalBefore, room.habitCompletionDao().getTotalCompletionCount())
        dao.deleteBySlotKey(row.slotKey)
        assertNull(dao.getBySlotKey(row.slotKey))
    }

    // ---------- util ----------

    private fun withRoom(name: String, block: (AppDatabase) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val room = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*allMigrations)
            .allowMainThreadQueries()
            .build()
        try {
            room.openHelper.writableDatabase // memicu validasi skema Room
            block(room)
        } finally {
            room.close()
        }
    }

    private fun insert(db: SupportSQLiteDatabase, table: String, vararg values: Pair<String, Any?>) {
        val cv = ContentValues()
        values.forEach { (k, v) ->
            when (v) {
                null -> cv.putNull(k)
                is String -> cv.put(k, v)
                is Long -> cv.put(k, v)
                else -> error("tipe tak didukung: $v")
            }
        }
        db.insert(table, SQLiteDatabase.CONFLICT_ABORT, cv)
    }

    private fun dumpTable(db: SupportSQLiteDatabase, table: String, pk: String) =
        dumpQuery(db, "SELECT * FROM $table ORDER BY $pk")

    private fun dumpQuery(db: SupportSQLiteDatabase, sql: String): List<List<Any?>> =
        db.query(sql).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add((0 until c.columnCount).map { i -> c.valueAt(i) })
                }
            }
        }

    private fun Cursor.valueAt(i: Int): Any? = when (getType(i)) {
        Cursor.FIELD_TYPE_NULL -> null
        Cursor.FIELD_TYPE_INTEGER -> getLong(i)
        Cursor.FIELD_TYPE_FLOAT -> getDouble(i)
        Cursor.FIELD_TYPE_BLOB -> getBlob(i).toList()
        else -> getString(i)
    }

    private fun scalar(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { c -> c.moveToFirst(); c.getLong(0) }
}

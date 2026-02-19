package com.islami.Aha.data.repository

import com.islami.Aha.data.local.DailyIslamicContentDao
import com.islami.Aha.data.model.HadithContentEntity
import com.islami.Aha.data.model.SurahVerseEntity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class DailyIslamicContent(
    val hadithText: String,
    val hadithSource: String,
    val surahMeaning: String,
    val surahReference: String
)

@Singleton
class DailyIslamicContentRepository @Inject constructor(
    private val contentDao: DailyIslamicContentDao
) {
    companion object {
        private const val MIN_SYNC_INTERVAL_MS = 60 * 60 * 1000L
        private const val HADITH_COLLECTION = "hadith_contents"
        private const val SURAH_COLLECTION = "surah_verses"
    }

    private val firestore: FirebaseFirestore? by lazy {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }
    private val syncMutex = Mutex()
    private var lastSyncAtMs: Long = 0L

    suspend fun getDailyContent(): DailyIslamicContent {
        ensureSeeded()

        val hadith = contentDao.getRandomHadith() ?: defaultHadiths.first()
        val surah = contentDao.getRandomSurahVerse() ?: defaultSurahVerses.first()

        return DailyIslamicContent(
            hadithText = hadith.text,
            hadithSource = hadith.source,
            surahMeaning = surah.translation,
            surahReference = "${surah.surahName}:${surah.ayahNumber}"
        )
    }

    suspend fun syncFromCloudIfAvailable(): Boolean {
        return syncMutex.withLock {
            val now = System.currentTimeMillis()
            if (now - lastSyncAtMs < MIN_SYNC_INTERVAL_MS) return@withLock false

            val cloud = firestore ?: return@withLock false
            lastSyncAtMs = now

            val remoteHadith = runCatching {
                cloud.collection(HADITH_COLLECTION).get().await().documents.mapNotNull { doc ->
                    val text = doc.getString("text").orEmpty().trim()
                    val source = doc.getString("source").orEmpty().trim()
                    if (text.isBlank() || source.isBlank()) return@mapNotNull null
                    HadithContentEntity(
                        id = doc.id.ifBlank { "remote_hadith_${text.hashCode()}" },
                        text = text,
                        source = source
                    )
                }
            }.getOrElse { error ->
                lastSyncAtMs = 0L
                runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
                return@withLock false
            }

            val remoteSurah = runCatching {
                cloud.collection(SURAH_COLLECTION).get().await().documents.mapNotNull { doc ->
                    val surahName = doc.getString("surahName").orEmpty().trim()
                    val ayah = doc.getLong("ayahNumber")?.toInt() ?: return@mapNotNull null
                    val translation = doc.getString("translation").orEmpty().trim()
                    if (surahName.isBlank() || translation.isBlank()) return@mapNotNull null
                    SurahVerseEntity(
                        id = doc.id.ifBlank { "remote_surah_${surahName}_${ayah}" },
                        surahName = surahName,
                        ayahNumber = ayah,
                        translation = translation
                    )
                }
            }.getOrElse { error ->
                lastSyncAtMs = 0L
                runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
                return@withLock false
            }

            var changed = false
            if (remoteHadith.isNotEmpty()) {
                contentDao.replaceHadith(remoteHadith)
                changed = true
            }
            if (remoteSurah.isNotEmpty()) {
                contentDao.replaceSurahVerses(remoteSurah)
                changed = true
            }
            changed
        }
    }

    private suspend fun ensureSeeded() {
        if (contentDao.getHadithCount() == 0) {
            contentDao.insertHadith(defaultHadiths)
        }
        if (contentDao.getSurahVerseCount() == 0) {
            contentDao.insertSurahVerses(defaultSurahVerses)
        }
    }

    private val defaultHadiths = listOf(
        HadithContentEntity(
            id = "seed_hadith_1",
            text = "Amalan yang paling dicintai Allah adalah yang dikerjakan terus-menerus walau sedikit.",
            source = "HR. Bukhari dan Muslim"
        ),
        HadithContentEntity(
            id = "seed_hadith_2",
            text = "Sebaik-baik kalian adalah yang paling baik akhlaknya.",
            source = "HR. Bukhari"
        ),
        HadithContentEntity(
            id = "seed_hadith_3",
            text = "Barang siapa memudahkan urusan orang lain, Allah mudahkan urusannya di dunia dan akhirat.",
            source = "HR. Muslim"
        ),
        HadithContentEntity(
            id = "seed_hadith_4",
            text = "Tersenyum kepada saudaramu adalah sedekah.",
            source = "HR. Tirmidzi"
        ),
        HadithContentEntity(
            id = "seed_hadith_5",
            text = "Orang kuat bukan yang menang bergulat, tetapi yang mampu menahan marah.",
            source = "HR. Bukhari dan Muslim"
        )
    )

    private val defaultSurahVerses = listOf(
        SurahVerseEntity(
            id = "seed_surah_1",
            surahName = "Al-Baqarah",
            ayahNumber = 286,
            translation = "Allah tidak membebani seseorang melainkan sesuai dengan kesanggupannya."
        ),
        SurahVerseEntity(
            id = "seed_surah_2",
            surahName = "Al-Insyirah",
            ayahNumber = 6,
            translation = "Sesungguhnya bersama kesulitan ada kemudahan."
        ),
        SurahVerseEntity(
            id = "seed_surah_3",
            surahName = "Ar-Rad",
            ayahNumber = 28,
            translation = "Hanya dengan mengingat Allah hati menjadi tenteram."
        ),
        SurahVerseEntity(
            id = "seed_surah_4",
            surahName = "Ali Imran",
            ayahNumber = 139,
            translation = "Janganlah kamu merasa lemah dan bersedih hati, kamu paling tinggi derajatnya jika beriman."
        ),
        SurahVerseEntity(
            id = "seed_surah_5",
            surahName = "At-Talaq",
            ayahNumber = 3,
            translation = "Barang siapa bertawakal kepada Allah, niscaya Allah akan mencukupkan kebutuhannya."
        )
    )
}

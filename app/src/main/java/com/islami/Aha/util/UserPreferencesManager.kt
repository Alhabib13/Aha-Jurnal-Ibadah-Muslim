package com.islami.Aha.util

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GenderProfile { UNSPECIFIED, MALE, FEMALE }

/**
 * Rentang usia dari onboarding. [code] = nilai di Firestore (meta/profile.rentangUsia dan
 * users/{uid}.rentang_umur), sama dengan ResearchSchema.Profil.RENTANG_UMUR_VALUES.
 */
enum class AgeRange(val code: String, val label: String) {
    UNDER_15("<15", "Di bawah 15 tahun"),
    AGE_15_18("15-18", "15–18 tahun"),
    AGE_19_25("19-25", "19–25 tahun"),
    AGE_26_35("26-35", "26–35 tahun"),
    OVER_35(">35", "Di atas 35 tahun");

    /** Rentang yang (sebagian) di bawah 18 tahun: perlu pernyataan izin orang tua/wali. */
    val isMinor: Boolean get() = this == UNDER_15 || this == AGE_15_18

    companion object {
        /** Kode lama sebelum v1.2.0 ("<13", "13-17", "18+"): hanya "<13" yang pasti masuk rentang baru. */
        fun fromCode(code: String?): AgeRange? =
            entries.firstOrNull { it.code == code } ?: if (code == "<13") UNDER_15 else null
    }
}

/** Kesibukan utama dari onboarding. [code] = users/{uid}.status_aktivitas. */
enum class StatusAktivitas(val code: String, val label: String) {
    PELAJAR("pelajar", "Pelajar"),
    MAHASISWA("mahasiswa", "Mahasiswa"),
    PEKERJA("pekerja", "Pekerja"),
    LAINNYA("lainnya", "Lainnya");

    companion object {
        fun fromCode(code: String?): StatusAktivitas? = entries.firstOrNull { it.code == code }
    }
}

object UserPreferencesManager {
    private const val KEY_GENDER = "gender_profile"
    private const val KEY_JUMAT_ENABLED = "jumat_enabled"
    private const val KEY_HAIDH_MODE = "haidh_mode"
    private const val KEY_HAIDH_DATES = "haidh_dates"
    private const val KEY_SEEN_PROMPT = "seen_gender_prompt"
    private const val KEY_AGE_RANGE = "age_range"
    private const val KEY_GUARDIAN_OK = "guardian_ok"
    private const val KEY_STATUS_AKTIVITAS = "status_aktivitas"
    private const val KEY_PILIHAN_AWAL = "pilihan_awal"
    
    private val _gender = MutableStateFlow(GenderProfile.UNSPECIFIED)
    val gender: StateFlow<GenderProfile> = _gender.asStateFlow()
    
    private val _hasSeenPrompt = MutableStateFlow(false)
    val hasSeenPrompt: StateFlow<Boolean> = _hasSeenPrompt.asStateFlow()
    
    private val _hasSeenOnboarding = MutableStateFlow(false)
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()
    
    private val _isJumatEnabled = MutableStateFlow(false)
    val isJumatEnabled: StateFlow<Boolean> = _isJumatEnabled.asStateFlow()
    
    private val _isHaidhMode = MutableStateFlow(false)
    val isHaidhMode: StateFlow<Boolean> = _isHaidhMode.asStateFlow()
    
    private val _haidhDates = MutableStateFlow<Set<String>>(emptySet())
    val haidhDates: StateFlow<Set<String>> = _haidhDates.asStateFlow()
    
    private val _ageRange = MutableStateFlow<AgeRange?>(null)
    val ageRange: StateFlow<AgeRange?> = _ageRange.asStateFlow()

    /** Pengguna di bawah 18 tahun menyatakan orang tua/wali sudah mengizinkan. */
    private val _guardianOk = MutableStateFlow(false)
    val guardianOk: StateFlow<Boolean> = _guardianOk.asStateFlow()

    private val _statusAktivitas = MutableStateFlow<StatusAktivitas?>(null)
    val statusAktivitas: StateFlow<StatusAktivitas?> = _statusAktivitas.asStateFlow()

    /** Snapshot ibadah yang dicentang di onboarding; null = tidak menjawab. */
    private val _pilihanAwal = MutableStateFlow<Set<String>?>(null)
    val pilihanAwal: StateFlow<Set<String>?> = _pilihanAwal.asStateFlow()

    private var prefs: SharedPreferences? = null

    /** Profil ibadah permanen: sekali diisi (laki-laki/perempuan) tidak bisa diganti dari aplikasi. */
    val isGenderLocked: Boolean get() = _gender.value != GenderProfile.UNSPECIFIED

    fun init(sharedPreferences: SharedPreferences) {
        prefs = sharedPreferences
        _gender.value = GenderProfile.valueOf(sharedPreferences.getString(KEY_GENDER, GenderProfile.UNSPECIFIED.name)!!)
        _isJumatEnabled.value = sharedPreferences.getBoolean(KEY_JUMAT_ENABLED, false)
        _isHaidhMode.value = sharedPreferences.getBoolean(KEY_HAIDH_MODE, false)
        _hasSeenPrompt.value = sharedPreferences.getBoolean(KEY_SEEN_PROMPT, false)
        _hasSeenOnboarding.value = sharedPreferences.getBoolean("seen_onboarding", false)
        _haidhDates.value = sharedPreferences.getStringSet(KEY_HAIDH_DATES, emptySet()) ?: emptySet()
        _ageRange.value = AgeRange.fromCode(sharedPreferences.getString(KEY_AGE_RANGE, null))
        _guardianOk.value = sharedPreferences.getBoolean(KEY_GUARDIAN_OK, false)
        _statusAktivitas.value = StatusAktivitas.fromCode(sharedPreferences.getString(KEY_STATUS_AKTIVITAS, null))
        _pilihanAwal.value = sharedPreferences.getStringSet(KEY_PILIHAN_AWAL, null)?.toSet()
        syncHaidhDates()
    }

    fun setHasSeenOnboarding() {
        _hasSeenOnboarding.value = true
        prefs?.edit()?.putBoolean("seen_onboarding", true)?.apply()
    }

    fun setHasSeenPrompt() {
        _hasSeenPrompt.value = true
        prefs?.edit()?.putBoolean(KEY_SEEN_PROMPT, true)?.apply()
    }

    /**
     * Set profil dari pilihan pengguna. Ditolak bila profil sudah terkunci dengan nilai lain
     * (profil permanen; server juga menolak perubahan lewat Firestore rules).
     * @return true bila tersimpan.
     */
    fun setGender(profile: GenderProfile): Boolean {
        if (isGenderLocked && profile != _gender.value) return false
        applyGender(profile)
        return true
    }

    /** Nilai dari server (akun yang login) selalu menang atas pilihan lokal. */
    fun setGenderFromCloud(profile: GenderProfile) {
        if (profile == GenderProfile.UNSPECIFIED) return
        applyGender(profile)
    }

    fun setAgeRange(range: AgeRange, guardianOk: Boolean) {
        _ageRange.value = range
        _guardianOk.value = range.isMinor && guardianOk
        prefs?.edit()?.putString(KEY_AGE_RANGE, range.code)?.putBoolean(KEY_GUARDIAN_OK, _guardianOk.value)?.apply()
    }

    fun setStatusAktivitas(status: StatusAktivitas) {
        _statusAktivitas.value = status
        prefs?.edit()?.putString(KEY_STATUS_AKTIVITAS, status.code)?.apply()
    }

    /** Disimpan sekali saat onboarding; [pilihan] kosong = "belum ada yang rutin". */
    fun setPilihanAwal(pilihan: Set<String>) {
        _pilihanAwal.value = pilihan
        prefs?.edit()?.putStringSet(KEY_PILIHAN_AWAL, pilihan)?.apply()
    }

    private fun applyGender(profile: GenderProfile) {
        _gender.value = profile
        prefs?.edit()?.putString(KEY_GENDER, profile.name)?.apply()
        
        if (profile == GenderProfile.MALE) {
            setJumatEnabled(true)
            setHaidhMode(false, reason = "setGender(MALE)")
        } else if (profile == GenderProfile.FEMALE) {
            setJumatEnabled(false)
        }
    }
    
    fun setJumatEnabled(enabled: Boolean) {
        _isJumatEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_JUMAT_ENABLED, enabled)?.apply()
    }
    
    fun getHaidhDates(): Set<String> {
        return prefs?.getStringSet(KEY_HAIDH_DATES, emptySet()) ?: emptySet()
    }
    
    fun syncHaidhDates() {
        if (_isHaidhMode.value) {
            val today = java.time.LocalDate.now()
            val dates = getHaidhDates().toMutableSet()
            
            if (dates.isNotEmpty()) {
                val parsedDates = dates.mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                val pastDates = parsedDates.filter { it.isBefore(today) }.sorted()
                
                if (pastDates.isNotEmpty()) {
                    var current = pastDates.last().plusDays(1)
                    while (current.isBefore(today) || current.isEqual(today)) {
                        dates.add(current.toString())
                        current = current.plusDays(1)
                    }
                } else {
                    dates.add(today.toString())
                }
            } else {
                dates.add(today.toString())
            }

            prefs?.edit()?.putStringSet(KEY_HAIDH_DATES, dates)?.apply()
            _haidhDates.value = dates.toSet()
        }
    }
    
    fun setHaidhMode(enabled: Boolean, reason: String = "Unknown") {
        if (!enabled && _isHaidhMode.value) {
            // Log this to Firestore or SharedPreferences so we can read it!
            prefs?.edit()?.putString("DEBUG_HAIDH_OFF_REASON", reason)?.apply()
        }
        _isHaidhMode.value = enabled
        prefs?.edit()?.putBoolean(KEY_HAIDH_MODE, enabled)?.apply()
        
        val dateKey = DateUtils.getTodayKey()
        val dates = getHaidhDates().toMutableSet()
        if (enabled) {
            dates.add(dateKey)
        } else {
            dates.remove(dateKey)
        }
        prefs?.edit()?.putStringSet(KEY_HAIDH_DATES, dates)?.apply()
        _haidhDates.value = dates.toSet()
    }
    
    fun setHaidhDates(dates: List<String>) {
        val mergedDates = getHaidhDates().toMutableSet()
        mergedDates.addAll(dates)
        prefs?.edit()?.putStringSet(KEY_HAIDH_DATES, mergedDates)?.apply()
        _haidhDates.value = mergedDates.toSet()
    }
    
    fun clearAll() {
        _gender.value = GenderProfile.UNSPECIFIED
        _isJumatEnabled.value = false
        _isHaidhMode.value = false
        _haidhDates.value = emptySet()
        _hasSeenPrompt.value = false
        _ageRange.value = null
        _guardianOk.value = false
        _statusAktivitas.value = null
        _pilihanAwal.value = null
        prefs?.edit()?.apply {
            remove(KEY_AGE_RANGE)
            remove(KEY_STATUS_AKTIVITAS)
            remove(KEY_PILIHAN_AWAL)
            remove(KEY_GUARDIAN_OK)
            remove(KEY_GENDER)
            remove(KEY_JUMAT_ENABLED)
            remove(KEY_HAIDH_MODE)
            remove(KEY_HAIDH_DATES)
            remove(KEY_SEEN_PROMPT)
            apply()
        }
    }
}

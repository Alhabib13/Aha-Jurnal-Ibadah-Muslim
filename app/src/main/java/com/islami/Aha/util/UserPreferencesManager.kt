package com.islami.Aha.util

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GenderProfile { UNSPECIFIED, MALE, FEMALE }

object UserPreferencesManager {
    private const val KEY_GENDER = "gender_profile"
    private const val KEY_JUMAT_ENABLED = "jumat_enabled"
    private const val KEY_HAIDH_MODE = "haidh_mode"
    private const val KEY_HAIDH_DATES = "haidh_dates"
    private const val KEY_SEEN_PROMPT = "seen_gender_prompt"
    
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
    
    private var prefs: SharedPreferences? = null

    fun init(sharedPreferences: SharedPreferences) {
        prefs = sharedPreferences
        _gender.value = GenderProfile.valueOf(sharedPreferences.getString(KEY_GENDER, GenderProfile.UNSPECIFIED.name)!!)
        _isJumatEnabled.value = sharedPreferences.getBoolean(KEY_JUMAT_ENABLED, false)
        _isHaidhMode.value = sharedPreferences.getBoolean(KEY_HAIDH_MODE, false)
        _hasSeenPrompt.value = sharedPreferences.getBoolean(KEY_SEEN_PROMPT, false)
        _hasSeenOnboarding.value = sharedPreferences.getBoolean("seen_onboarding", false)
        _haidhDates.value = sharedPreferences.getStringSet(KEY_HAIDH_DATES, emptySet()) ?: emptySet()
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

    fun setGender(profile: GenderProfile) {
        _gender.value = profile
        prefs?.edit()?.putString(KEY_GENDER, profile.name)?.apply()
        
        if (profile == GenderProfile.MALE) {
            setJumatEnabled(true)
            setHaidhMode(false)
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
            val dateKey = today.toString()
            val dates = getHaidhDates().toMutableSet()
            
            // Check if we should auto-turn off (Max 15 days in Islam)
            var shouldTurnOff = false
            if (dates.isNotEmpty()) {
                val sortedDates = dates.mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }.sorted()
                if (sortedDates.isNotEmpty()) {
                    val lastDate = sortedDates.last()
                    
                    // Find the start of the CURRENT cycle by walking backwards.
                    // A gap of more than 15 days means it's a different cycle.
                    var currentCycleStart = lastDate
                    for (i in sortedDates.size - 2 downTo 0) {
                        val prevDate = sortedDates[i]
                        val gap = java.time.temporal.ChronoUnit.DAYS.between(prevDate, currentCycleStart)
                        if (gap > 15) {
                            break // Found the gap separating the previous cycle
                        }
                        currentCycleStart = prevDate
                    }
                    
                    val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(currentCycleStart, today)
                    val daysSinceLast = java.time.temporal.ChronoUnit.DAYS.between(lastDate, today)
                    
                    // Turn off if it's been more than 15 days since the start of this current cycle,
                    // OR if the user hasn't opened the app (in Haidh mode) for > 15 days.
                    if (daysSinceStart > 15 || daysSinceLast > 15) {
                        shouldTurnOff = true
                    }
                }
            }

            if (shouldTurnOff) {
                setHaidhMode(false)
                return
            }

            // Fill in any gaps between the last recorded date and today
            if (dates.isNotEmpty()) {
                val sortedDates = dates.mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }.sorted()
                if (sortedDates.isNotEmpty()) {
                    var current = sortedDates.last().plusDays(1)
                    while (current.isBefore(today) || current.isEqual(today)) {
                        dates.add(current.toString())
                        current = current.plusDays(1)
                    }
                }
            } else {
                dates.add(dateKey)
            }

            prefs?.edit()?.putStringSet(KEY_HAIDH_DATES, dates)?.apply()
            _haidhDates.value = dates.toSet()
        }
    }
    
    fun setHaidhMode(enabled: Boolean) {
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
        prefs?.edit()?.apply {
            remove(KEY_GENDER)
            remove(KEY_JUMAT_ENABLED)
            remove(KEY_HAIDH_MODE)
            remove(KEY_HAIDH_DATES)
            remove(KEY_SEEN_PROMPT)
            apply()
        }
    }
}

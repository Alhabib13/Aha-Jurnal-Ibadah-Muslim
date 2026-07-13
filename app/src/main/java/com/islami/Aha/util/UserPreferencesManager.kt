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
    
    private val _isJumatEnabled = MutableStateFlow(false)
    val isJumatEnabled: StateFlow<Boolean> = _isJumatEnabled.asStateFlow()
    
    private val _isHaidhMode = MutableStateFlow(false)
    val isHaidhMode: StateFlow<Boolean> = _isHaidhMode.asStateFlow()
    
    private var prefs: SharedPreferences? = null

    fun init(sharedPreferences: SharedPreferences) {
        prefs = sharedPreferences
        _gender.value = GenderProfile.valueOf(sharedPreferences.getString(KEY_GENDER, GenderProfile.UNSPECIFIED.name)!!)
        _isJumatEnabled.value = sharedPreferences.getBoolean(KEY_JUMAT_ENABLED, false)
        _isHaidhMode.value = sharedPreferences.getBoolean(KEY_HAIDH_MODE, false)
        _hasSeenPrompt.value = sharedPreferences.getBoolean(KEY_SEEN_PROMPT, false)
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
    }
    
    fun clearAll() {
        _gender.value = GenderProfile.UNSPECIFIED
        _isJumatEnabled.value = false
        _isHaidhMode.value = false
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

package com.islami.Aha.ui.onboarding

import androidx.lifecycle.ViewModel
import com.islami.Aha.data.research.ResearchLogRepository
import com.islami.Aha.util.AgeRange
import com.islami.Aha.util.GenderProfile
import com.islami.Aha.util.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Menyimpan pilihan onboarding. Semuanya lokal; bila pengguna login, profil & usia didorong
 * ke users/{uid}/meta/profile oleh alur login di HomeViewModel.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val researchLogRepository: ResearchLogRepository
) : ViewModel() {

    fun simpanProfil(profil: GenderProfile) {
        UserPreferencesManager.setGender(profil)
        UserPreferencesManager.setHasSeenPrompt()
    }

    fun simpanUsia(usia: AgeRange, izinWali: Boolean) {
        UserPreferencesManager.setAgeRange(usia, izinWali)
    }

    fun simpanPersetujuan(setuju: Boolean) {
        researchLogRepository.setResearchConsent(setuju)
    }
}

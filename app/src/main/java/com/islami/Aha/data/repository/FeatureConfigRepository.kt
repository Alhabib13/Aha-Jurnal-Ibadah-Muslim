package com.islami.Aha.data.repository

import com.islami.Aha.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppFeatureConfig(
    val puasaWajibRamadanEnabled: Boolean = false,
    val ramadanScheduleByLocationEnabled: Boolean = false,
    val sholatTarawihEnabled: Boolean = false,
    val fardhuScheduleByLocationEnabled: Boolean = true
)

@Singleton
class FeatureConfigRepository @Inject constructor() {
    private val _featureConfig = MutableStateFlow(buildConfig())
    val featureConfig: StateFlow<AppFeatureConfig> = _featureConfig.asStateFlow()

    fun refresh() {
        _featureConfig.value = buildConfig()
    }

    private fun buildConfig(): AppFeatureConfig {
        val isRamadanMonth = DateUtils.isRamadanMonth()
        return AppFeatureConfig(
            puasaWajibRamadanEnabled = isRamadanMonth,
            ramadanScheduleByLocationEnabled = isRamadanMonth,
            sholatTarawihEnabled = isRamadanMonth,
            fardhuScheduleByLocationEnabled = true
        )
    }
}

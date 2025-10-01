package com.trackingapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackingapp.data.model.AppSettings
import com.trackingapp.data.model.ExportFormat
import com.trackingapp.data.model.LocationAccuracy
import com.trackingapp.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun updateLocationAccuracy(accuracy: LocationAccuracy) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val updatedSettings = currentSettings.copy(locationAccuracy = accuracy)
            settingsRepository.updateSettings(updatedSettings)
        }
    }

    fun updateLocationInterval(intervalMs: Long) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val updatedSettings = currentSettings.copy(locationIntervalMs = intervalMs)
            settingsRepository.updateSettings(updatedSettings)
        }
    }

    fun updateMinDistance(minDistanceMeters: Float) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val updatedSettings = currentSettings.copy(minDistanceMeters = minDistanceMeters)
            settingsRepository.updateSettings(updatedSettings)
        }
    }

    fun updateAccuracyThreshold(thresholdMeters: Float) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val updatedSettings = currentSettings.copy(accuracyThresholdMeters = thresholdMeters)
            settingsRepository.updateSettings(updatedSettings)
        }
    }

    fun updateAutoPauseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val updatedSettings = currentSettings.copy(autoPauseEnabled = enabled)
            settingsRepository.updateSettings(updatedSettings)
        }
    }

    fun updateAutoPauseSpeedThreshold(thresholdMs: Float) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val updatedSettings = currentSettings.copy(autoPauseSpeedThreshold = thresholdMs)
            settingsRepository.updateSettings(updatedSettings)
        }
    }

    fun updateAutoPauseDuration(durationSec: Long) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val updatedSettings = currentSettings.copy(autoPauseDurationSec = durationSec)
            settingsRepository.updateSettings(updatedSettings)
        }
    }

    fun updateDefaultExportFormat(format: ExportFormat) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val updatedSettings = currentSettings.copy(defaultExportFormat = format)
            settingsRepository.updateSettings(updatedSettings)
        }
    }

    fun getLocationAccuracyOptions(): List<LocationAccuracyOption> {
        return listOf(
            LocationAccuracyOption(
                accuracy = LocationAccuracy.HIGH_ACCURACY,
                name = "Высокая точность",
                description = "Точность до 1-3 метров, больше расход батареи"
            ),
            LocationAccuracyOption(
                accuracy = LocationAccuracy.BALANCED,
                name = "Сбалансированная",
                description = "Точность до 5-10 метров, оптимальный расход батареи"
            ),
            LocationAccuracyOption(
                accuracy = LocationAccuracy.LOW_POWER,
                name = "Экономичная",
                description = "Точность до 50-100 метров, минимальный расход батареи"
            )
        )
    }

    fun getLocationIntervalOptions(): List<LocationIntervalOption> {
        return listOf(
            LocationIntervalOption(1000L, "1 секунда"),
            LocationIntervalOption(2000L, "2 секунды"),
            LocationIntervalOption(5000L, "5 секунд"),
            LocationIntervalOption(10000L, "10 секунд"),
            LocationIntervalOption(15000L, "15 секунд"),
            LocationIntervalOption(30000L, "30 секунд")
        )
    }

    fun getMinDistanceOptions(): List<MinDistanceOption> {
        return listOf(
            MinDistanceOption(1f, "1 метр"),
            MinDistanceOption(5f, "5 метров"),
            MinDistanceOption(10f, "10 метров"),
            MinDistanceOption(20f, "20 метров"),
            MinDistanceOption(50f, "50 метров")
        )
    }

    fun getAccuracyThresholdOptions(): List<AccuracyThresholdOption> {
        return listOf(
            AccuracyThresholdOption(10f, "10 метров"),
            AccuracyThresholdOption(20f, "20 метров"),
            AccuracyThresholdOption(50f, "50 метров"),
            AccuracyThresholdOption(100f, "100 метров"),
            AccuracyThresholdOption(200f, "200 метров")
        )
    }

    data class LocationAccuracyOption(
        val accuracy: LocationAccuracy,
        val name: String,
        val description: String
    )

    data class LocationIntervalOption(
        val intervalMs: Long,
        val name: String
    )

    data class MinDistanceOption(
        val distanceMeters: Float,
        val name: String
    )

    data class AccuracyThresholdOption(
        val thresholdMeters: Float,
        val name: String
    )
}




package com.xtrack.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val locationAccuracy: LocationAccuracy = LocationAccuracy.BALANCED,
    val locationIntervalMs: Long = 5000L,
    val minDistanceMeters: Float = 10f,
    val accuracyThresholdMeters: Float = 50f,
    val autoPauseEnabled: Boolean = false,
    val autoPauseSpeedThreshold: Float = 1.0f, // m/s
    val autoPauseDurationSec: Long = 30L,
    val defaultExportFormat: ExportFormat = ExportFormat.GPX,
    val appExitState: AppExitState = AppExitState.STOPPED, // Состояние приложения при выходе
    val distanceNotificationIntervalMeters: Int = 1000, // Интервал уведомлений о расстоянии (100-5000 метров)
    val distanceNotificationsEnabled: Boolean = false // Включить/выключить уведомления о расстоянии
)

enum class LocationAccuracy {
    HIGH_ACCURACY,
    BALANCED,
    LOW_POWER
}

enum class ExportFormat {
    GPX,
    GEOJSON
}

enum class AppExitState {
    STOPPED,        // Приложение было остановлено нормально (зеленая кнопка)
    RECORDING       // Приложение было закрыто во время записи (красная кнопка)
}


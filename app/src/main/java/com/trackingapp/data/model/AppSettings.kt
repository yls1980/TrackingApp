package com.trackingapp.data.model

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
    val defaultExportFormat: ExportFormat = ExportFormat.GPX
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


package com.xtrack.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey
    val id: String,
    val name: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val distanceMeters: Double,
    val durationSec: Long,
    val gpxPath: String?,
    val geojsonPath: String?,
    val isRecording: Boolean = false
)


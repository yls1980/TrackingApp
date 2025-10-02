package com.xtrack.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "buffered_track_points")
data class BufferedTrackPoint(
    @PrimaryKey
    val id: String,
    val trackId: String,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val accuracy: Float,
    val isSynced: Boolean = false,
    val createdAt: Instant = kotlinx.datetime.Clock.System.now()
)

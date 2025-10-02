package com.xtrack.data.model

import kotlinx.datetime.Instant

data class GpxTrack(
    val name: String,
    val trackPoints: List<GpxTrackPoint>
)

data class GpxTrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val time: Instant?,
    val speed: Float?,
    val course: Float?,
    val hdop: Float?
)






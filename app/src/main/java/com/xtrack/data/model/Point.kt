package com.xtrack.data.model

/**
 * Simple point class to replace Google's LatLng
 */
data class Point(
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        fun fromTrackPoint(trackPoint: TrackPoint): Point {
            return Point(trackPoint.latitude, trackPoint.longitude)
        }
    }
}



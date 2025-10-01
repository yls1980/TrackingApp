package com.trackingapp.data.parser

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.trackingapp.data.model.GeoJsonGeometry
import com.trackingapp.data.model.GeoJsonProperties
import com.trackingapp.data.model.GeoJsonTrack
import com.trackingapp.data.model.Track
import com.trackingapp.data.model.TrackPoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoJsonGenerator @Inject constructor() {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    fun generateGeoJson(track: Track, trackPoints: List<TrackPoint>): String {
        val coordinates = trackPoints.map { point ->
            val coordinate = mutableListOf(point.longitude, point.latitude)
            point.altitude?.let { coordinate.add(it) }
            coordinate
        }

        val properties = GeoJsonProperties(
            name = track.name,
            distance_m = track.distanceMeters,
            duration_s = track.durationSec
        )

        val geometry = GeoJsonGeometry(
            type = "LineString",
            coordinates = coordinates
        )

        val geoJsonTrack = GeoJsonTrack(
            type = "Feature",
            properties = properties,
            geometry = geometry
        )

        return gson.toJson(geoJsonTrack)
    }
}




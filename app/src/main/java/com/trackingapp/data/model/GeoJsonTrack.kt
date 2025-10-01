package com.trackingapp.data.model

data class GeoJsonTrack(
    val type: String = "Feature",
    val properties: GeoJsonProperties,
    val geometry: GeoJsonGeometry
)

data class GeoJsonProperties(
    val name: String,
    val distance_m: Double,
    val duration_s: Long
)

data class GeoJsonGeometry(
    val type: String = "LineString",
    val coordinates: List<List<Double>>
)




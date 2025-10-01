package com.trackingapp.data.parser

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.trackingapp.data.model.GeoJsonTrack
import com.trackingapp.data.model.GpxTrack
import com.trackingapp.data.model.GpxTrackPoint
import kotlinx.datetime.Instant
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoJsonParser @Inject constructor(
    private val gson: Gson
) {

    fun parseGeoJson(inputStream: InputStream): GeoJsonTrack? {
        return try {
            val json = inputStream.bufferedReader().use { it.readText() }
            gson.fromJson(json, GeoJsonTrack::class.java)
        } catch (e: JsonSyntaxException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun convertToGpxTrack(geoJsonTrack: GeoJsonTrack): GpxTrack {
        val trackPoints = geoJsonTrack.geometry.coordinates.map { coordinate ->
            GpxTrackPoint(
                latitude = coordinate[1], // GeoJSON uses [lon, lat, alt]
                longitude = coordinate[0],
                elevation = if (coordinate.size > 2) coordinate[2] else null,
                time = null, // GeoJSON doesn't include time
                speed = null,
                course = null,
                hdop = null
            )
        }
        
        return GpxTrack(geoJsonTrack.properties.name, trackPoints)
    }
}




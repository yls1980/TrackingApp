package com.trackingapp.data.parser

import com.google.gson.Gson
import com.trackingapp.data.model.GeoJsonTrack
import com.trackingapp.data.model.Track
import com.trackingapp.data.model.TrackPoint
import kotlinx.datetime.Clock
import org.junit.Assert.*
import org.junit.Test

class GeoJsonGeneratorTest {

    private val geoJsonGenerator = GeoJsonGenerator()
    private val gson = Gson()

    @Test
    fun `generateGeoJson should create valid GeoJSON content`() {
        val track = Track(
            id = "test-track-1",
            name = "Test Track",
            startedAt = Clock.System.now(),
            endedAt = null,
            distanceMeters = 1000.0,
            durationSec = 3600,
            gpxPath = null,
            geojsonPath = null,
            isRecording = false
        )

        val trackPoints = listOf(
            TrackPoint(
                id = "point-1",
                trackId = "test-track-1",
                timestamp = Clock.System.now(),
                latitude = 55.751244,
                longitude = 37.618423,
                altitude = 156.3,
                speed = 2.7f,
                bearing = 180.0f,
                accuracy = 5.0f
            ),
            TrackPoint(
                id = "point-2",
                trackId = "test-track-1",
                timestamp = Clock.System.now(),
                latitude = 55.752244,
                longitude = 37.619423,
                altitude = 157.1,
                speed = 3.1f,
                bearing = 185.0f,
                accuracy = 4.5f
            )
        )

        val geoJsonContent = geoJsonGenerator.generateGeoJson(track, trackPoints)

        // Parse the generated JSON to verify it's valid
        val geoJsonTrack = gson.fromJson(geoJsonContent, GeoJsonTrack::class.java)

        assertEquals("Feature", geoJsonTrack.type)
        assertEquals("Test Track", geoJsonTrack.properties.name)
        assertEquals(1000.0, geoJsonTrack.properties.distance_m, 0.1)
        assertEquals(3600L, geoJsonTrack.properties.duration_s)
        assertEquals("LineString", geoJsonTrack.geometry.type)
        assertEquals(2, geoJsonTrack.geometry.coordinates.size)
        
        // Check first coordinate [lon, lat, alt]
        val firstCoord = geoJsonTrack.geometry.coordinates[0]
        assertEquals(37.618423, firstCoord[0], 0.000001) // longitude
        assertEquals(55.751244, firstCoord[1], 0.000001) // latitude
        assertEquals(156.3, firstCoord[2], 0.1) // altitude
        
        // Check second coordinate
        val secondCoord = geoJsonTrack.geometry.coordinates[1]
        assertEquals(37.619423, secondCoord[0], 0.000001) // longitude
        assertEquals(55.752244, secondCoord[1], 0.000001) // latitude
        assertEquals(157.1, secondCoord[2], 0.1) // altitude
    }

    @Test
    fun `generateGeoJson should handle points without altitude`() {
        val track = Track(
            id = "test-track-2",
            name = "Track Without Altitude",
            startedAt = Clock.System.now(),
            endedAt = null,
            distanceMeters = 500.0,
            durationSec = 1800,
            gpxPath = null,
            geojsonPath = null,
            isRecording = false
        )

        val trackPoints = listOf(
            TrackPoint(
                id = "point-1",
                trackId = "test-track-2",
                timestamp = Clock.System.now(),
                latitude = 55.751244,
                longitude = 37.618423,
                altitude = null,
                speed = null,
                bearing = null,
                accuracy = null
            )
        )

        val geoJsonContent = geoJsonGenerator.generateGeoJson(track, trackPoints)
        val geoJsonTrack = gson.fromJson(geoJsonContent, GeoJsonTrack::class.java)

        assertEquals(1, geoJsonTrack.geometry.coordinates.size)
        val coord = geoJsonTrack.geometry.coordinates[0]
        assertEquals(2, coord.size) // Should only have [lon, lat] without altitude
        assertEquals(37.618423, coord[0], 0.000001) // longitude
        assertEquals(55.751244, coord[1], 0.000001) // latitude
    }

    @Test
    fun `generateGeoJson should handle empty track points`() {
        val track = Track(
            id = "test-track-3",
            name = "Empty Track",
            startedAt = Clock.System.now(),
            endedAt = null,
            distanceMeters = 0.0,
            durationSec = 0,
            gpxPath = null,
            geojsonPath = null,
            isRecording = false
        )

        val geoJsonContent = geoJsonGenerator.generateGeoJson(track, emptyList())
        val geoJsonTrack = gson.fromJson(geoJsonContent, GeoJsonTrack::class.java)

        assertEquals("Feature", geoJsonTrack.type)
        assertEquals("Empty Track", geoJsonTrack.properties.name)
        assertEquals(0.0, geoJsonTrack.properties.distance_m, 0.1)
        assertEquals(0L, geoJsonTrack.properties.duration_s)
        assertEquals("LineString", geoJsonTrack.geometry.type)
        assertTrue(geoJsonTrack.geometry.coordinates.isEmpty())
    }
}




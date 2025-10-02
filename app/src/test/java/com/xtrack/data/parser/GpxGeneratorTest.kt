package com.xtrack.data.parser

import com.xtrack.data.model.Track
import com.xtrack.data.model.TrackPoint
import kotlinx.datetime.Clock
import org.junit.Assert.*
import org.junit.Test

class GpxGeneratorTest {

    private val gpxGenerator = GpxGenerator()

    @Test
    fun `generateGpx should create valid GPX content`() {
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

        val gpxContent = gpxGenerator.generateGpx(track, trackPoints)

        assertTrue("GPX should contain XML declaration", gpxContent.contains("<?xml"))
        assertTrue("GPX should contain gpx root element", gpxContent.contains("<gpx"))
        assertTrue("GPX should contain track name", gpxContent.contains("Test Track"))
        assertTrue("GPX should contain track points", gpxContent.contains("<trkpt"))
        assertTrue("GPX should contain latitude", gpxContent.contains("lat=\"55.751244\""))
        assertTrue("GPX should contain longitude", gpxContent.contains("lon=\"37.618423\""))
        assertTrue("GPX should contain elevation", gpxContent.contains("<ele>156.3</ele>"))
        assertTrue("GPX should contain speed", gpxContent.contains("<speed>2.7</speed>"))
        assertTrue("GPX should contain course", gpxContent.contains("<course>180.0</course>"))
        assertTrue("GPX should contain hdop", gpxContent.contains("<hdop>5.0</hdop>"))
    }

    @Test
    fun `generateGpx should handle special characters in track name`() {
        val track = Track(
            id = "test-track-2",
            name = "Test & Track <with> \"special\" 'chars'",
            startedAt = Clock.System.now(),
            endedAt = null,
            distanceMeters = 0.0,
            durationSec = 0,
            gpxPath = null,
            geojsonPath = null,
            isRecording = false
        )

        val gpxContent = gpxGenerator.generateGpx(track, emptyList())

        assertTrue("GPX should escape ampersand", gpxContent.contains("&amp;"))
        assertTrue("GPX should escape less than", gpxContent.contains("&lt;"))
        assertTrue("GPX should escape greater than", gpxContent.contains("&gt;"))
        assertTrue("GPX should escape quotes", gpxContent.contains("&quot;"))
        assertTrue("GPX should escape apostrophe", gpxContent.contains("&apos;"))
    }

    @Test
    fun `generateGpx should handle empty track points`() {
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

        val gpxContent = gpxGenerator.generateGpx(track, emptyList())

        assertTrue("GPX should contain track segment", gpxContent.contains("<trkseg>"))
        assertTrue("GPX should contain closing track segment", gpxContent.contains("</trkseg>"))
        assertFalse("GPX should not contain track points", gpxContent.contains("<trkpt"))
    }
}






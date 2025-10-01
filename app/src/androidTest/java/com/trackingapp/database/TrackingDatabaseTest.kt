package com.trackingapp.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trackingapp.data.database.TrackingDatabase
import com.trackingapp.data.database.TrackDao
import com.trackingapp.data.database.TrackPointDao
import com.trackingapp.data.model.Track
import com.trackingapp.data.model.TrackPoint
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class TrackingDatabaseTest {

    private lateinit var database: TrackingDatabase
    private lateinit var trackDao: TrackDao
    private lateinit var trackPointDao: TrackPointDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TrackingDatabase::class.java
        ).allowMainThreadQueries().build()
        
        trackDao = database.trackDao()
        trackPointDao = database.trackPointDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetTrack() = runBlocking {
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

        trackDao.insertTrack(track)
        val retrievedTrack = trackDao.getTrackById("test-track-1")

        assertNotNull(retrievedTrack)
        assertEquals("Test Track", retrievedTrack?.name)
        assertEquals(1000.0, retrievedTrack?.distanceMeters)
    }

    @Test
    fun insertAndGetTrackPoints() = runBlocking {
        val track = Track(
            id = "test-track-2",
            name = "Test Track with Points",
            startedAt = Clock.System.now(),
            endedAt = null,
            distanceMeters = 0.0,
            durationSec = 0,
            gpxPath = null,
            geojsonPath = null,
            isRecording = false
        )

        trackDao.insertTrack(track)

        val trackPoint = TrackPoint(
            id = "point-1",
            trackId = "test-track-2",
            timestamp = Clock.System.now(),
            latitude = 55.751244,
            longitude = 37.618423,
            altitude = 156.3,
            speed = 2.7f,
            bearing = 180.0f,
            accuracy = 5.0f
        )

        trackPointDao.insertTrackPoint(trackPoint)
        val retrievedPoints = trackPointDao.getTrackPointsSync("test-track-2")

        assertEquals(1, retrievedPoints.size)
        assertEquals(55.751244, retrievedPoints[0].latitude)
        assertEquals(37.618423, retrievedPoints[0].longitude)
    }

    @Test
    fun deleteTrack() = runBlocking {
        val track = Track(
            id = "test-track-3",
            name = "Track to Delete",
            startedAt = Clock.System.now(),
            endedAt = null,
            distanceMeters = 0.0,
            durationSec = 0,
            gpxPath = null,
            geojsonPath = null,
            isRecording = false
        )

        trackDao.insertTrack(track)
        trackDao.deleteTrackById("test-track-3")
        val deletedTrack = trackDao.getTrackById("test-track-3")

        assertNull(deletedTrack)
    }

    @Test
    fun getCurrentRecordingTrack() = runBlocking {
        val recordingTrack = Track(
            id = "recording-track",
            name = "Currently Recording",
            startedAt = Clock.System.now(),
            endedAt = null,
            distanceMeters = 0.0,
            durationSec = 0,
            gpxPath = null,
            geojsonPath = null,
            isRecording = true
        )

        val finishedTrack = Track(
            id = "finished-track",
            name = "Finished Track",
            startedAt = Clock.System.now(),
            endedAt = Clock.System.now(),
            distanceMeters = 1000.0,
            durationSec = 3600,
            gpxPath = null,
            geojsonPath = null,
            isRecording = false
        )

        trackDao.insertTrack(recordingTrack)
        trackDao.insertTrack(finishedTrack)

        val currentRecording = trackDao.getCurrentRecordingTrack()

        assertNotNull(currentRecording)
        assertEquals("recording-track", currentRecording?.id)
        assertEquals(true, currentRecording?.isRecording)
    }
}




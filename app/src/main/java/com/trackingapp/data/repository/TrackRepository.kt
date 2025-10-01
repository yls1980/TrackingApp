package com.trackingapp.data.repository

import com.trackingapp.data.database.TrackDao
import com.trackingapp.data.database.TrackPointDao
import com.trackingapp.data.model.Track
import com.trackingapp.data.model.TrackPoint
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val trackPointDao: TrackPointDao
) {
    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks()

    suspend fun getTrackById(trackId: String): Track? = trackDao.getTrackById(trackId)

    suspend fun getCurrentRecordingTrack(): Track? = trackDao.getCurrentRecordingTrack()

    suspend fun insertTrack(track: Track) = trackDao.insertTrack(track)

    suspend fun updateTrack(track: Track) = trackDao.updateTrack(track)

    suspend fun deleteTrack(track: Track) = trackDao.deleteTrack(track)

    suspend fun deleteTrackById(trackId: String) = trackDao.deleteTrackById(trackId)

    fun getTrackPoints(trackId: String): Flow<List<TrackPoint>> = trackPointDao.getTrackPoints(trackId)

    suspend fun getTrackPointsSync(trackId: String): List<TrackPoint> = trackPointDao.getTrackPointsSync(trackId)

    suspend fun getLastTrackPoint(trackId: String): TrackPoint? = trackPointDao.getLastTrackPoint(trackId)

    suspend fun insertTrackPoint(trackPoint: TrackPoint) = trackPointDao.insertTrackPoint(trackPoint)

    suspend fun insertTrackPoints(trackPoints: List<TrackPoint>) = trackPointDao.insertTrackPoints(trackPoints)

    suspend fun deleteTrackPointsByTrackId(trackId: String) = trackPointDao.deleteTrackPointsByTrackId(trackId)
}


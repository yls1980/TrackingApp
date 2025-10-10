package com.xtrack.data.repository

import com.xtrack.data.database.TrackDao
import com.xtrack.data.database.TrackPointDao
import com.xtrack.data.model.Track
import com.xtrack.data.model.TrackPoint
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val trackPointDao: TrackPointDao,
    private val mapNoteRepository: MapNoteRepository
) {
    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks()

    suspend fun getTrackById(trackId: String): Track? = trackDao.getTrackById(trackId)

    suspend fun getCurrentRecordingTrack(): Track? = trackDao.getCurrentRecordingTrack()

    suspend fun getLastIncompleteTrack(): Track? = trackDao.getLastIncompleteTrack()

    suspend fun insertTrack(track: Track) = trackDao.insertTrack(track)

    suspend fun updateTrack(track: Track) = trackDao.updateTrack(track)

    suspend fun deleteTrack(track: Track) {
        // Сначала удаляем все заметки трека с медиа файлами
        mapNoteRepository.deleteAllNotesWithMediaByTrackId(track.id)
        
        // Затем удаляем точки трека
        trackPointDao.deleteTrackPointsByTrackId(track.id)
        
        // Наконец удаляем сам трек
        trackDao.deleteTrack(track)
    }

    suspend fun deleteTrackById(trackId: String) {
        // Сначала удаляем все заметки трека с медиа файлами
        mapNoteRepository.deleteAllNotesWithMediaByTrackId(trackId)
        
        // Затем удаляем точки трека
        trackPointDao.deleteTrackPointsByTrackId(trackId)
        
        // Наконец удаляем сам трек
        trackDao.deleteTrackById(trackId)
    }

    fun getTrackPoints(trackId: String): Flow<List<TrackPoint>> = trackPointDao.getTrackPoints(trackId)

    suspend fun getTrackPointsSync(trackId: String): List<TrackPoint> = trackPointDao.getTrackPointsSync(trackId)

    suspend fun getLastTrackPoint(trackId: String): TrackPoint? = trackPointDao.getLastTrackPoint(trackId)

    suspend fun insertTrackPoint(trackPoint: TrackPoint) = trackPointDao.insertTrackPoint(trackPoint)

    suspend fun insertTrackPoints(trackPoints: List<TrackPoint>) = trackPointDao.insertTrackPoints(trackPoints)

    suspend fun deleteTrackPointsByTrackId(trackId: String) = trackPointDao.deleteTrackPointsByTrackId(trackId)
}


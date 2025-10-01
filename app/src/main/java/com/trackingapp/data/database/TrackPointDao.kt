package com.trackingapp.data.database

import androidx.room.*
import com.trackingapp.data.model.TrackPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getTrackPoints(trackId: String): Flow<List<TrackPoint>>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getTrackPointsSync(trackId: String): List<TrackPoint>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTrackPoint(trackId: String): TrackPoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoint(trackPoint: TrackPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoints(trackPoints: List<TrackPoint>)

    @Delete
    suspend fun deleteTrackPoint(trackPoint: TrackPoint)

    @Query("DELETE FROM track_points WHERE trackId = :trackId")
    suspend fun deleteTrackPointsByTrackId(trackId: String)
}


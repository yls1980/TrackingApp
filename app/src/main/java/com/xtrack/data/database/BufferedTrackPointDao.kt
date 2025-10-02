package com.xtrack.data.database

import androidx.room.*
import com.xtrack.data.model.BufferedTrackPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface BufferedTrackPointDao {
    @Query("SELECT * FROM buffered_track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getBufferedPointsByTrackId(trackId: String): Flow<List<BufferedTrackPoint>>

    @Query("SELECT * FROM buffered_track_points WHERE trackId = :trackId AND isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedPointsByTrackId(trackId: String): List<BufferedTrackPoint>

    @Query("SELECT * FROM buffered_track_points WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getAllUnsyncedPoints(): List<BufferedTrackPoint>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBufferedPoint(point: BufferedTrackPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBufferedPoints(points: List<BufferedTrackPoint>)

    @Update
    suspend fun updateBufferedPoint(point: BufferedTrackPoint)

    @Query("UPDATE buffered_track_points SET isSynced = 1 WHERE id = :pointId")
    suspend fun markPointAsSynced(pointId: String)

    @Query("UPDATE buffered_track_points SET isSynced = 1 WHERE trackId = :trackId")
    suspend fun markAllPointsAsSyncedForTrack(trackId: String)

    @Delete
    suspend fun deleteBufferedPoint(point: BufferedTrackPoint)

    @Query("DELETE FROM buffered_track_points WHERE trackId = :trackId")
    suspend fun deleteBufferedPointsByTrackId(trackId: String)

    @Query("DELETE FROM buffered_track_points WHERE isSynced = 1")
    suspend fun deleteSyncedPoints()

    @Query("SELECT COUNT(*) FROM buffered_track_points WHERE trackId = :trackId AND isSynced = 0")
    suspend fun getUnsyncedPointsCount(trackId: String): Int
}

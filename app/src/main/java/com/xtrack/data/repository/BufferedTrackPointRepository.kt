package com.xtrack.data.repository

import com.xtrack.data.database.BufferedTrackPointDao
import com.xtrack.data.model.BufferedTrackPoint
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BufferedTrackPointRepository @Inject constructor(
    private val bufferedTrackPointDao: BufferedTrackPointDao
) {
    fun getBufferedPointsByTrackId(trackId: String): Flow<List<BufferedTrackPoint>> = 
        bufferedTrackPointDao.getBufferedPointsByTrackId(trackId)

    suspend fun getUnsyncedPointsByTrackId(trackId: String): List<BufferedTrackPoint> = 
        bufferedTrackPointDao.getUnsyncedPointsByTrackId(trackId)

    suspend fun getAllUnsyncedPoints(): List<BufferedTrackPoint> = 
        bufferedTrackPointDao.getAllUnsyncedPoints()

    suspend fun insertBufferedPoint(point: BufferedTrackPoint) = 
        bufferedTrackPointDao.insertBufferedPoint(point)

    suspend fun insertBufferedPoints(points: List<BufferedTrackPoint>) = 
        bufferedTrackPointDao.insertBufferedPoints(points)

    suspend fun updateBufferedPoint(point: BufferedTrackPoint) = 
        bufferedTrackPointDao.updateBufferedPoint(point)

    suspend fun markPointAsSynced(pointId: String) = 
        bufferedTrackPointDao.markPointAsSynced(pointId)

    suspend fun markAllPointsAsSyncedForTrack(trackId: String) = 
        bufferedTrackPointDao.markAllPointsAsSyncedForTrack(trackId)

    suspend fun deleteBufferedPoint(point: BufferedTrackPoint) = 
        bufferedTrackPointDao.deleteBufferedPoint(point)

    suspend fun deleteBufferedPointsByTrackId(trackId: String) = 
        bufferedTrackPointDao.deleteBufferedPointsByTrackId(trackId)

    suspend fun deleteSyncedPoints() = 
        bufferedTrackPointDao.deleteSyncedPoints()

    suspend fun getUnsyncedPointsCount(trackId: String): Int = 
        bufferedTrackPointDao.getUnsyncedPointsCount(trackId)
}

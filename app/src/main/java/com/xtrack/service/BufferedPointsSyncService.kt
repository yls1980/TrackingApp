package com.xtrack.service

import android.content.Context
import com.xtrack.data.model.BufferedTrackPoint
import com.xtrack.data.model.TrackPoint
import com.xtrack.data.repository.BufferedTrackPointRepository
import com.xtrack.data.repository.TrackRepository
import com.xtrack.utils.ErrorLogger
import com.xtrack.utils.NetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BufferedPointsSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bufferedTrackPointRepository: BufferedTrackPointRepository,
    private val trackRepository: TrackRepository
) {
    
    /**
     * Синхронизирует все несинхронизированные буферизованные точки
     */
    suspend fun syncAllBufferedPoints() {
        try {
            ErrorLogger.logMessage(
                context,
                "Starting syncAllBufferedPoints",
                ErrorLogger.LogLevel.INFO
            )
            
            // Принудительно синхронизируем, даже если нет сети
            // Это нужно для локальных операций
            ErrorLogger.logMessage(
                context,
                "Force syncing buffered points regardless of network status",
                ErrorLogger.LogLevel.INFO
            )
            
            val unsyncedPoints = bufferedTrackPointRepository.getAllUnsyncedPoints()
            ErrorLogger.logMessage(
                context,
                "Found ${unsyncedPoints.size} unsynced buffered points",
                ErrorLogger.LogLevel.INFO
            )
            
            if (unsyncedPoints.isEmpty()) {
                ErrorLogger.logMessage(
                    context,
                    "No unsynced buffered points found",
                    ErrorLogger.LogLevel.INFO
                )
                return
            }
            
            ErrorLogger.logMessage(
                context,
                "Syncing ${unsyncedPoints.size} buffered points",
                ErrorLogger.LogLevel.INFO
            )
            
            // Группируем точки по трекам
            val pointsByTrack = unsyncedPoints.groupBy { it.trackId }
            
            for ((trackId, points) in pointsByTrack) {
                try {
                    syncBufferedPointsForTrack(trackId, points)
                } catch (e: Exception) {
                    ErrorLogger.logError(
                        context,
                        e,
                        "Failed to sync buffered points for track: $trackId"
                    )
                }
            }
            
        } catch (e: Exception) {
            ErrorLogger.logError(
                context,
                e,
                "Failed to sync buffered points"
            )
        }
    }
    
    /**
     * Синхронизирует буферизованные точки для конкретного трека
     */
    private suspend fun syncBufferedPointsForTrack(trackId: String, bufferedPoints: List<BufferedTrackPoint>) {
        try {
            // Проверяем, существует ли трек
            val track = trackRepository.getTrackById(trackId)
            if (track == null) {
                ErrorLogger.logMessage(
                    context,
                    "Track $trackId not found, deleting buffered points",
                    ErrorLogger.LogLevel.WARNING
                )
                // Удаляем буферизованные точки для несуществующего трека
                bufferedPoints.forEach { point ->
                    bufferedTrackPointRepository.deleteBufferedPoint(point)
                }
                return
            }
            
            // Конвертируем буферизованные точки в обычные TrackPoint
            val trackPoints = bufferedPoints.map { bufferedPoint ->
                TrackPoint(
                    id = bufferedPoint.id,
                    trackId = bufferedPoint.trackId,
                    timestamp = bufferedPoint.timestamp,
                    latitude = bufferedPoint.latitude,
                    longitude = bufferedPoint.longitude,
                    altitude = bufferedPoint.altitude,
                    speed = bufferedPoint.speed,
                    bearing = bufferedPoint.bearing,
                    accuracy = bufferedPoint.accuracy
                )
            }
            
            // Сохраняем точки в основной таблице
            trackRepository.insertTrackPoints(trackPoints)
            
            // Помечаем буферизованные точки как синхронизированные
            bufferedTrackPointRepository.markAllPointsAsSyncedForTrack(trackId)
            
            ErrorLogger.logMessage(
                context,
                "Successfully synced ${trackPoints.size} points for track: $trackId",
                ErrorLogger.LogLevel.INFO
            )
            
        } catch (e: Exception) {
            ErrorLogger.logError(
                context,
                e,
                "Failed to sync buffered points for track: $trackId"
            )
            throw e
        }
    }
    
    /**
     * Добавляет GPS точку в буфер (когда нет интернета)
     */
    suspend fun addPointToBuffer(
        trackId: String,
        latitude: Double,
        longitude: Double,
        altitude: Double? = null,
        speed: Float? = null,
        bearing: Float? = null,
        accuracy: Float
    ) {
        try {
            val bufferedPoint = BufferedTrackPoint(
                id = UUID.randomUUID().toString(),
                trackId = trackId,
                timestamp = Clock.System.now(),
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                speed = speed,
                bearing = bearing,
                accuracy = accuracy,
                isSynced = false
            )
            
            bufferedTrackPointRepository.insertBufferedPoint(bufferedPoint)
            
            ErrorLogger.logMessage(
                context,
                "Point buffered for track: $trackId at $latitude, $longitude",
                ErrorLogger.LogLevel.INFO
            )
            
        } catch (e: Exception) {
            ErrorLogger.logError(
                context,
                e,
                "Failed to add point to buffer"
            )
        }
    }
    
    /**
     * Получает количество несинхронизированных точек для трека
     */
    suspend fun getUnsyncedPointsCount(trackId: String): Int {
        return try {
            bufferedTrackPointRepository.getUnsyncedPointsCount(trackId)
        } catch (e: Exception) {
            ErrorLogger.logError(
                context,
                e,
                "Failed to get unsynced points count"
            )
            0
        }
    }
    
    /**
     * Очищает синхронизированные буферизованные точки
     */
    suspend fun cleanupSyncedPoints() {
        try {
            bufferedTrackPointRepository.deleteSyncedPoints()
            ErrorLogger.logMessage(
                context,
                "Cleaned up synced buffered points",
                ErrorLogger.LogLevel.INFO
            )
        } catch (e: Exception) {
            // Игнорируем CancellationException - это нормальная отмена корутины
            if (e is kotlinx.coroutines.CancellationException) {
                ErrorLogger.logMessage(
                    context,
                    "Cleanup synced points cancelled (normal behavior)",
                    ErrorLogger.LogLevel.DEBUG
                )
            } else {
                ErrorLogger.logError(
                    context,
                    e,
                    "Failed to cleanup synced points"
                )
            }
        }
    }
}

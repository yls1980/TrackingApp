package com.xtrack.utils

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.xtrack.data.model.Point
import java.util.Locale
import kotlin.math.*

object LocationUtils {
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Calculate distance between two Point objects
     */
    fun calculateDistance(point1: Point, point2: Point): Double {
        return calculateDistance(point1.latitude, point1.longitude, point2.latitude, point2.longitude)
    }
    
    /**
     * Calculate total distance for a list of points
     */
    fun calculateTotalDistance(points: List<Point>): Double {
        if (points.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 1 until points.size) {
            totalDistance += calculateDistance(points[i - 1], points[i])
        }
        return totalDistance
    }
    
    /**
     * Calculate total distance for a list of TrackPoints
     */
    fun calculateTotalDistanceFromTrackPoints(trackPoints: List<com.xtrack.data.model.TrackPoint>): Double {
        if (trackPoints.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 1 until trackPoints.size) {
            val prevPoint = trackPoints[i - 1]
            val currentPoint = trackPoints[i]
            totalDistance += calculateDistance(
                prevPoint.latitude, prevPoint.longitude,
                currentPoint.latitude, currentPoint.longitude
            )
        }
        return totalDistance
    }
    
    /**
     * Calculate bearing between two points
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }
    
    /**
     * Convert speed from m/s to km/h
     */
    fun metersPerSecondToKmh(speedMs: Float): Float {
        return speedMs * 3.6f
    }
    
    /**
     * Convert speed from km/h to m/s
     */
    fun kmhToMetersPerSecond(speedKmh: Float): Float {
        return speedKmh / 3.6f
    }
    
    /**
     * Format distance in meters to human readable string
     */
    fun formatDistance(distanceMeters: Double): String {
        return when {
            distanceMeters < 1000 -> "${distanceMeters.toInt()} м"
            else -> "${String.format(Locale.US, "%.2f", distanceMeters / 1000)} км"
        }
    }
    
    /**
     * Format speed in m/s to human readable string
     */
    fun formatSpeed(speedMs: Float): String {
        val speedKmh = metersPerSecondToKmh(speedMs)
        return "${String.format(Locale.US, "%.1f", speedKmh)} км/ч"
    }
    
    /**
     * Format duration in seconds to human readable string
     */
    fun formatDuration(durationSeconds: Long): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        
        return when {
            hours > 0 -> "${hours}ч ${minutes}мин ${seconds}сек"
            minutes > 0 -> "${minutes}мин ${seconds}сек"
            else -> "${seconds}сек"
        }
    }
    
    /**
     * Check if location is valid (not null and has reasonable accuracy)
     */
    fun isLocationValid(location: Location?, maxAccuracy: Float = 100f): Boolean {
        return location != null && 
               location.accuracy <= maxAccuracy && 
               location.latitude != 0.0 && 
               location.longitude != 0.0
    }
    
    /**
     * Проверяет, включена ли геолокация на устройстве
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Для Android 9.0+
            locationManager?.isLocationEnabled ?: false
        } else {
            // Для более старых версий проверяем провайдеры
            val gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
            val networkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
            gpsEnabled || networkEnabled
        }
    }
}


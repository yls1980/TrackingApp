package com.xtrack.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.xtrack.R
import com.xtrack.data.model.AppSettings
import com.xtrack.data.model.Track
import com.xtrack.data.model.TrackPoint
import com.xtrack.data.repository.SettingsRepository
import com.xtrack.data.repository.TrackRepository
import com.xtrack.presentation.MainActivity
import com.xtrack.service.BufferedPointsSyncService
import com.xtrack.utils.ErrorLogger
import com.xtrack.utils.NetworkUtils
import com.xtrack.utils.LocationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import java.util.*
import kotlin.coroutines.resume
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : LifecycleService() {

    @Inject
    lateinit var trackRepository: TrackRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var bufferedPointsSyncService: BufferedPointsSyncService

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentTrack: Track? = null
    private var lastLocation: Location? = null
    private var totalDistance = 0.0
    private var startTime: kotlinx.datetime.Instant? = null
    private var locationCounter = 0

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "route_recording_channel"
        const val ACTION_START_RECORDING = "start_recording"
        const val ACTION_STOP_RECORDING = "stop_recording"
        const val ACTION_PAUSE_RECORDING = "pause_recording"
        const val ACTION_RESUME_RECORDING = "resume_recording"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        startNetworkMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        // Сразу запускаем foreground сервис, чтобы избежать ошибки таймаута
        startForeground(NOTIFICATION_ID, createNotification())
        
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_PAUSE_RECORDING -> pauseRecording()
            ACTION_RESUME_RECORDING -> resumeRecording()
        }
        
        return START_STICKY
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Уменьшаем частоту логирования - только при первом получении или ошибках
                if (locationResult.locations.isNotEmpty()) {
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "LocationCallback received ${locationResult.locations.size} locations",
                        ErrorLogger.LogLevel.INFO
                    )
                }
                locationResult.lastLocation?.let { location ->
                    onLocationUpdate(location)
                }
            }
            
            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                // Логируем только изменения доступности, а не каждое обновление
                ErrorLogger.logMessage(
                    this@LocationTrackingService,
                    "Location availability changed: isLocationAvailable=${locationAvailability.isLocationAvailable}",
                    ErrorLogger.LogLevel.INFO
                )
            }
        }
    }

    private fun startNetworkMonitoring() {
        lifecycleScope.launch {
            try {
                NetworkUtils.networkStateFlow(this@LocationTrackingService).collect { isNetworkAvailable ->
                    if (isNetworkAvailable) {
                        ErrorLogger.logMessage(
                            this@LocationTrackingService,
                            "Network available, syncing buffered points",
                            ErrorLogger.LogLevel.INFO
                        )
                        // Синхронизируем буферизованные точки при восстановлении сети
                        bufferedPointsSyncService.syncAllBufferedPoints()
                        // Очищаем синхронизированные точки
                        bufferedPointsSyncService.cleanupSyncedPoints()
                    } else {
                        ErrorLogger.logMessage(
                            this@LocationTrackingService,
                            "Network unavailable, points will be buffered",
                            ErrorLogger.LogLevel.WARNING
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    // JobCancellationException - это нормальное завершение корутины
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Network monitoring cancelled: ${e.message}",
                        ErrorLogger.LogLevel.DEBUG
                    )
                } else {
                    ErrorLogger.logError(
                        this@LocationTrackingService,
                        e,
                        "Error in network monitoring"
                    )
                }
            }
        }
    }

    private fun startRecording() {
        if (currentTrack != null) return // Already recording

        lifecycleScope.launch {
            try {
                val settings = settingsRepository.getSettings().first() ?: AppSettings()
                ErrorLogger.logMessage(
                    this@LocationTrackingService,
                    "Starting recording with settings: interval=${settings.locationIntervalMs}ms, accuracy=${settings.locationAccuracy}",
                    ErrorLogger.LogLevel.INFO
                )
                
                // Проверяем доступность GPS перед созданием маршрута
                val currentLocation = getCurrentLocationSync()
                if (currentLocation == null) {
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "GPS not available, cannot start recording",
                        ErrorLogger.LogLevel.WARNING
                    )
                    return@launch
                }
            
            // Create new track
            val trackId = UUID.randomUUID().toString()
            val now = Clock.System.now()
            
            currentTrack = Track(
                id = trackId,
                name = "Маршрут ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}",
                startedAt = now,
                endedAt = null,
                distanceMeters = 0.0,
                durationSec = 0,
                gpxPath = null,
                geojsonPath = null,
                isRecording = true
            )
            
            startTime = now
            totalDistance = 0.0
            
            // Save track to database
            currentTrack?.let { track ->
                trackRepository.insertTrack(track)
                
                // Сохраняем начальную точку сразу после создания трека
                try {
                    val currentLocation = getCurrentLocationSync()
                    if (currentLocation != null) {
                        val initialTrackPoint = TrackPoint(
                            id = UUID.randomUUID().toString(),
                            trackId = track.id,
                            timestamp = now,
                            latitude = currentLocation.latitude,
                            longitude = currentLocation.longitude,
                            altitude = if (currentLocation.hasAltitude()) currentLocation.altitude.toDouble() else null,
                            speed = if (currentLocation.hasSpeed()) currentLocation.speed else null,
                            bearing = if (currentLocation.hasBearing()) currentLocation.bearing else null,
                            accuracy = currentLocation.accuracy
                        )
                        
                        trackRepository.insertTrackPoint(initialTrackPoint)
                        ErrorLogger.logMessage(
                            this@LocationTrackingService,
                            "Initial track point saved: ${initialTrackPoint.latitude}, ${initialTrackPoint.longitude} for track: ${track.id} (accuracy: ${initialTrackPoint.accuracy}m)",
                            ErrorLogger.LogLevel.INFO
                        )
                    } else {
                        ErrorLogger.logMessage(
                            this@LocationTrackingService,
                            "Could not get current location for initial track point",
                            ErrorLogger.LogLevel.WARNING
                        )
                    }
                } catch (e: Exception) {
                    ErrorLogger.logError(
                        this@LocationTrackingService,
                        e,
                        "Failed to save initial track point"
                    )
                }
            }
            
            startLocationUpdates(settings)
            // startForeground уже вызван в onStartCommand
            } catch (e: Exception) {
                ErrorLogger.logError(
                    this@LocationTrackingService,
                    e,
                    "Failed to start recording"
                )
            }
        }
    }

    private fun stopRecording() {
        lifecycleScope.launch {
            currentTrack?.let { track ->
                // Проверяем, есть ли реальные точки геопозиции в маршруте
                val trackPoints = trackRepository.getTrackPointsSync(track.id)
                
                if (trackPoints.isEmpty()) {
                    // Если точек нет, удаляем маршрут
                    trackRepository.deleteTrackById(track.id)
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Track deleted - no GPS points recorded: ${track.id}",
                        ErrorLogger.LogLevel.INFO
                    )
                } else {
                    // Синхронизируем буферизованные точки перед сохранением маршрута
                    val syncJob = lifecycleScope.launch {
                        try {
                            ErrorLogger.logMessage(
                                this@LocationTrackingService,
                                "Starting buffered points sync for track: ${track.id}",
                                ErrorLogger.LogLevel.INFO
                            )
                            bufferedPointsSyncService.syncAllBufferedPoints()
                            ErrorLogger.logMessage(
                                this@LocationTrackingService,
                                "Buffered points synced before saving track",
                                ErrorLogger.LogLevel.INFO
                            )
                        } catch (e: Exception) {
                            ErrorLogger.logError(
                                this@LocationTrackingService,
                                e,
                                "Failed to sync buffered points before saving track"
                            )
                        }
                    }
                    
                    // Ждем завершения синхронизации
                    try {
                        syncJob.join()
                    } catch (e: Exception) {
                        ErrorLogger.logError(
                            this@LocationTrackingService,
                            e,
                            "Failed to wait for buffered points sync"
                        )
                    }
                    
                    // Получаем актуальное количество точек после синхронизации
                    val finalTrackPoints = trackRepository.getTrackPoints(track.id).first()
                    
                    // Пересчитываем расстояние на основе всех точек
                    val recalculatedDistance = if (finalTrackPoints.size >= 2) {
                        var calculatedDistance = 0.0
                        for (i in 1 until finalTrackPoints.size) {
                            val prevPoint = finalTrackPoints[i - 1]
                            val currentPoint = finalTrackPoints[i]
                            calculatedDistance += com.xtrack.utils.LocationUtils.calculateDistance(
                                prevPoint.latitude, prevPoint.longitude,
                                currentPoint.latitude, currentPoint.longitude
                            )
                        }
                        calculatedDistance
                    } else {
                        totalDistance
                    }
                    
                    // Если есть точки, сохраняем маршрут
                    val updatedTrack = track.copy(
                        endedAt = Clock.System.now(),
                        isRecording = false,
                        durationSec = startTime?.let { 
                            Clock.System.now().epochSeconds - it.epochSeconds 
                        } ?: 0,
                        distanceMeters = recalculatedDistance
                    )
                    
                    trackRepository.updateTrack(updatedTrack)
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Track saved with ${finalTrackPoints.size} GPS points, distance: ${recalculatedDistance}m: ${track.id}",
                        ErrorLogger.LogLevel.INFO
                    )
                }
                
                currentTrack = null
            }
            
            stopLocationUpdates()
            stopForeground(true)
            stopSelf()
        }
    }

    private fun pauseRecording() {
        // Implementation for pause functionality
        stopLocationUpdates()
    }

    private fun resumeRecording() {
        // Implementation for resume functionality
        lifecycleScope.launch {
            val settings = settingsRepository.getSettings().first() ?: AppSettings()
            startLocationUpdates(settings)
        }
    }

    private fun startLocationUpdates(settings: com.xtrack.data.model.AppSettings) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ErrorLogger.logMessage(
                    this@LocationTrackingService,
                    "Location permissions not granted, cannot start location updates",
                    ErrorLogger.LogLevel.WARNING
                )
                return
            }

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                settings.locationIntervalMs
            ).apply {
                setMinUpdateDistanceMeters(settings.minDistanceMeters)
                setMaxUpdateDelayMillis(settings.locationIntervalMs * 2)
            }.build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
            
            ErrorLogger.logMessage(
                this@LocationTrackingService,
                "Location updates started successfully",
                ErrorLogger.LogLevel.INFO
            )
        } catch (e: Exception) {
            ErrorLogger.logError(
                this@LocationTrackingService,
                e,
                "Failed to start location updates, but continuing recording"
            )
            // Продолжаем запись даже если не удалось запустить обновления локации
        }
    }

    private fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            ErrorLogger.logMessage(
                this@LocationTrackingService,
                "Location updates stopped successfully",
                ErrorLogger.LogLevel.INFO
            )
        } catch (e: Exception) {
            ErrorLogger.logError(
                this@LocationTrackingService,
                e,
                "Failed to stop location updates"
            )
            // Продолжаем работу даже если не удалось остановить обновления локации
        }
    }

    private fun onLocationUpdate(location: Location) {
        // Логируем только важные обновления местоположения (каждое 10-е или при изменении точности)
        if (locationCounter % 10 == 0 || location.accuracy < 5.0) {
            ErrorLogger.logMessage(
                this@LocationTrackingService,
                "Location update received: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m",
                ErrorLogger.LogLevel.INFO
            )
        }
        locationCounter++
        
        if (!com.xtrack.utils.LocationUtils.isLocationValid(location)) {
            ErrorLogger.logMessage(
                this@LocationTrackingService,
                "Invalid location received, skipping: accuracy=${location.accuracy}m, lat=${location.latitude}, lon=${location.longitude}",
                ErrorLogger.LogLevel.WARNING
            )
            return
        }

        lifecycleScope.launch {
            try {
                val settings = settingsRepository.getSettings().first() ?: AppSettings()
                
                // Check accuracy threshold - логируем только при превышении порога
                if (location.accuracy > settings.accuracyThresholdMeters) {
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Checking accuracy: location=${location.accuracy}m, threshold=${settings.accuracyThresholdMeters}m",
                        ErrorLogger.LogLevel.INFO
                    )
                }
                
                if (location.accuracy > settings.accuracyThresholdMeters) {
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Location accuracy too low (${location.accuracy}m > ${settings.accuracyThresholdMeters}m), skipping point",
                        ErrorLogger.LogLevel.WARNING
                    )
                    return@launch
                }

                currentTrack?.let { track ->
                    try {
                        // Check minimum distance from last point
                        lastLocation?.let { lastLoc ->
                            val distance = com.xtrack.utils.LocationUtils.calculateDistance(
                                lastLoc.latitude, lastLoc.longitude,
                                location.latitude, location.longitude
                            )
                            
                            // Логируем расстояние только при превышении минимального порога
                            if (distance >= settings.minDistanceMeters) {
                                ErrorLogger.logMessage(
                                    this@LocationTrackingService,
                                    "Distance from last point: ${distance}m, min distance: ${settings.minDistanceMeters}m",
                                    ErrorLogger.LogLevel.INFO
                                )
                            }
                            
                            // Skip if distance is too small (same location)
                            if (distance < settings.minDistanceMeters) {
                                ErrorLogger.logMessage(
                                    this@LocationTrackingService,
                                    "Distance too small (${distance}m < ${settings.minDistanceMeters}m), skipping point",
                                    ErrorLogger.LogLevel.WARNING
                                )
                                return@launch
                            }
                            
                            totalDistance += distance
                        }

                        // Create track point
                        val trackPoint = TrackPoint(
                            id = UUID.randomUUID().toString(),
                            trackId = track.id,
                            timestamp = Clock.System.now(),
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = if (location.hasAltitude()) location.altitude.toDouble() else null,
                            speed = if (location.hasSpeed()) location.speed else null,
                            bearing = if (location.hasBearing()) location.bearing else null,
                            accuracy = location.accuracy
                        )

                        // Save track point with network-aware buffering
                        try {
                            val networkAvailable = NetworkUtils.isNetworkAvailable(this@LocationTrackingService)
                            
                            if (networkAvailable) {
                                // Если есть интернет, сохраняем напрямую
                                trackRepository.insertTrackPoint(trackPoint)
                                // Логируем сохранение только каждое 20-е или при проблемах с сетью
                                if (locationCounter % 20 == 0) {
                                    ErrorLogger.logMessage(
                                        this@LocationTrackingService,
                                        "Track point saved directly: ${trackPoint.latitude}, ${trackPoint.longitude} for track: ${track.id}",
                                        ErrorLogger.LogLevel.INFO
                                    )
                                }
                            } else {
                                // Если нет интернета, буферизуем точку
                                bufferedPointsSyncService.addPointToBuffer(
                                    trackId = track.id,
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    altitude = if (location.hasAltitude()) location.altitude.toDouble() else null,
                                    speed = if (location.hasSpeed()) location.speed else null,
                                    bearing = if (location.hasBearing()) location.bearing else null,
                                    accuracy = location.accuracy
                                )
                                ErrorLogger.logMessage(
                                    this@LocationTrackingService,
                                    "Track point buffered (no internet): ${trackPoint.latitude}, ${trackPoint.longitude} for track: ${track.id}",
                                    ErrorLogger.LogLevel.INFO
                                )
                            }
                        } catch (e: Exception) {
                            ErrorLogger.logError(
                                this@LocationTrackingService,
                                e,
                                "Failed to save/buffer track point, but continuing recording"
                            )
                            // Продолжаем запись даже если не удалось сохранить точку
                        }

                        // Update track with new distance
                        try {
                            val updatedTrack = track.copy(distanceMeters = totalDistance)
                            trackRepository.updateTrack(updatedTrack)
                            currentTrack = updatedTrack
                        } catch (e: Exception) {
                            ErrorLogger.logError(
                                this@LocationTrackingService,
                                e,
                                "Failed to update track distance, but continuing recording"
                            )
                            // Продолжаем запись даже если не удалось обновить трек
                        }

                        // Update notification
                        try {
                            updateNotification()
                        } catch (e: Exception) {
                            ErrorLogger.logError(
                                this@LocationTrackingService,
                                e,
                                "Failed to update notification, but continuing recording"
                            )
                            // Продолжаем запись даже если не удалось обновить уведомление
                        }

                        lastLocation = location
                    } catch (e: Exception) {
                        ErrorLogger.logError(
                            this@LocationTrackingService,
                            e,
                            "Error processing location update, but continuing recording"
                        )
                        // Продолжаем запись даже при ошибке обработки локации
                    }
                }
            } catch (e: Exception) {
                ErrorLogger.logError(
                    this@LocationTrackingService,
                    e,
                    "Error in onLocationUpdate, but continuing recording"
                )
                // Продолжаем запись даже при критической ошибке
            }
        }
    }

    private fun createNotification(): Notification {
        return try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            val stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.recording_notification_title))
                .setContentText(getString(R.string.recording_notification_content))
                .setSmallIcon(R.drawable.ic_location_on)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.ic_stop,
                    getString(R.string.stop_recording_action),
                    stopPendingIntent
                )
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        } catch (e: Exception) {
            ErrorLogger.logError(
                this@LocationTrackingService,
                e,
                "Failed to create notification, using fallback"
            )
            // Создаем простое уведомление в случае ошибки
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Recording GPS Track")
                .setContentText("GPS tracking is active")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }

    private fun updateNotification() {
        try {
            val notification = createNotification()
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            ErrorLogger.logError(
                this@LocationTrackingService,
                e,
                "Failed to update notification, but continuing recording"
            )
            // Продолжаем запись даже если не удалось обновить уведомление
        }
    }

    private suspend fun getCurrentLocationSync(): Location? {
        return try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                suspendCancellableCoroutine { continuation ->
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location ->
                            continuation.resume(location)
                        }
                        .addOnFailureListener { exception ->
                            ErrorLogger.logError(
                                this@LocationTrackingService,
                                exception,
                                "Failed to get current location"
                            )
                            continuation.resume(null)
                        }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            ErrorLogger.logError(
                this,
                e,
                "Failed to get current location synchronously"
            )
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}






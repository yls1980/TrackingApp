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
import com.xtrack.utils.RateLimitedLogger
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
    private var totalDistanceMeters = 0f
    private var lastNotificationDistance = 0f
    private var locationCounter = 0

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "route_recording_channel"
        const val ACTION_START_RECORDING = "start_recording"
        const val ACTION_STOP_RECORDING = "stop_recording"
        const val ACTION_PAUSE_RECORDING = "pause_recording"
        const val ACTION_RESUME_RECORDING = "resume_recording"
        const val ACTION_EMERGENCY_STOP = "emergency_stop"
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
            ACTION_EMERGENCY_STOP -> emergencyStop()
        }
        
        return START_STICKY
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT // Изменено с LOW на DEFAULT для включения звука
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
                // Включаем звук уведомлений
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500) // Паттерн вибрации
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
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
                // Используем RateLimitedLogger чтобы не спамить в логи при частых изменениях
                RateLimitedLogger.logMessage(
                    this@LocationTrackingService,
                    "Location availability changed: isLocationAvailable=${locationAvailability.isLocationAvailable}",
                    ErrorLogger.LogLevel.INFO,
                    key = "location_availability_${locationAvailability.isLocationAvailable}"
                )
            }
        }
    }

    private fun startNetworkMonitoring() {
        lifecycleScope.launch {
            try {
                NetworkUtils.networkStateFlow(this@LocationTrackingService).collect { isNetworkAvailable ->
                    if (isNetworkAvailable) {
                        // Используем RateLimitedLogger чтобы не спамить при частых переключениях сети
                        RateLimitedLogger.logMessage(
                            this@LocationTrackingService,
                            "Network available, syncing buffered points",
                            ErrorLogger.LogLevel.INFO,
                            key = "network_available"
                        )
                        // Синхронизируем буферизованные точки при восстановлении сети
                        bufferedPointsSyncService.syncAllBufferedPoints()
                        // Очищаем синхронизированные точки
                        bufferedPointsSyncService.cleanupSyncedPoints()
                    } else {
                        // Используем RateLimitedLogger чтобы не спамить при частых переключениях сети
                        RateLimitedLogger.logMessage(
                            this@LocationTrackingService,
                            "Network unavailable, points will be buffered",
                            ErrorLogger.LogLevel.WARNING,
                            key = "network_unavailable"
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
        lifecycleScope.launch {
            try {
                // Сначала убеждаемся, что настройки инициализированы
                settingsRepository.insertDefaultSettings()
                val settings = settingsRepository.getSettingsSync() ?: AppSettings()
                ErrorLogger.logMessage(
                    this@LocationTrackingService,
                    "Starting recording with settings: interval=${settings.locationIntervalMs}ms, accuracy=${settings.locationAccuracy}",
                    ErrorLogger.LogLevel.INFO
                )
                
                // Если уже есть активный трек, просто возобновляем запись
                if (currentTrack != null) {
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Resuming recording for existing track: ${currentTrack?.id}",
                        ErrorLogger.LogLevel.INFO
                    )
                    startLocationUpdates(settings)
                    return@launch
                }
                
                // Проверяем, есть ли незавершенный трек для продолжения
                val existingTrack = trackRepository.getLastIncompleteTrack()
                if (existingTrack != null) {
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Found existing incomplete track to resume: ${existingTrack.id}",
                        ErrorLogger.LogLevel.INFO
                    )
                    currentTrack = existingTrack
                    startLocationUpdates(settings)
                    return@launch
                }
                
                // Создаем новый трек
                ErrorLogger.logMessage(
                    this@LocationTrackingService,
                    "Creating new track",
                    ErrorLogger.LogLevel.INFO
                )
                
                // Проверяем доступность GPS, но не блокируем запуск
                val currentLocation = getCurrentLocationSync()
                if (currentLocation == null) {
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "GPS not available at start, will wait for first location update",
                        ErrorLogger.LogLevel.INFO
                    )
                } else {
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "GPS available at start, current location: ${currentLocation.latitude}, ${currentLocation.longitude}",
                        ErrorLogger.LogLevel.INFO
                    )
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
            totalDistanceMeters = 0f
            lastNotificationDistance = 0f
            
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
                // Сначала синхронизируем буферизованные точки
                try {
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Starting buffered points sync for track: ${track.id}",
                        ErrorLogger.LogLevel.INFO
                    )
                    bufferedPointsSyncService.syncAllBufferedPoints()
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Buffered points synced before checking track validity",
                        ErrorLogger.LogLevel.INFO
                    )
                } catch (e: Exception) {
                    ErrorLogger.logError(
                        this@LocationTrackingService,
                        e,
                        "Failed to sync buffered points before saving track"
                    )
                }
                
                // Теперь проверяем, есть ли реальные точки геопозиции в маршруте
                val trackPoints = trackRepository.getTrackPointsSync(track.id)
                
                if (trackPoints.isEmpty()) {
                    // Если точек нет даже после синхронизации, удаляем маршрут
                    trackRepository.deleteTrackById(track.id)
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Track deleted - no GPS points recorded after sync: ${track.id}",
                        ErrorLogger.LogLevel.INFO
                    )
                } else {
                    
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
                    
                    // Рассчитываем набор высоты
                    val elevationGain = calculateElevationGain(finalTrackPoints)
                    
                    // Если есть точки, сохраняем маршрут
                    val updatedTrack = track.copy(
                        endedAt = Clock.System.now(),
                        isRecording = false,
                        durationSec = startTime?.let { 
                            Clock.System.now().epochSeconds - it.epochSeconds 
                        } ?: 0,
                        distanceMeters = recalculatedDistance,
                        elevationGainMeters = elevationGain
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
            
            // Сохраняем состояние остановки записи в настройках
            try {
                val settings = settingsRepository.getSettingsSync() ?: AppSettings()
                settingsRepository.updateSettings(settings.copy(appExitState = com.xtrack.data.model.AppExitState.STOPPED))
                ErrorLogger.logMessage(
                    this@LocationTrackingService,
                    "Recording state cleared in settings",
                    ErrorLogger.LogLevel.INFO
                )
            } catch (e: Exception) {
                ErrorLogger.logError(
                    this@LocationTrackingService,
                    e,
                    "Failed to clear recording state in settings"
                )
            }
            
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
        // Логируем только важные обновления местоположения с ограничением частоты
        RateLimitedLogger.logMessage(
            this@LocationTrackingService,
            "Location update received: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m",
            ErrorLogger.LogLevel.INFO,
            "location_update"
        )
        locationCounter++
        
        if (!com.xtrack.utils.LocationUtils.isLocationValid(location)) {
            // Используем RateLimitedLogger чтобы не спамить при частых некорректных координатах
            RateLimitedLogger.logMessage(
                this@LocationTrackingService,
                "Invalid location received, skipping: accuracy=${location.accuracy}m, lat=${location.latitude}, lon=${location.longitude}",
                ErrorLogger.LogLevel.WARNING,
                key = "invalid_location"
            )
            return
        }

        lifecycleScope.launch {
            try {
                // Сначала убеждаемся, что настройки инициализированы
                settingsRepository.insertDefaultSettings()
                val settings = settingsRepository.getSettingsSync() ?: AppSettings()
                
                // Логируем загрузку настроек для отладки
                ErrorLogger.logMessage(
                    this@LocationTrackingService,
                    "Settings loaded: distanceNotificationsEnabled=${settings.distanceNotificationsEnabled}, interval=${settings.distanceNotificationIntervalMeters}m",
                    ErrorLogger.LogLevel.INFO
                )
                
                // Check accuracy threshold - логируем только при превышении порога
                if (location.accuracy > settings.accuracyThresholdMeters) {
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Checking accuracy: location=${location.accuracy}m, threshold=${settings.accuracyThresholdMeters}m",
                        ErrorLogger.LogLevel.INFO
                    )
                }
                
                if (location.accuracy > settings.accuracyThresholdMeters) {
                    // Используем RateLimitedLogger чтобы не спамить при частых превышениях порога точности
                    RateLimitedLogger.logMessage(
                        this@LocationTrackingService,
                        "Location accuracy too low (${location.accuracy}m > ${settings.accuracyThresholdMeters}m), skipping point",
                        ErrorLogger.LogLevel.WARNING,
                        key = "accuracy_too_low"
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
                            
                            // Логируем расстояние только при превышении минимального порога с ограничением частоты
                            if (distance >= settings.minDistanceMeters) {
                                RateLimitedLogger.logMessage(
                                    this@LocationTrackingService,
                                    "Distance from last point: ${distance}m, min distance: ${settings.minDistanceMeters}m",
                                    ErrorLogger.LogLevel.INFO,
                                    "distance_check"
                                )
                            }
                            
                            // Skip if distance is too small (same location)
                            if (distance < settings.minDistanceMeters) {
                                // Используем RateLimitedLogger чтобы не спамить при частых маленьких расстояниях
                                RateLimitedLogger.logMessage(
                                    this@LocationTrackingService,
                                    "Distance too small (${distance}m < ${settings.minDistanceMeters}m), skipping point",
                                    ErrorLogger.LogLevel.WARNING,
                                    key = "distance_too_small"
                                )
                                return@launch
                            }
                            
                            totalDistance += distance
                            totalDistanceMeters += distance.toFloat()
                            
                            // Проверяем, нужно ли показать уведомление о пройденном расстоянии
                            if (settings.distanceNotificationsEnabled) {
                                val notificationInterval = settings.distanceNotificationIntervalMeters
                                if (totalDistanceMeters - lastNotificationDistance >= notificationInterval) {
                                    lastNotificationDistance = totalDistanceMeters
                                    showDistanceNotification(totalDistanceMeters)
                                }
                            }
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
                                // Логируем сохранение с ограничением частоты
                                RateLimitedLogger.logMessage(
                                    this@LocationTrackingService,
                                    "Track point saved directly: ${trackPoint.latitude}, ${trackPoint.longitude} for track: ${track.id}",
                                    ErrorLogger.LogLevel.INFO,
                                    "track_point_saved"
                                )
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
            // Проверяем разрешение на показ уведомлений (только для Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                    // Используем RateLimitedLogger чтобы не спамить в логи
                    RateLimitedLogger.logMessage(
                        this,
                        "POST_NOTIFICATIONS permission not granted, cannot update notification",
                        ErrorLogger.LogLevel.WARNING,
                        key = "post_notifications_permission_update"
                    )
                    return
                }
            }
            
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

    private fun calculateElevationGain(trackPoints: List<com.xtrack.data.model.TrackPoint>): Double {
        if (trackPoints.size < 2) return 0.0
        
        var totalGain = 0.0
        var lastValidElevation: Double? = null
        
        // Фильтруем точки с валидными данными о высоте
        val validPoints = trackPoints.filter { 
            it.altitude != null && it.altitude!! > -1000 && it.altitude!! < 10000 
        }
        
        if (validPoints.size < 2) return 0.0
        
        validPoints.forEach { point ->
            val currentElevation = point.altitude!!
            
            lastValidElevation?.let { last ->
                val gain = currentElevation - last
                // Добавляем только положительные изменения высоты (подъем)
                // И игнорируем слишком большие скачки (возможные ошибки GPS)
                if (gain > 0 && gain < 100) { // Максимальный разумный подъем за один шаг - 100м
                    totalGain += gain
                }
            }
            lastValidElevation = currentElevation
        }
        
        return totalGain
    }

    private fun showDistanceNotification(distanceMeters: Float) {
        try {
            val notificationManager = NotificationManagerCompat.from(this)
            
            // Проверяем разрешение на показ уведомлений (только для Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                    // Используем RateLimitedLogger чтобы не спамить в логи
                    RateLimitedLogger.logMessage(
                        this,
                        "POST_NOTIFICATIONS permission not granted, cannot show distance notification",
                        ErrorLogger.LogLevel.WARNING,
                        key = "post_notifications_permission_distance"
                    )
                    return
                }
            }
            
            val distanceText = if (distanceMeters >= 1000) {
                "${String.format("%.1f", distanceMeters / 1000)} км"
            } else {
                "${distanceMeters.toInt()} м"
            }
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_location_on)
                .setContentTitle("Маршрут записывается")
                .setContentText("Пройдено: $distanceText")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Изменено с LOW на DEFAULT для звука
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Включаем звук, вибрацию и свет
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // Явно указываем звук
                .setVibrate(longArrayOf(0, 500, 200, 500)) // Паттерн вибрации
                .build()
            
            // Используем уникальный ID для каждого уведомления о расстоянии
            val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            notificationManager.notify(notificationId, notification)
            
            ErrorLogger.logMessage(
                this,
                "Distance notification shown: $distanceText",
                ErrorLogger.LogLevel.INFO
            )
            
        } catch (e: Exception) {
            ErrorLogger.logError(
                this,
                e,
                "Failed to show distance notification"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
    
    /**
     * Аварийная остановка записи при неожиданном завершении приложения
     */
    private fun emergencyStop() {
        lifecycleScope.launch {
            try {
                ErrorLogger.logMessage(
                    this@LocationTrackingService,
                    "Emergency stop triggered - finalizing current track if exists",
                    ErrorLogger.LogLevel.WARNING
                )
                
                currentTrack?.let { track ->
                    // Сначала синхронизируем буферизованные точки
                    try {
                        bufferedPointsSyncService.syncAllBufferedPoints()
                        ErrorLogger.logMessage(
                            this@LocationTrackingService,
                            "Buffered points synced during emergency stop",
                            ErrorLogger.LogLevel.INFO
                        )
                    } catch (e: Exception) {
                        ErrorLogger.logError(
                            this@LocationTrackingService,
                            e,
                            "Failed to sync buffered points during emergency stop"
                        )
                    }
                    
                    // Получаем все точки трека
                    val trackPoints = trackRepository.getTrackPointsSync(track.id)
                    
                    if (trackPoints.isNotEmpty()) {
                        // Пересчитываем расстояние
                        val recalculatedDistance = com.xtrack.utils.LocationUtils.calculateTotalDistanceFromTrackPoints(trackPoints)
                        val elevationGain = calculateElevationGain(trackPoints)
                        
                        // Сохраняем трек как завершенный
                        val updatedTrack = track.copy(
                            endedAt = Clock.System.now(),
                            isRecording = false,
                            durationSec = startTime?.let { 
                                Clock.System.now().epochSeconds - it.epochSeconds 
                            } ?: 0,
                            distanceMeters = recalculatedDistance,
                            elevationGainMeters = elevationGain
                        )
                        
                        trackRepository.updateTrack(updatedTrack)
                        ErrorLogger.logMessage(
                            this@LocationTrackingService,
                            "Emergency stop: Track saved with ${trackPoints.size} GPS points, distance: ${recalculatedDistance}m",
                            ErrorLogger.LogLevel.INFO
                        )
                    } else {
                        // Нет точек - удаляем трек
                        trackRepository.deleteTrackById(track.id)
                        ErrorLogger.logMessage(
                            this@LocationTrackingService,
                            "Emergency stop: Track deleted - no GPS points recorded",
                            ErrorLogger.LogLevel.INFO
                        )
                    }
                }
                
                // Очищаем состояние
                currentTrack = null
                stopLocationUpdates()
                stopForeground(true)
                
                // Очищаем состояние записи в настройках
                try {
                    val settings = settingsRepository.getSettingsSync() ?: AppSettings()
                    settingsRepository.updateSettings(settings.copy(appExitState = com.xtrack.data.model.AppExitState.STOPPED))
                    ErrorLogger.logMessage(
                        this@LocationTrackingService,
                        "Emergency stop: Recording state cleared in settings",
                        ErrorLogger.LogLevel.INFO
                    )
                } catch (e: Exception) {
                    ErrorLogger.logError(
                        this@LocationTrackingService,
                        e,
                        "Failed to clear recording state during emergency stop"
                    )
                }
                
                stopSelf()
                
            } catch (e: Exception) {
                ErrorLogger.logError(
                    this@LocationTrackingService,
                    e,
                    "Failed to perform emergency stop"
                )
                // Принудительно останавливаем сервис даже при ошибке
                stopSelf()
            }
        }
    }
}






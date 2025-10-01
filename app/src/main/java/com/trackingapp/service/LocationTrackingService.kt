package com.trackingapp.service

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
import com.trackingapp.R
import com.trackingapp.data.model.Track
import com.trackingapp.data.model.TrackPoint
import com.trackingapp.data.repository.SettingsRepository
import com.trackingapp.data.repository.TrackRepository
import com.trackingapp.presentation.MainActivity
import com.trackingapp.utils.LocationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : LifecycleService() {

    @Inject
    lateinit var trackRepository: TrackRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentTrack: Track? = null
    private var lastLocation: Location? = null
    private var totalDistance = 0.0
    private var startTime: kotlinx.datetime.Instant? = null

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
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
                locationResult.lastLocation?.let { location ->
                    onLocationUpdate(location)
                }
            }
        }
    }

    private fun startRecording() {
        if (currentTrack != null) return // Already recording

        lifecycleScope.launch {
            val settings = settingsRepository.getSettings().first()
            
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
            }
            
            startLocationUpdates(settings)
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    private fun stopRecording() {
        lifecycleScope.launch {
            currentTrack?.let { track ->
                val updatedTrack = track.copy(
                    endedAt = Clock.System.now(),
                    isRecording = false,
                    durationSec = startTime?.let { 
                        Clock.System.now().epochSeconds - it.epochSeconds 
                    } ?: 0,
                    distanceMeters = totalDistance
                )
                
                trackRepository.updateTrack(updatedTrack)
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
            val settings = settingsRepository.getSettings().first()
            startLocationUpdates(settings)
        }
    }

    private fun startLocationUpdates(settings: com.trackingapp.data.model.AppSettings) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun onLocationUpdate(location: Location) {
        if (!com.trackingapp.utils.LocationUtils.isLocationValid(location)) return

        lifecycleScope.launch {
            val settings = settingsRepository.getSettings().first()
            
            // Check accuracy threshold
            if (location.accuracy > settings.accuracyThresholdMeters) return@launch

            currentTrack?.let { track ->
                // Calculate distance from last point
                lastLocation?.let { lastLoc ->
                    val distance = com.trackingapp.utils.LocationUtils.calculateDistance(
                        lastLoc.latitude, lastLoc.longitude,
                        location.latitude, location.longitude
                    )
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

                // Save track point
                trackRepository.insertTrackPoint(trackPoint)

                // Update track with new distance
                val updatedTrack = track.copy(distanceMeters = totalDistance)
                trackRepository.updateTrack(updatedTrack)
                currentTrack = updatedTrack

                // Update notification
                updateNotification()

                lastLocation = location
            }
        }
    }

    private fun createNotification(): Notification {
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
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
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}




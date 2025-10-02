package com.xtrack.presentation.viewmodel

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.xtrack.data.model.LastLocation
import com.xtrack.data.model.Point
import com.xtrack.data.model.Track
import com.xtrack.data.repository.LastLocationRepository
import com.xtrack.data.repository.SettingsRepository
import com.xtrack.data.repository.TrackRepository
import com.xtrack.service.LocationTrackingService
import com.xtrack.utils.ErrorLogger
import com.xtrack.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import kotlin.coroutines.resume
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val trackRepository: TrackRepository,
    private val settingsRepository: SettingsRepository,
    private val lastLocationRepository: LastLocationRepository
) : AndroidViewModel(application) {

    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(application)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _trackPoints = MutableStateFlow<List<com.xtrack.data.model.TrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<com.xtrack.data.model.TrackPoint>> = _trackPoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<Point?>(null)
    val currentLocation: StateFlow<Point?> = _currentLocation.asStateFlow()

    val allTracks: StateFlow<List<Track>> = trackRepository.getAllTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Сначала пытаемся очистить базу данных при проблемах с миграцией
        clearDatabaseIfNeeded()
        checkCurrentRecording()
        loadLastLocation()
        // Автоматически позиционируемся на текущую/последнюю точку при запуске
        centerOnCurrentOrLastLocation()
    }
    
    private fun clearDatabaseIfNeeded() {
        viewModelScope.launch {
            try {
                // Пытаемся получить настройки - если это не удается, значит проблема с миграцией
                settingsRepository.getSettingsSync()
            } catch (e: Exception) {
                if (e.message?.contains("Room cannot verify the data integrity") == true) {
                    ErrorLogger.logMessage(
                        getApplication(),
                        "Database migration failed, clearing database",
                        ErrorLogger.LogLevel.WARNING
                    )
                    // Очищаем базу данных
                    com.xtrack.data.database.TrackingDatabase.clearDatabase(getApplication())
                }
            }
        }
    }

    private fun checkCurrentRecording() {
        viewModelScope.launch {
            try {
                val currentRecordingTrack = trackRepository.getCurrentRecordingTrack()
                if (currentRecordingTrack != null) {
                    _isRecording.value = true
                    _currentTrack.value = currentRecordingTrack
                    loadTrackPoints(currentRecordingTrack.id)
                    ErrorLogger.logMessage(
                        getApplication(),
                        "Found active recording track: ${currentRecordingTrack.id}",
                        ErrorLogger.LogLevel.INFO
                    )
                } else {
                    // Проверяем сохраненное состояние записи
                    val settings = settingsRepository.getSettingsSync()
                    if (settings?.wasRecordingOnExit == true) {
                        ErrorLogger.logMessage(
                            getApplication(),
                            "App was recording on exit, but no active track found. This might indicate an unexpected shutdown.",
                            ErrorLogger.LogLevel.WARNING
                        )
                        // Сбрасываем флаг, так как активной записи нет
                        settingsRepository.updateSettings(settings.copy(wasRecordingOnExit = false))
                    }
                }
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to check current recording state"
                )
            }
        }
    }

    private fun loadLastLocation() {
        viewModelScope.launch {
            try {
                lastLocationRepository.getLastLocation().collect { lastLocation ->
                    lastLocation?.let { location ->
                        _currentLocation.value = Point(location.latitude, location.longitude)
                        ErrorLogger.logMessage(
                            getApplication(),
                            "Last location loaded: ${location.latitude}, ${location.longitude}",
                            ErrorLogger.LogLevel.INFO
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Игнорируем отмену корутины - это нормальное поведение при уничтожении ViewModel
                ErrorLogger.logMessage(
                    getApplication(),
                    "Last location loading was cancelled (normal behavior)",
                    ErrorLogger.LogLevel.DEBUG
                )
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to load last location"
                )
            }
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            try {
                val intent = Intent(getApplication(), LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.ACTION_START_RECORDING
                }
                getApplication<Application>().startForegroundService(intent)
                _isRecording.value = true
                
                // Сохраняем состояние записи в настройках
                val settings = settingsRepository.getSettingsSync() ?: com.xtrack.data.model.AppSettings()
                settingsRepository.updateSettings(settings.copy(wasRecordingOnExit = true))
                
                ErrorLogger.logMessage(
                    getApplication(),
                    "Recording started and state saved",
                    ErrorLogger.LogLevel.INFO
                )
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to start recording"
                )
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                // Проверяем, есть ли точки в текущем маршруте перед остановкой
                val currentTrack = _currentTrack.value
                val trackPoints = _trackPoints.value
                
                if (currentTrack != null && trackPoints.isEmpty()) {
                    ErrorLogger.logMessage(
                        getApplication(),
                        "Stopping recording - no GPS points recorded, track will be deleted",
                        ErrorLogger.LogLevel.INFO
                    )
                } else if (currentTrack != null) {
                    ErrorLogger.logMessage(
                        getApplication(),
                        "Stopping recording - ${trackPoints.size} GPS points recorded, track will be saved",
                        ErrorLogger.LogLevel.INFO
                    )
                }
                
                val intent = Intent(getApplication(), LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.ACTION_STOP_RECORDING
                }
                getApplication<Application>().startService(intent)
                _isRecording.value = false
                _currentTrack.value = null
                _trackPoints.value = emptyList()
                
                // Сохраняем состояние остановки записи в настройках
                val settings = settingsRepository.getSettingsSync() ?: com.xtrack.data.model.AppSettings()
                settingsRepository.updateSettings(settings.copy(wasRecordingOnExit = false))
                
                ErrorLogger.logMessage(
                    getApplication(),
                    "Recording stopped and state saved",
                    ErrorLogger.LogLevel.INFO
                )
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to stop recording"
                )
            }
        }
    }

    fun pauseRecording() {
        val intent = Intent(getApplication(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_PAUSE_RECORDING
        }
        getApplication<Application>().startService(intent)
    }

    fun resumeRecording() {
        val intent = Intent(getApplication(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_RESUME_RECORDING
        }
        getApplication<Application>().startService(intent)
    }

    fun loadTrackPoints(trackId: String) {
        viewModelScope.launch {
            try {
                trackRepository.getTrackPoints(trackId)
                    .collect { points ->
                        _trackPoints.value = points
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Игнорируем отмену корутины - это нормальное поведение при уничтожении ViewModel
                ErrorLogger.logMessage(
                    getApplication(),
                    "Track points loading was cancelled (normal behavior)",
                    ErrorLogger.LogLevel.DEBUG
                )
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to load track points for track: $trackId"
                )
                // Устанавливаем пустой список в случае ошибки
                _trackPoints.value = emptyList()
            }
        }
    }

    fun updateCurrentLocation(point: Point) {
        _currentLocation.value = point
        saveLastLocation(point)
    }

    private fun saveLastLocation(point: Point) {
        viewModelScope.launch {
            try {
                val lastLocation = LastLocation(
                    id = 1,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    timestamp = Clock.System.now(),
                    accuracy = null
                )
                lastLocationRepository.saveLastLocation(lastLocation)
                ErrorLogger.logMessage(
                    getApplication(),
                    "Last location saved: ${point.latitude}, ${point.longitude}",
                    ErrorLogger.LogLevel.INFO
                )
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to save last location"
                )
            }
        }
    }

    fun formatDistance(distanceMeters: Double): String {
        return LocationUtils.formatDistance(distanceMeters)
    }

    fun formatDuration(durationSeconds: Long): String {
        return LocationUtils.formatDuration(durationSeconds)
    }

    fun formatSpeed(speedMs: Float): String {
        return LocationUtils.formatSpeed(speedMs)
    }

    fun calculateTotalDistance(): Double {
        val points = _trackPoints.value
        if (points.size < 2) return 0.0
        
        val pointList = points.map { Point(it.latitude, it.longitude) }
        return LocationUtils.calculateTotalDistance(pointList)
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                if (ContextCompat.checkSelfPermission(
                        getApplication(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val location = getLastKnownLocation()
                    location?.let { loc ->
                        val point = Point(loc.latitude, loc.longitude)
                        _currentLocation.value = point
                        ErrorLogger.logMessage(
                            getApplication(),
                            "Current location updated: ${loc.latitude}, ${loc.longitude}",
                            ErrorLogger.LogLevel.INFO
                        )
                    }
                } else {
                    ErrorLogger.logMessage(
                        getApplication(),
                        "Location permission not granted",
                        ErrorLogger.LogLevel.WARNING
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Игнорируем отмену корутины - это нормальное поведение при уничтожении ViewModel
                ErrorLogger.logMessage(
                    getApplication(),
                    "Current location update was cancelled (normal behavior)",
                    ErrorLogger.LogLevel.DEBUG
                )
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to get current location"
                )
            }
        }
    }
    
    fun centerOnCurrentOrLastLocation() {
        viewModelScope.launch {
            try {
                if (ContextCompat.checkSelfPermission(
                        getApplication(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Сначала пробуем получить текущее местоположение
                    val location = getLastKnownLocation()
                    location?.let { loc ->
                        val point = Point(loc.latitude, loc.longitude)
                        _currentLocation.value = point
                        ErrorLogger.logMessage(
                            getApplication(),
                            "Centered on current location: ${loc.latitude}, ${loc.longitude}",
                            ErrorLogger.LogLevel.INFO
                        )
                        return@launch
                    }
                }
                
                // Если не удалось получить текущее местоположение, загружаем последнее сохраненное
                lastLocationRepository.getLastLocation().collect { lastLocation ->
                    lastLocation?.let { location ->
                        _currentLocation.value = Point(location.latitude, location.longitude)
                        ErrorLogger.logMessage(
                            getApplication(),
                            "Centered on last saved location: ${location.latitude}, ${location.longitude}",
                            ErrorLogger.LogLevel.INFO
                        )
                    }
                    return@collect // Получаем только первое значение
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Игнорируем отмену корутины - это нормальное поведение при уничтожении ViewModel
                ErrorLogger.logMessage(
                    getApplication(),
                    "Location centering was cancelled (normal behavior)",
                    ErrorLogger.LogLevel.DEBUG
                )
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to center on current or last location"
                )
            }
        }
    }

    private suspend fun getLastKnownLocation(): Location? = suspendCancellableCoroutine { continuation ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                continuation.resume(location)
            }
            .addOnFailureListener { exception ->
                ErrorLogger.logError(
                    getApplication(),
                    exception,
                    "Failed to get last known location"
                )
                continuation.resume(null)
            }
    }
}


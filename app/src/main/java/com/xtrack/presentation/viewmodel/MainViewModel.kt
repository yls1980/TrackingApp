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
import com.xtrack.utils.RateLimitedLogger
import com.xtrack.data.model.AppSettings
import com.xtrack.data.model.AppExitState
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
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val allTracks: StateFlow<List<Track>> = trackRepository.getAllTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Инициализируем настройки и проверяем состояние записи
        initializeSettings()
        checkCurrentRecording()
        loadLastLocation()
        // Автоматически позиционируемся на текущую/последнюю точку при запуске
        centerOnCurrentOrLastLocation()
    }
    
    private fun initializeSettings() {
        viewModelScope.launch {
            try {
                // Пытаемся получить настройки - если это не удается, Room автоматически 
                // применит fallbackToDestructiveMigration() из TrackingDatabase
                settingsRepository.getSettingsSync()
                ErrorLogger.logMessage(
                    getApplication(),
                    "Settings loaded successfully during initialization",
                    ErrorLogger.LogLevel.INFO
                )
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to load settings during initialization - Room will handle migration automatically"
                )
                // Room автоматически применит fallbackToDestructiveMigration() при следующем обращении к БД
            }
        }
    }

    private fun checkCurrentRecording() {
        viewModelScope.launch {
            try {
                // Сначала проверяем, действительно ли сервис работает
                val isServiceRunning = com.xtrack.utils.ServiceUtils.isServiceRunning(
                    getApplication(), 
                    LocationTrackingService::class.java
                )
                
                if (isServiceRunning) {
                    // Сервис работает - проверяем активный трек
                    val currentRecordingTrack = trackRepository.getCurrentRecordingTrack()
                    if (currentRecordingTrack != null) {
                        _isRecording.value = true
                        _currentTrack.value = currentRecordingTrack
                        loadTrackPoints(currentRecordingTrack.id)
                        ErrorLogger.logMessage(
                            getApplication(),
                            "Service is running, found active recording track: ${currentRecordingTrack.id}",
                            ErrorLogger.LogLevel.INFO
                        )
                    } else {
                        // Позиционируемся на карте
                        centerOnCurrentOrLastLocation()
                        
                        ErrorLogger.logMessage(
                            getApplication(),
                            "Service is running but no active track found - normal state after emergency stop",
                            ErrorLogger.LogLevel.INFO
                        )
                    }
                } else {
                    // Сервис не работает - проверяем состояние приложения при выходе
                    val settings = settingsRepository.getSettingsSync()
                    
                    when (settings?.appExitState) {
                        AppExitState.RECORDING -> {
                            ErrorLogger.logMessage(
                                getApplication(),
                                "App was recording on exit, but service is not running. Attempting to resume recording.",
                                ErrorLogger.LogLevel.INFO
                            )
                            
                            // Сначала выполняем аварийную остановку для завершения предыдущего трека
                            performEmergencyStop()
                            
                            // Затем пытаемся восстановить запись трека
                            resumeRecordingAfterAppRestart()
                        }
                        
                        AppExitState.STOPPED -> {
                            ErrorLogger.logMessage(
                                getApplication(),
                                "App was stopped normally on exit - initializing without recording",
                                ErrorLogger.LogLevel.INFO
                            )
                            
                            // Приложение было остановлено нормально (зеленая кнопка)
                            // Просто позиционируемся на карте без статистики и записи
                            _isRecording.value = false
                            _currentTrack.value = null
                            _trackPoints.value = emptyList()
                            
                            // Позиционируемся на текущую GPS позицию или последнюю сохраненную
                            centerOnCurrentOrLastLocation()
                        }
                        
                        null -> {
                            // Настройки не найдены - безопасная инициализация
                            ErrorLogger.logMessage(
                                getApplication(),
                                "No app exit state found - initializing in stopped state",
                                ErrorLogger.LogLevel.INFO
                            )
                            
                            _isRecording.value = false
                            _currentTrack.value = null
                            _trackPoints.value = emptyList()
                            
                            // Позиционируемся на карте
                            centerOnCurrentOrLastLocation()
                        }
                    }
                }
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to check current recording state"
                )
                // В случае ошибки инициализируем в состоянии остановки
                _isRecording.value = false
                _currentTrack.value = null
                _trackPoints.value = emptyList()
            }
        }
    }

    private fun loadLastLocation() {
        viewModelScope.launch {
            try {
                val lastLocation = lastLocationRepository.getLastLocation().first()
                lastLocation?.let { location ->
                    _currentLocation.value = Point(location.latitude, location.longitude)
                    ErrorLogger.logMessage(
                        getApplication(),
                        "Last location loaded: ${location.latitude}, ${location.longitude}",
                        ErrorLogger.LogLevel.INFO
                    )
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
                // Проверяем, включена ли геолокация на устройстве
                if (!LocationUtils.isLocationEnabled(getApplication())) {
                    _errorMessage.value = "Включите геолокацию на устройстве для начала записи маршрута"
                    ErrorLogger.logMessage(
                        getApplication(),
                        "Cannot start recording: location services are disabled",
                        ErrorLogger.LogLevel.WARNING
                    )
                    return@launch
                }
                
                // Проверяем разрешения на геолокацию
                if (ContextCompat.checkSelfPermission(
                        getApplication(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    _errorMessage.value = "Разрешите доступ к геолокации для записи маршрута"
                    ErrorLogger.logMessage(
                        getApplication(),
                        "Cannot start recording: location permission not granted",
                        ErrorLogger.LogLevel.WARNING
                    )
                    return@launch
                }
                
                val intent = Intent(getApplication(), LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.ACTION_START_RECORDING
                }
                getApplication<Application>().startForegroundService(intent)
                _isRecording.value = true
                
                // Сохраняем состояние записи в настройках
                val settings = settingsRepository.getSettingsSync() ?: com.xtrack.data.model.AppSettings()
                settingsRepository.updateSettings(settings.copy(appExitState = AppExitState.RECORDING))
                
                ErrorLogger.logMessage(
                    getApplication(),
                    "Recording started and state saved",
                    ErrorLogger.LogLevel.INFO
                )
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при запуске записи: ${e.message}"
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to start recording"
                )
            }
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
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
                settingsRepository.updateSettings(settings.copy(appExitState = AppExitState.STOPPED))
                
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
                        // Используем RateLimitedLogger чтобы не спамить при частых обновлениях текущей позиции
                        RateLimitedLogger.logMessage(
                            getApplication(),
                            "Current location updated: ${loc.latitude}, ${loc.longitude}",
                            ErrorLogger.LogLevel.INFO,
                            key = "current_location_updated"
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
    
    /**
     * Выполняет аварийную остановку для завершения предыдущего трека
     */
    private fun performEmergencyStop() {
        viewModelScope.launch {
            try {
                ErrorLogger.logMessage(
                    getApplication(),
                    "Performing emergency stop to finalize any incomplete tracks",
                    ErrorLogger.LogLevel.INFO
                )
                
                // Отправляем команду аварийной остановки в сервис
                val emergencyIntent = Intent(getApplication(), LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.ACTION_EMERGENCY_STOP
                }
                getApplication<Application>().startService(emergencyIntent)
                
                // Небольшая задержка для завершения аварийной остановки
                kotlinx.coroutines.delay(1000)
                
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to perform emergency stop"
                )
            }
        }
    }
    
    /**
     * Восстанавливает запись трека после перезапуска приложения
     */
    private fun resumeRecordingAfterAppRestart() {
        viewModelScope.launch {
            try {
                // Ищем последний незавершенный трек
                val lastIncompleteTrack = trackRepository.getLastIncompleteTrack()
                
                if (lastIncompleteTrack != null) {
                    ErrorLogger.logMessage(
                        getApplication(),
                        "Found incomplete track to resume: ${lastIncompleteTrack.id}",
                        ErrorLogger.LogLevel.INFO
                    )
                    
                    // Получаем точки этого трека
                    val trackPoints = trackRepository.getTrackPointsSync(lastIncompleteTrack.id)
                    
                    if (trackPoints.isNotEmpty()) {
                        // Обновляем UI с найденным треком
                        _currentTrack.value = lastIncompleteTrack
                        _trackPoints.value = trackPoints
                        _isRecording.value = true
                        
                        // Позиционируемся на последнюю точку трека
                        val lastPoint = trackPoints.last()
                        _currentLocation.value = Point(lastPoint.latitude, lastPoint.longitude)
                        
                        // Запускаем сервис записи для продолжения
                        val intent = Intent(getApplication(), LocationTrackingService::class.java).apply {
                            action = LocationTrackingService.ACTION_START_RECORDING
                        }
                        getApplication<Application>().startService(intent)
                        
                        // Сохраняем состояние записи
                        val settings = settingsRepository.getSettingsSync() ?: AppSettings()
                        settingsRepository.updateSettings(settings.copy(appExitState = AppExitState.RECORDING))
                        
                        ErrorLogger.logMessage(
                            getApplication(),
                            "Recording resumed for track: ${lastIncompleteTrack.id} with ${trackPoints.size} existing points",
                            ErrorLogger.LogLevel.INFO
                        )
                        
                    } else {
                        // Трек без точек - удаляем его
                        trackRepository.deleteTrackById(lastIncompleteTrack.id)
                        ErrorLogger.logMessage(
                            getApplication(),
                            "Deleted incomplete track with no points: ${lastIncompleteTrack.id}",
                            ErrorLogger.LogLevel.INFO
                        )
                        
                        // Сбрасываем состояние
                        _isRecording.value = false
                        _currentTrack.value = null
                        _trackPoints.value = emptyList()
                        
                        // Очищаем флаг записи и позиционируемся на карте
                        val settings = settingsRepository.getSettingsSync() ?: AppSettings()
                        settingsRepository.updateSettings(settings.copy(appExitState = AppExitState.STOPPED))
                        
                        // Позиционируемся на карте после очистки
                        centerOnCurrentOrLastLocation()
                    }
                } else {
                    ErrorLogger.logMessage(
                        getApplication(),
                        "No incomplete track found to resume",
                        ErrorLogger.LogLevel.INFO
                    )
                    
                    // Сбрасываем состояние
                    _isRecording.value = false
                    _currentTrack.value = null
                    _trackPoints.value = emptyList()
                    
                    // Очищаем флаг записи и позиционируемся на карте
                    val settings = settingsRepository.getSettingsSync() ?: AppSettings()
                    settingsRepository.updateSettings(settings.copy(appExitState = AppExitState.STOPPED))
                    
                    // Позиционируемся на карте
                    centerOnCurrentOrLastLocation()
                }
                
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to resume recording after app restart"
                )
                
                // В случае ошибки сбрасываем состояние
                _isRecording.value = false
                _currentTrack.value = null
                _trackPoints.value = emptyList()
                
                // Очищаем флаг записи и позиционируемся на карте
                try {
                    val settings = settingsRepository.getSettingsSync() ?: AppSettings()
                    settingsRepository.updateSettings(settings.copy(appExitState = AppExitState.STOPPED))
                    
                    // Позиционируемся на карте после ошибки
                    centerOnCurrentOrLastLocation()
                } catch (settingsError: Exception) {
                    ErrorLogger.logError(
                        getApplication(),
                        settingsError,
                        "Failed to clear recording flag after resume error"
                    )
                    
                    // Все равно пытаемся позиционироваться
                    centerOnCurrentOrLastLocation()
                }
            }
        }
    }
}


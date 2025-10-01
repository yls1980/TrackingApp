package com.trackingapp.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trackingapp.data.model.Point
import com.trackingapp.data.model.Track
import com.trackingapp.data.repository.SettingsRepository
import com.trackingapp.data.repository.TrackRepository
import com.trackingapp.service.LocationTrackingService
import com.trackingapp.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val trackRepository: TrackRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _trackPoints = MutableStateFlow<List<com.trackingapp.data.model.TrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<com.trackingapp.data.model.TrackPoint>> = _trackPoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<Point?>(null)
    val currentLocation: StateFlow<Point?> = _currentLocation.asStateFlow()

    val allTracks: StateFlow<List<Track>> = trackRepository.getAllTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        checkCurrentRecording()
    }

    private fun checkCurrentRecording() {
        viewModelScope.launch {
            val currentRecordingTrack = trackRepository.getCurrentRecordingTrack()
            if (currentRecordingTrack != null) {
                _isRecording.value = true
                _currentTrack.value = currentRecordingTrack
                loadTrackPoints(currentRecordingTrack.id)
            }
        }
    }

    fun startRecording() {
        val intent = Intent(getApplication(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_RECORDING
        }
        getApplication<Application>().startForegroundService(intent)
        _isRecording.value = true
    }

    fun stopRecording() {
        val intent = Intent(getApplication(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_RECORDING
        }
        getApplication<Application>().startService(intent)
        _isRecording.value = false
        _currentTrack.value = null
        _trackPoints.value = emptyList()
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
            trackRepository.getTrackPoints(trackId)
                .collect { points ->
                    _trackPoints.value = points
                }
        }
    }

    fun updateCurrentLocation(point: Point) {
        _currentLocation.value = point
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
}


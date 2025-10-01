package com.trackingapp.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackingapp.data.model.Track
import com.trackingapp.data.model.TrackPoint
import com.trackingapp.data.parser.GeoJsonGenerator
import com.trackingapp.data.parser.GpxGenerator
import com.trackingapp.data.repository.TrackRepository
import com.trackingapp.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TrackDetailViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val gpxGenerator: GpxGenerator,
    private val geoJsonGenerator: GeoJsonGenerator
) : ViewModel() {

    private val _track = MutableStateFlow<Track?>(null)
    val track: StateFlow<Track?> = _track.asStateFlow()

    private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadTrack(trackId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val track = trackRepository.getTrackById(trackId)
                _track.value = track
                
                if (track != null) {
                    trackRepository.getTrackPoints(trackId)
                        .collect { points ->
                            _trackPoints.value = points
                        }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportGpx(context: Context): Uri? {
        val track = _track.value ?: return null
        val trackPoints = _trackPoints.value
        
        return try {
            val gpxContent = gpxGenerator.generateGpx(track, trackPoints)
            val fileName = "track_${track.id}_${getCurrentDateString()}.gpx"
            val tracksDir = File(context.getExternalFilesDir(null), "tracks")
            if (!tracksDir.exists()) {
                tracksDir.mkdirs()
            }
            val file = File(tracksDir, fileName)
            
            FileOutputStream(file).use { fos ->
                fos.write(gpxContent.toByteArray())
            }
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            _error.value = e.message
            null
        }
    }

    fun exportGeoJson(context: Context): Uri? {
        val track = _track.value ?: return null
        val trackPoints = _trackPoints.value
        
        return try {
            val geoJsonContent = geoJsonGenerator.generateGeoJson(track, trackPoints)
            val fileName = "track_${track.id}_${getCurrentDateString()}.geojson"
            val tracksDir = File(context.getExternalFilesDir(null), "tracks")
            if (!tracksDir.exists()) {
                tracksDir.mkdirs()
            }
            val file = File(tracksDir, fileName)
            
            FileOutputStream(file).use { fos ->
                fos.write(geoJsonContent.toByteArray())
            }
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            _error.value = e.message
            null
        }
    }

    fun shareTrack(context: Context, format: ExportFormat) {
        val uri = when (format) {
            ExportFormat.GPX -> exportGpx(context)
            ExportFormat.GEOJSON -> exportGeoJson(context)
        }
        
        uri?.let { fileUri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when (format) {
                    ExportFormat.GPX -> "application/gpx+xml"
                    ExportFormat.GEOJSON -> "application/geo+json"
                }
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Поделиться маршрутом"))
        }
    }

    fun deleteTrack() {
        viewModelScope.launch {
            _track.value?.let { track ->
                trackRepository.deleteTrack(track)
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

    fun calculateElevationGain(): Double {
        val points = _trackPoints.value
        if (points.size < 2) return 0.0
        
        var totalGain = 0.0
        var lastElevation: Double? = null
        
        points.forEach { point ->
            point.altitude?.let { currentElevation ->
                lastElevation?.let { last ->
                    val gain = currentElevation - last
                    if (gain > 0) {
                        totalGain += gain
                    }
                }
                lastElevation = currentElevation
            }
        }
        
        return totalGain
    }

    fun calculateAverageSpeed(): Float {
        val track = _track.value ?: return 0f
        val duration = track.durationSec
        if (duration <= 0) return 0f
        
        val distanceKm = track.distanceMeters / 1000.0
        val durationHours = duration / 3600.0
        
        return (distanceKm / durationHours).toFloat()
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun clearError() {
        _error.value = null
    }

    enum class ExportFormat {
        GPX,
        GEOJSON
    }
}




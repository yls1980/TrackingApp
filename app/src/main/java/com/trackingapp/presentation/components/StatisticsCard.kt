package com.trackingapp.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trackingapp.data.model.Point
import com.trackingapp.data.model.Track
import com.trackingapp.data.model.TrackPoint
import com.trackingapp.utils.LocationUtils
import kotlinx.datetime.Clock

@Composable
fun StatisticsCard(
    track: Track,
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier
) {
    val currentTime = Clock.System.now()
    val duration = if (track.isRecording) {
        currentTime.epochSeconds - track.startedAt.epochSeconds
    } else {
        track.durationSec
    }

    val totalDistance = if (track.isRecording) {
        LocationUtils.calculateTotalDistance(
            trackPoints.map { Point(it.latitude, it.longitude) }
        )
    } else {
        track.distanceMeters
    }

    val averageSpeed = if (duration > 0) {
        val distanceKm = totalDistance / 1000.0
        val durationHours = duration / 3600.0
        (distanceKm / durationHours).toFloat()
    } else {
        0f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = "Длительность",
                    value = LocationUtils.formatDuration(duration)
                )
                
                StatisticItem(
                    label = "Дистанция",
                    value = LocationUtils.formatDistance(totalDistance)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = "Средняя скорость",
                    value = LocationUtils.formatSpeed(averageSpeed)
                )
                
                StatisticItem(
                    label = "Точек",
                    value = trackPoints.size.toString()
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}


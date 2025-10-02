package com.xtrack.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xtrack.presentation.components.MapView
import com.xtrack.presentation.viewmodel.TrackDetailViewModel
import com.xtrack.utils.ErrorLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailScreen(
    trackId: String,
    onNavigateBack: () -> Unit,
    viewModel: TrackDetailViewModel = hiltViewModel()
) {
    val track by viewModel.track.collectAsStateWithLifecycle()
    val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    
    var showExportMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(trackId) {
        try {
            ErrorLogger.logMessage(
                context,
                "Loading track detail for ID: $trackId",
                ErrorLogger.LogLevel.INFO
            )
            viewModel.loadTrack(trackId)
        } catch (e: Exception) {
            ErrorLogger.logError(
                context,
                e,
                "Failed to load track detail for ID: $trackId"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(track?.name ?: "Маршрут") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ошибка: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            track != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Map
                    MapView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        trackPoints = trackPoints,
                        currentLocation = if (trackPoints.isNotEmpty()) {
                            // Используем первую точку трека как текущую позицию для начального позиционирования
                            com.xtrack.data.model.Point(
                                latitude = trackPoints.first().latitude,
                                longitude = trackPoints.first().longitude
                            )
                        } else null,
                        onLocationUpdate = {},
                        centerOnLocation = false,
                        centerOnTrack = true
                    )

                    // Statistics
                    TrackDetailStatistics(
                        track = track!!,
                        trackPoints = trackPoints,
                        viewModel = viewModel,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Export menu
    if (showExportMenu) {
        ExportMenu(
            onExportGpx = {
                viewModel.shareTrack(context, TrackDetailViewModel.ExportFormat.GPX)
                showExportMenu = false
            },
            onExportGeoJson = {
                viewModel.shareTrack(context, TrackDetailViewModel.ExportFormat.GEOJSON)
                showExportMenu = false
            },
            onDismiss = { showExportMenu = false }
        )
    }

    // Delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить маршрут") },
            text = { Text("Вы уверены, что хотите удалить этот маршрут?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrack()
                        onNavigateBack()
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun TrackDetailStatistics(
    track: com.xtrack.data.model.Track,
    trackPoints: List<com.xtrack.data.model.TrackPoint>,
    viewModel: TrackDetailViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Статистика маршрута",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = "Дистанция",
                    value = viewModel.formatDistance(track.distanceMeters)
                )
                
                StatisticItem(
                    label = "Длительность",
                    value = viewModel.formatDuration(track.durationSec)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = "Средняя скорость",
                    value = viewModel.formatSpeed(viewModel.calculateAverageSpeed())
                )
                
                StatisticItem(
                    label = "Набор высоты",
                    value = "${viewModel.calculateElevationGain().toInt()} м"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            StatisticItem(
                label = "Количество точек",
                value = trackPoints.size.toString()
            )
        }
    }
}

@Composable
private fun ExportMenu(
    onExportGpx: () -> Unit,
    onExportGeoJson: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Экспорт маршрута") },
        text = {
            Column {
                TextButton(
                    onClick = onExportGpx,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Экспорт GPX")
                }
                TextButton(
                    onClick = onExportGeoJson,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Экспорт GeoJSON")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
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
            fontWeight = FontWeight.Bold
        )
    }
}




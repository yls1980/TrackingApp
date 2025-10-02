package com.xtrack.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xtrack.presentation.components.MapView
import com.xtrack.presentation.components.StatisticsCard
import com.xtrack.presentation.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToTracks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAddNote: (Double, Double) -> Unit = { _, _ -> }
) {
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    
    // Логируем состояние для диагностики
    LaunchedEffect(Unit) {
        android.util.Log.d("MainScreen", "MainScreen initialized with onNavigateToAddNote: ${onNavigateToAddNote != null}")
    }
    
    // Состояние для центрирования карты
    var centerOnLocation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Трекер маршрутов") },
                actions = {
                    IconButton(onClick = {
                        viewModel.getCurrentLocation()
                        centerOnLocation = true
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Мое местоположение")
                    }
                    IconButton(onClick = {
                        // Создаем заметку в текущем местоположении
                        currentLocation?.let { location ->
                            onNavigateToAddNote(location.latitude, location.longitude)
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить заметку")
                    }
                    IconButton(onClick = onNavigateToTracks) {
                        Icon(Icons.Default.List, contentDescription = "Маршруты")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording()
                    }
                    // После запуска/остановки записи центрируем карту на текущей/последней точке
                    viewModel.centerOnCurrentOrLastLocation()
                    centerOnLocation = true
                },
                containerColor = if (isRecording) Color.Red else Color.Green
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRecording) "Остановить" else "Начать"
                )
            }
        }
    ) { paddingValues ->
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
                currentLocation = currentLocation,
                onLocationUpdate = viewModel::updateCurrentLocation,
                centerOnLocation = centerOnLocation,
                onLongPress = { lat, lon ->
                    android.util.Log.d("MainScreen", "Long press received: $lat, $lon, isRecording: $isRecording, currentTrack: ${currentTrack?.id}")
                    // Позволяем создавать заметки в любое время
                    // Если идет запись, привязываем к текущему треку
                    // Если нет записи, создаем заметку без привязки к треку
                    android.util.Log.d("MainScreen", "Navigating to add note screen")
                    onNavigateToAddNote(lat, lon)
                }
            )
            
            // Сбрасываем флаг центрирования после использования
            LaunchedEffect(centerOnLocation) {
                if (centerOnLocation) {
                    centerOnLocation = false
                }
            }

            // Statistics
            currentTrack?.let { track ->
                StatisticsCard(
                    track = track,
                    trackPoints = trackPoints,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}


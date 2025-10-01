package com.trackingapp.presentation.screen

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
import com.trackingapp.presentation.components.MapView
import com.trackingapp.presentation.components.StatisticsCard
import com.trackingapp.presentation.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToTracks: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Трекер маршрутов") },
                actions = {
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
                onLocationUpdate = viewModel::updateCurrentLocation
            )

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


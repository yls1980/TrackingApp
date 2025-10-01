package com.trackingapp.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackingapp.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToErrorLogs: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Location accuracy
            SettingsSection(title = "Точность местоположения") {
                val accuracyOptions = viewModel.getLocationAccuracyOptions()
                accuracyOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = settings.locationAccuracy == option.accuracy,
                            onClick = { viewModel.updateLocationAccuracy(option.accuracy) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Location interval
            SettingsSection(title = "Интервал обновления") {
                val intervalOptions = viewModel.getLocationIntervalOptions()
                intervalOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = settings.locationIntervalMs == option.intervalMs,
                            onClick = { viewModel.updateLocationInterval(option.intervalMs) }
                        )
                        Text(
                            text = option.name,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Min distance
            SettingsSection(title = "Минимальная дистанция") {
                val distanceOptions = viewModel.getMinDistanceOptions()
                distanceOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = settings.minDistanceMeters == option.distanceMeters,
                            onClick = { viewModel.updateMinDistance(option.distanceMeters) }
                        )
                        Text(
                            text = option.name,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Accuracy threshold
            SettingsSection(title = "Порог точности") {
                val thresholdOptions = viewModel.getAccuracyThresholdOptions()
                thresholdOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = settings.accuracyThresholdMeters == option.thresholdMeters,
                            onClick = { viewModel.updateAccuracyThreshold(option.thresholdMeters) }
                        )
                        Text(
                            text = option.name,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Auto pause
            SettingsSection(title = "Авто-пауза") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Включить авто-паузу",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = settings.autoPauseEnabled,
                        onCheckedChange = viewModel::updateAutoPauseEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error Logs
            SettingsSection(title = "Диагностика") {
                OutlinedButton(
                    onClick = onNavigateToErrorLogs,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Просмотр логов ошибок")
                }
                Text(
                    text = "Отправьте лог-файл разработчику для диагностики проблем",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}




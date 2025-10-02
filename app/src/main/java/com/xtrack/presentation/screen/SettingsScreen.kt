package com.xtrack.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xtrack.presentation.viewmodel.SettingsViewModel
import com.xtrack.utils.ErrorLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToErrorLogs: () -> Unit = {},
    onNavigateToNotesList: () -> Unit = {},
    onNavigateToNotesMap: () -> Unit = {},
    onExitApp: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Логируем состояние настроек
    LaunchedEffect(settings) {
        if (settings != null) {
            ErrorLogger.logMessage(
                context,
                "Settings loaded successfully",
                ErrorLogger.LogLevel.INFO
            )
        } else {
            ErrorLogger.logMessage(
                context,
                "Settings are null, showing loading",
                ErrorLogger.LogLevel.WARNING
            )
        }
    }

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
            // Показываем содержимое только если настройки загружены
            settings?.let { currentSettings ->
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
                                selected = currentSettings.locationAccuracy == option.accuracy,
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
                                selected = currentSettings.locationIntervalMs == option.intervalMs,
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
                                selected = currentSettings.minDistanceMeters == option.distanceMeters,
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
                                selected = currentSettings.accuracyThresholdMeters == option.thresholdMeters,
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
                            checked = currentSettings.autoPauseEnabled,
                            onCheckedChange = viewModel::updateAutoPauseEnabled
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Notes section
                SettingsSection(title = "Заметки на карте") {
                    OutlinedButton(
                        onClick = onNavigateToNotesList,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Note,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Список заметок")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = onNavigateToNotesMap,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Список заметок на карте")
                    }
                    
                    Text(
                        text = "Просмотр и управление заметками, созданными на карте",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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

                Spacer(modifier = Modifier.height(24.dp))

                // Exit App
                SettingsSection(title = "Приложение") {
                    OutlinedButton(
                        onClick = onExitApp,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Выход из приложения")
                    }
                    Text(
                        text = "Закрыть приложение и вернуться на главный экран устройства",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } ?: run {
                // Показываем загрузку если настройки еще не загружены
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
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




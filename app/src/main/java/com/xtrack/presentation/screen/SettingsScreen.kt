package com.xtrack.presentation.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xtrack.presentation.viewmodel.SettingsViewModel
import com.xtrack.presentation.viewmodel.ExportNotesViewModel
import com.xtrack.utils.ErrorLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToErrorLogs: () -> Unit = {},
    onNavigateToNotesList: () -> Unit = {},
    onNavigateToNotesMap: () -> Unit = {},
    onExitApp: () -> Unit = {},
    onKillApp: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    exportNotesViewModel: ExportNotesViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Состояние для диалога подтверждения закрытия
    var showKillAppDialog by remember { mutableStateOf(false) }
    
    // Состояние экспорта заметок
    val isExporting by exportNotesViewModel.isExporting.collectAsStateWithLifecycle()
    val exportResult by exportNotesViewModel.exportResult.collectAsStateWithLifecycle()
    val exportError by exportNotesViewModel.error.collectAsStateWithLifecycle()

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

                // Distance notifications section
                SettingsSection(title = "Уведомления о расстоянии") {
                    // Переключатель включения/выключения уведомлений
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Включить уведомления о расстоянии",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Показывать уведомления каждые указанные метры во время записи маршрута",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = settings?.distanceNotificationsEnabled ?: false,
                            onCheckedChange = viewModel::updateDistanceNotificationsEnabled
                        )
                    }
                    
                    // Настройки интервала (показываются только если уведомления включены)
                    if (settings?.distanceNotificationsEnabled == true) {
                        val currentInterval = (settings?.distanceNotificationIntervalMeters ?: 1000).toFloat()
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = "Интервал уведомлений: ${formatDistance(currentInterval.toInt())}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Slider(
                                value = currentInterval,
                                onValueChange = { newValue ->
                                    // Округляем до ближайших 100 метров
                                    val roundedValue = (newValue / 100).roundToInt() * 100
                                    viewModel.updateDistanceNotificationInterval(roundedValue)
                                },
                                valueRange = 100f..5000f,
                                steps = 48, // (5000-100)/100 - 1 = 48 шагов
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "100 м",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "5 км",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = "Перетащите ползунок для выбора интервала уведомлений",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
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

                // Export Notes
                SettingsSection(title = "Экспорт данных") {
                    OutlinedButton(
                        onClick = { exportNotesViewModel.exportAllNotes(context) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Экспорт...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Экспорт всех заметок")
                        }
                    }
                    Text(
                        text = "Экспортировать все заметки в ZIP архив с медиа файлами",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // App Controls
                SettingsSection(title = "Приложение") {
                    // Свернуть приложение (минимизировать)
                    OutlinedButton(
                        onClick = onExitApp,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Свернуть приложение")
                    }
                    Text(
                        text = "Минимизировать приложение и вернуться на главный экран устройства",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Закрыть приложение (полное завершение)
                    OutlinedButton(
                        onClick = { showKillAppDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Закрыть приложение")
                    }
                    Text(
                        text = "Принудительно завершить приложение и освободить память. Если приложение остается в списке недавних, проведите по нему вверх для полного удаления.",
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

    // Обработчик для экспорта файлов
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { selectedUri ->
            // Копируем файл в выбранное место
            exportNotesViewModel.copyFileToSelectedLocation(selectedUri, context)
        }
        exportNotesViewModel.clearResult()
    }

    // Диалог результата экспорта
    exportResult?.let { result ->
        AlertDialog(
            onDismissRequest = { exportNotesViewModel.clearResult() },
            title = { Text(if (result.success) "Экспорт завершен" else "Ошибка экспорта") },
            text = { Text(result.message) },
            confirmButton = {
                if (result.success && result.fileUri != null) {
                    TextButton(
                        onClick = {
                            val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.getDefault()).format(java.util.Date())
                            exportLauncher.launch("xtrack_notes_$currentTime.zip")
                            exportNotesViewModel.clearResult()
                        }
                    ) {
                        Text("Сохранить файл")
                    }
                } else {
                    TextButton(onClick = { exportNotesViewModel.clearResult() }) {
                        Text("ОК")
                    }
                }
            }
        )
    }

    // Диалог ошибки экспорта
    exportError?.let { error ->
        AlertDialog(
            onDismissRequest = { exportNotesViewModel.clearResult() },
            title = { Text("Ошибка экспорта") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { exportNotesViewModel.clearResult() }) {
                    Text("ОК")
                }
            }
        )
    }
    
    // Диалог подтверждения закрытия приложения
    if (showKillAppDialog) {
        AlertDialog(
            onDismissRequest = { showKillAppDialog = false },
            title = { Text("Закрыть приложение") },
            text = { 
                Text("Вы уверены, что хотите принудительно закрыть приложение?\n\nЭто действие:\n• Завершит все процессы приложения\n• Освободит память\n• Попытается удалить приложение из списка недавних\n\nЕсли приложение остается в списке недавних, проведите по нему вверх для полного удаления.\n\nПриложение можно будет запустить только вручную.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showKillAppDialog = false
                        onKillApp()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Закрыть")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillAppDialog = false }) {
                    Text("Отмена")
                }
            }
        )
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

private fun formatDistance(meters: Int): String {
    return if (meters >= 1000) {
        "${String.format("%.1f", meters / 1000.0)} км"
    } else {
        "$meters м"
    }
}




package com.xtrack.presentation.screen

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.xtrack.utils.ErrorLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorLogScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var logContent by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        logContent = ErrorLogger.getLogContent(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Логи ошибок") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Обновить
                    IconButton(onClick = {
                        logContent = ErrorLogger.getLogContent(context)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    
                    // Поделиться
                    IconButton(onClick = {
                        shareLogFile(context)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться")
                    }
                    
                    // Очистить
                    IconButton(onClick = {
                        showClearDialog = true
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Очистить")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Информационная карточка
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ℹ️ Информация",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Этот файл содержит все ошибки приложения. Вы можете отправить его разработчику для анализа.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Содержимое логов
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (logContent.isEmpty() || logContent == "Лог-файл пуст") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "✅ Ошибок пока нет",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = logContent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
    
    // Диалог подтверждения очистки
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистить логи?") },
            text = { Text("Все записи об ошибках будут удалены. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ErrorLogger.clearLogs(context)
                        logContent = ""
                        showClearDialog = false
                    }
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

private fun shareLogFile(context: Context) {
    try {
        val logFile = ErrorLogger.getLogFile(context)
        
        if (!logFile.exists() || logFile.length() == 0L) {
            // Файл пуст, нечего отправлять
            return
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            logFile
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "TrackingApp Error Logs")
            putExtra(Intent.EXTRA_TEXT, "Логи ошибок TrackingApp для анализа")
            // Важно: добавляем флаги разрешений для URI
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // Создаем chooser intent с правильными разрешениями
        val chooserIntent = Intent.createChooser(intent, "Отправить логи").apply {
            // Добавляем флаг для предоставления разрешений через chooser
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        ErrorLogger.logError(context, e, "Failed to share log file")
    }
}


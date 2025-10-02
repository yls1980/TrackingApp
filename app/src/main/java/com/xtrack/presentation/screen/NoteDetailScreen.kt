package com.xtrack.presentation.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xtrack.data.model.MapNote
import com.xtrack.data.model.NoteType
import com.xtrack.presentation.viewmodel.NoteDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Функция для открытия медиафайла
    fun openMediaFile(mediaPath: String, noteType: NoteType) {
        try {
            android.util.Log.d("NoteDetailScreen", "Attempting to open media file: $mediaPath")
            
            val file = File(mediaPath)
            if (!file.exists()) {
                android.util.Log.e("NoteDetailScreen", "Media file does not exist: $mediaPath")
                return
            }
            
            // Создаем URI через FileProvider для безопасного доступа
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            android.util.Log.d("NoteDetailScreen", "Created URI: $uri")
            
            val intent = when (noteType) {
                NoteType.PHOTO, NoteType.MIXED -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                NoteType.VIDEO -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                else -> null
            }
            
            if (intent != null) {
                android.util.Log.d("NoteDetailScreen", "Starting activity with intent for: $noteType")
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("NoteDetailScreen", "Failed to open media file: $mediaPath", e)
        }
    }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали заметки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToMap) {
                        Icon(Icons.Default.Map, contentDescription = "Показать на карте")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить заметку")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Ошибка загрузки заметки: $error",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else if (note != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Основная информация о заметке
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = note!!.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Координаты с возможностью нажатия
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Координаты:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.6f", note!!.latitude)}, ${String.format("%.6f", note!!.longitude)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            OutlinedButton(
                                onClick = onNavigateToMap
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("На карте")
                            }
                        }
                        
                        note!!.description?.let { description ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Описание:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Информация о типе и времени
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getNoteTypeIcon(note!!.noteType),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = getNoteTypeName(note!!.noteType),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Text(
                                text = formatTimestamp(note!!.timestamp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Дополнительная информация
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ID маршрута: ${note!!.trackId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Медиа контент
                note!!.mediaPath?.let { mediaPath ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Медиа файл:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            when (note!!.noteType) {
                                NoteType.PHOTO, NoteType.MIXED -> {
                                    val file = File(mediaPath)
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Фото заметки",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(300.dp)
                                                .clickable { 
                                                    openMediaFile(mediaPath, note!!.noteType)
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Показываем сообщение об ошибке, если файл не найден
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(300.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        Icons.Default.Error,
                                                        contentDescription = "Ошибка",
                                                        modifier = Modifier.size(48.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Файл не найден",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                    Text(
                                                        text = mediaPath,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                NoteType.VIDEO -> {
                                    val file = File(mediaPath)
                                    if (file.exists()) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clickable { 
                                                    openMediaFile(mediaPath, note!!.noteType)
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        Icons.Default.Videocam,
                                                        contentDescription = "Видео",
                                                        modifier = Modifier.size(48.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Видео файл",
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(
                                                        text = "Нажмите для воспроизведения",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Показываем сообщение об ошибке, если видеофайл не найден
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        Icons.Default.Error,
                                                        contentDescription = "Ошибка",
                                                        modifier = Modifier.size(48.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Видео файл не найден",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                    Text(
                                                        text = mediaPath,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Text(
                                        text = "Медиа файл недоступен",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Диалог удаления
    if (showDeleteDialog && note != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить заметку") },
            text = { Text("Вы уверены, что хотите удалить заметку \"${note!!.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(note!!.id)
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

private fun getNoteTypeIcon(noteType: NoteType) = when (noteType) {
    NoteType.TEXT -> Icons.Default.TextFields
    NoteType.PHOTO -> Icons.Default.Photo
    NoteType.VIDEO -> Icons.Default.Videocam
    NoteType.MIXED -> Icons.Default.Photo
}

private fun getNoteTypeName(noteType: NoteType): String = when (noteType) {
    NoteType.TEXT -> "Текст"
    NoteType.PHOTO -> "Фото"
    NoteType.VIDEO -> "Видео"
    NoteType.MIXED -> "Смешанная"
}

private fun formatTimestamp(timestamp: kotlinx.datetime.Instant): String {
    val date = Date(timestamp.toEpochMilliseconds())
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

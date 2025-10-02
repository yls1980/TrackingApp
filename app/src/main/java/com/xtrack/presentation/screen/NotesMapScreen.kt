package com.xtrack.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xtrack.data.model.MapNote
import com.xtrack.data.model.Point
import com.xtrack.presentation.components.MapView
import com.xtrack.presentation.viewmodel.NotesMapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesMapScreen(
    centerOnLastNote: Boolean = false,
    centerOnNoteId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: NotesMapViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val selectedNote by viewModel.selectedNote.collectAsStateWithLifecycle()
    val lastNote by viewModel.lastNote.collectAsStateWithLifecycle()
    val currentNoteIndex by viewModel.currentNoteIndex.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Определяем заметку для центрирования
    val noteToCenter = remember(centerOnLastNote, centerOnNoteId, notes, selectedNote) {
        when {
            centerOnNoteId != null -> notes.find { it.id == centerOnNoteId }
            centerOnLastNote -> lastNote
            selectedNote != null -> selectedNote
            else -> null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setContext(context)
        viewModel.loadNotes()
        android.util.Log.d("NotesMapScreen", "Notes loaded, count: ${notes.size}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заметки на карте (${notes.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (notes.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.toggleShowNotes() }
                        ) {
                            Icon(
                                if (viewModel.showNotes.value) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (viewModel.showNotes.value) "Скрыть заметки" else "Показать заметки"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                        text = "Ошибка загрузки заметок: $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                // Информационная панель о заметках
                if (notes.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "Найдено заметок: ${notes.size}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Карта с заметками
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    trackPoints = emptyList(), // Пустой список треков
                    currentLocation = null,
                    onLocationUpdate = { },
                    centerOnLocation = false,
                    centerOnTrack = false,
                    centerOnNote = noteToCenter,
                    onLongPress = { lat, lon ->
                        // Можно добавить создание новой заметки по долгому нажатию
                    },
                    notes = if (viewModel.showNotes.value) notes else emptyList(),
                    onNoteClick = { note ->
                        viewModel.selectNote(note)
                    }
                )

                // Информационная панель с выбранной заметкой
                selectedNote?.let { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.BottomCenter),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = "Координаты: ${String.format("%.6f", note.latitude)}, ${String.format("%.6f", note.longitude)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    note.description?.let { description ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                
                                Row {
                                    IconButton(
                                        onClick = { viewModel.cycleToNextNote() }
                                    ) {
                                        Icon(
                                            Icons.Default.SkipNext,
                                            contentDescription = "Следующая заметка"
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.clearSelectedNote() }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Закрыть"
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getNoteTypeIcon(note.noteType),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = getNoteTypeName(note.noteType),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Text(
                                    text = formatTimestamp(note.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Счетчик заметок с возможностью циклического переключения
                if (viewModel.showNotes.value && notes.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedNote != null) 
                                MaterialTheme.colorScheme.secondaryContainer 
                            else 
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                        onClick = {
                            viewModel.cycleToNextNote()
                        }
                    ) {
                        Text(
                            text = if (selectedNote != null) {
                                "Заметка ${currentNoteIndex + 1}/${notes.size}"
                            } else {
                                "Заметок: ${notes.size}"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedNote != null) 
                                MaterialTheme.colorScheme.onSecondaryContainer 
                            else 
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

private fun getNoteTypeIcon(noteType: com.xtrack.data.model.NoteType) = when (noteType) {
    com.xtrack.data.model.NoteType.TEXT -> Icons.Default.TextFields
    com.xtrack.data.model.NoteType.PHOTO -> Icons.Default.Photo
    com.xtrack.data.model.NoteType.VIDEO -> Icons.Default.Videocam
    com.xtrack.data.model.NoteType.MIXED -> Icons.Default.Photo
}

private fun getNoteTypeName(noteType: com.xtrack.data.model.NoteType): String = when (noteType) {
    com.xtrack.data.model.NoteType.TEXT -> "Текст"
    com.xtrack.data.model.NoteType.PHOTO -> "Фото"
    com.xtrack.data.model.NoteType.VIDEO -> "Видео"
    com.xtrack.data.model.NoteType.MIXED -> "Смешанная"
}

private fun formatTimestamp(timestamp: kotlinx.datetime.Instant): String {
    val date = java.util.Date(timestamp.toEpochMilliseconds())
    val formatter = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
}

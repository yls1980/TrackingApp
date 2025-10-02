package com.xtrack.presentation.screen

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.xtrack.data.model.MediaType
import com.xtrack.data.model.NoteType
import com.xtrack.presentation.viewmodel.AddNoteViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddNoteScreen(
    latitude: Double,
    longitude: Double,
    trackId: String,
    onNavigateBack: () -> Unit,
    viewModel: AddNoteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf<MediaType?>(null) }
    
    // Разрешения для камеры и аудио
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    // Launchers для камеры и галереи
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && mediaUri != null) {
            mediaType = MediaType.PHOTO
        }
    }
    
    val takeVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && mediaUri != null) {
            mediaType = MediaType.VIDEO
        }
    }
    
    // Функция для запуска камеры с проверкой разрешений
    fun launchCamera() {
        if (cameraPermissionState.status.isGranted) {
            val mediaDir = File(context.getExternalFilesDir(null), "temp")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }
            val photoFile = File(mediaDir, "photo_${System.currentTimeMillis()}.jpg")
            mediaUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(mediaUri)
        } else {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    // Функция для запуска видеозаписи с проверкой разрешений
    fun launchVideoRecorder() {
        if (cameraPermissionState.status.isGranted && audioPermissionState.status.isGranted) {
            val mediaDir = File(context.getExternalFilesDir(null), "temp")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }
            val videoFile = File(mediaDir, "video_${System.currentTimeMillis()}.mp4")
            mediaUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )
            takeVideoLauncher.launch(mediaUri)
        } else {
            // Запрашиваем разрешения, если они не предоставлены
            if (!cameraPermissionState.status.isGranted) {
                cameraPermissionState.launchPermissionRequest()
            } else if (!audioPermissionState.status.isGranted) {
                audioPermissionState.launchPermissionRequest()
            }
        }
    }
    
    // Тип заметки определяется автоматически на основе содержимого
    val selectedNoteType = remember(mediaUri, mediaType) {
        when {
            mediaUri != null && mediaType == MediaType.PHOTO -> NoteType.PHOTO
            mediaUri != null && mediaType == MediaType.VIDEO -> NoteType.VIDEO
            mediaUri != null -> NoteType.MIXED
            else -> NoteType.TEXT
        }
    }
    
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.setLocation(latitude, longitude, trackId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить заметку") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveNote(
                                context = context,
                                title = title,
                                description = description,
                                noteType = selectedNoteType,
                                mediaUri = mediaUri,
                                mediaType = mediaType
                            )
                        },
                        enabled = title.isNotBlank() && !isSaving
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Информация о местоположении
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Местоположение",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Широта: ${String.format("%.6f", latitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Долгота: ${String.format("%.6f", longitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }


            // Заголовок
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Заголовок заметки") },
                placeholder = { Text("Введите заголовок...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Описание
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                placeholder = { Text("Введите описание заметки...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Секция медиа
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Добавить медиа",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Информация о разрешениях
                    if (!cameraPermissionState.status.isGranted || !audioPermissionState.status.isGranted) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Требуются разрешения:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!cameraPermissionState.status.isGranted) {
                                    Text(
                                        text = "• Камера - для фото и видео",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (!audioPermissionState.status.isGranted) {
                                    Text(
                                        text = "• Микрофон - для видеозаписи",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (!cameraPermissionState.status.isGranted) {
                                            cameraPermissionState.launchPermissionRequest()
                                        } else if (!audioPermissionState.status.isGranted) {
                                            audioPermissionState.launchPermissionRequest()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Предоставить разрешения")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Кнопка фото
                        Button(
                            onClick = { launchCamera() },
                            modifier = Modifier.weight(1f),
                            enabled = cameraPermissionState.status.isGranted
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Сделать фото",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Фото", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Кнопка видео
                        Button(
                            onClick = { launchVideoRecorder() },
                            modifier = Modifier.weight(1f),
                            enabled = cameraPermissionState.status.isGranted && audioPermissionState.status.isGranted
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = "Снять видео",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Видео", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    
                    // Превью медиа
                    mediaUri?.let { uri ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Медиа файл:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (mediaType == MediaType.PHOTO) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Превью фото",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Videocam,
                                            contentDescription = "Видео",
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Видео файл готов",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Кнопка удаления медиа
                                OutlinedButton(
                                    onClick = {
                                        mediaUri = null
                                        mediaType = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Удалить медиа")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка сохранения
            Button(
                onClick = {
                    viewModel.saveNote(
                        context = context,
                        title = title,
                        description = description,
                        noteType = selectedNoteType,
                        mediaUri = mediaUri,
                        mediaType = mediaType
                    )
                },
                enabled = title.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isSaving) "Сохранение..." else "Сохранить заметку")
            }

            // Ошибка
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // Обработка успешного сохранения
    val isNoteSaved by viewModel.isNoteSaved.collectAsStateWithLifecycle()
    LaunchedEffect(isNoteSaved) {
        if (isNoteSaved) {
            onNavigateBack()
        }
    }
}


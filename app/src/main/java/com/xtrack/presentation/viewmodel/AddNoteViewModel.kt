package com.xtrack.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xtrack.data.model.MapNote
import com.xtrack.data.model.MediaType
import com.xtrack.data.model.NoteType
import com.xtrack.data.repository.MapNoteRepository
import com.xtrack.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AddNoteViewModel @Inject constructor(
    private val mapNoteRepository: MapNoteRepository
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isNoteSaved = MutableStateFlow(false)
    val isNoteSaved: StateFlow<Boolean> = _isNoteSaved.asStateFlow()

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var currentTrackId: String = ""

    fun setLocation(latitude: Double, longitude: Double, trackId: String = "") {
        currentLatitude = latitude
        currentLongitude = longitude
        currentTrackId = trackId
    }

    fun saveNote(
        context: Context,
        title: String,
        description: String,
        noteType: NoteType,
        mediaUri: Uri? = null,
        mediaType: MediaType? = null
    ) {
        if (title.isBlank()) {
            _error.value = "Заголовок не может быть пустым"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null

            try {
                // Копируем медиа файл в постоянное хранилище
                var finalMediaPath: String? = null
                mediaUri?.let { uri ->
                    try {
                        val mediaDir = File(context.getExternalFilesDir(null), "media")
                        if (!mediaDir.exists()) {
                            mediaDir.mkdirs()
                        }
                        
                        val mediaFile = File(mediaDir, "${UUID.randomUUID()}.${getFileExtension(uri, mediaType)}")
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            mediaFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        finalMediaPath = mediaFile.absolutePath
                        
                        ErrorLogger.logMessage(
                            context,
                            "Media file saved: ${mediaFile.absolutePath}",
                            ErrorLogger.LogLevel.INFO
                        )
                    } catch (e: Exception) {
                        ErrorLogger.logError(
                            context,
                            e,
                            "Failed to save media file"
                        )
                        _error.value = "Ошибка сохранения медиа файла: ${e.message}"
                        return@launch
                    }
                }

                val note = MapNote(
                    id = UUID.randomUUID().toString(),
                    trackId = currentTrackId.ifEmpty { "standalone_note" }, // Используем специальный ID для заметок без трека
                    latitude = currentLatitude,
                    longitude = currentLongitude,
                    title = title,
                    description = description.takeIf { it.isNotBlank() },
                    timestamp = Clock.System.now(),
                    noteType = noteType,
                    mediaPath = finalMediaPath,
                    mediaType = mediaType
                )

                mapNoteRepository.insertNote(note)
                
                ErrorLogger.logMessage(
                    context,
                    "Note saved: $title at ($currentLatitude, $currentLongitude)",
                    ErrorLogger.LogLevel.INFO
                )

                _isNoteSaved.value = true
            } catch (e: Exception) {
                ErrorLogger.logError(
                    context,
                    e,
                    "Failed to save note: $title"
                )
                _error.value = "Ошибка сохранения заметки: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    private fun getFileExtension(uri: Uri, mediaType: MediaType?): String {
        return when (mediaType) {
            MediaType.PHOTO -> "jpg"
            MediaType.VIDEO -> "mp4"
            null -> {
                // Пытаемся определить по URI
                val uriString = uri.toString()
                when {
                    uriString.contains(".jpg") || uriString.contains(".jpeg") -> "jpg"
                    uriString.contains(".png") -> "png"
                    uriString.contains(".mp4") -> "mp4"
                    uriString.contains(".avi") -> "avi"
                    uriString.contains(".mov") -> "mov"
                    else -> "jpg" // По умолчанию jpg для фото
                }
            }
        }
    }
}

package com.xtrack.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xtrack.data.model.MapNote
import com.xtrack.data.model.Track
import com.xtrack.data.repository.MapNoteRepository
import com.xtrack.data.repository.TrackRepository
import com.xtrack.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class ExportNotesViewModel @Inject constructor(
    application: Application,
    private val noteRepository: MapNoteRepository,
    private val trackRepository: TrackRepository
) : AndroidViewModel(application) {

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    data class ExportResult(val success: Boolean, val message: String, val fileUri: Uri? = null)
    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _lastArchiveName = MutableStateFlow<String?>(null)
    val lastArchiveName: StateFlow<String?> = _lastArchiveName.asStateFlow()

    fun exportNotes(trackId: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _isExporting.value = true
                _error.value = null
                _exportResult.value = null
            }

            try {
                ErrorLogger.logMessage(
                    getApplication(),
                    "Starting notes export for track: $trackId",
                    ErrorLogger.LogLevel.INFO
                )

                // Получаем трек и заметки
                val track = trackRepository.getTrackById(trackId)
                val notes = noteRepository.getNotesByTrackId(trackId).first()

                if (track == null) {
                    withContext(Dispatchers.Main) {
                        _error.value = "Трек не найден"
                    }
                    return@launch
                }

                if (notes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _error.value = "В этом треке нет заметок для экспорта"
                    }
                    return@launch
                }

                // Создаем временную папку для архива
                val tempDir = File(context.cacheDir, "notes_export")
                if (!tempDir.exists()) {
                    tempDir.mkdirs()
                }

                // Создаем ZIP архив
                val archiveName = "notes_${track.name.replace("[^a-zA-Z0-9а-яА-Я]".toRegex(), "_")}_${getCurrentDateString()}.zip"
                val archiveFile = File(tempDir, archiveName)

                createNotesArchive(notes, track, archiveFile, context)

                // Создаем URI для файла
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    archiveFile
                )

                withContext(Dispatchers.Main) {
                    _exportResult.value = ExportResult(
                        success = true,
                        message = "Заметки успешно экспортированы: ${notes.size} заметок",
                        fileUri = fileUri
                    )
                }

                ErrorLogger.logMessage(
                    getApplication(),
                    "Notes export completed successfully: ${notes.size} notes exported",
                    ErrorLogger.LogLevel.INFO
                )

            } catch (e: Exception) {
                val errorMessage = "Ошибка экспорта заметок: ${e.message}"
                withContext(Dispatchers.Main) {
                    _error.value = errorMessage
                }
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to export notes for track: $trackId"
                )
            } finally {
                withContext(Dispatchers.Main) {
                    _isExporting.value = false
                }
            }
        }
    }

    private suspend fun createNotesArchive(
        notes: List<MapNote>,
        track: Track,
        archiveFile: File,
        context: Context
    ) = withContext(Dispatchers.IO) {
        FileOutputStream(archiveFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // Добавляем общую информацию о треке
                addTrackInfoToArchive(zos, track, notes)

                // Обрабатываем каждую заметку
                notes.forEachIndexed { index, note ->
                    try {
                        addNoteToArchive(zos, note, index + 1, context)
                    } catch (e: Exception) {
                        ErrorLogger.logError(
                            getApplication(),
                            e,
                            "Failed to add note ${note.id} to archive"
                        )
                        // Продолжаем обработку других заметок
                    }
                }
            }
        }
    }

    private fun addTrackInfoToArchive(zos: ZipOutputStream, track: Track, notes: List<MapNote>) {
        val trackInfoContent = buildString {
            appendLine("=== ИНФОРМАЦИЯ О ТРЕКЕ ===")
            appendLine("Название: ${track.name}")
            appendLine("Дата начала: ${formatDateTime(track.startedAt)}")
            track.endedAt?.let { appendLine("Дата окончания: ${formatDateTime(it)}") }
            appendLine("Дистанция: ${String.format("%.2f", track.distanceMeters / 1000)} км")
            appendLine("Длительность: ${track.durationSec / 60} мин")
            appendLine("Количество заметок: ${notes.size}")
            appendLine()
        }

        val trackInfoEntry = ZipEntry("00_track_info.txt")
        zos.putNextEntry(trackInfoEntry)
        zos.write(trackInfoContent.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun addNoteToArchive(
        zos: ZipOutputStream,
        note: MapNote,
        noteNumber: Int,
        context: Context
    ) {
        val noteFolderName = "${noteNumber.toString().padStart(2, '0')}_${sanitizeFileName(note.title)}"
        
        // Создаем текстовый файл с информацией о заметке
        val noteInfoContent = buildString {
            appendLine("=== ЗАМЕТКА #$noteNumber ===")
            appendLine("Название: ${note.title}")
            appendLine("Описание: ${note.description ?: "Нет описания"}")
            appendLine("Дата создания: ${formatDateTime(note.timestamp)}")
            appendLine("Тип заметки: ${getNoteTypeString(note.noteType)}")
            appendLine("Координаты: ${String.format("%.6f", note.latitude)}, ${String.format("%.6f", note.longitude)}")
            appendLine()
            appendLine("=== СОДЕРЖИМОЕ ЗАМЕТКИ ===")
            appendLine(note.description ?: "Нет текстового содержимого")
        }

        val noteInfoEntry = ZipEntry("$noteFolderName/note_info.txt")
        zos.putNextEntry(noteInfoEntry)
        zos.write(noteInfoContent.toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // Копируем медиа файлы
        copyMediaFiles(zos, note, noteFolderName, context)
    }

    private fun copyMediaFiles(
        zos: ZipOutputStream,
        note: MapNote,
        noteFolderName: String,
        context: Context
    ) {
        // Копируем медиа файл (фото или видео)
        note.mediaPath?.let { mediaPath ->
            try {
                val mediaFile = File(mediaPath)
                if (mediaFile.exists()) {
                    val mediaExtension = when (note.mediaType) {
                        com.xtrack.data.model.MediaType.PHOTO -> "photo"
                        com.xtrack.data.model.MediaType.VIDEO -> "video"
                        null -> "media"
                    }
                    val mediaEntry = ZipEntry("$noteFolderName/${mediaExtension}_${mediaFile.name}")
                    zos.putNextEntry(mediaEntry)
                    
                    FileInputStream(mediaFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            } catch (e: Exception) {
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to copy media for note ${note.id}"
                )
            }
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace("[^a-zA-Z0-9а-яА-Я\\s]".toRegex(), "_")
            .replace("\\s+".toRegex(), "_")
            .take(50) // Ограничиваем длину имени
    }

    private fun getNoteTypeString(noteType: com.xtrack.data.model.NoteType): String {
        return when (noteType) {
            com.xtrack.data.model.NoteType.TEXT -> "Текстовая заметка"
            com.xtrack.data.model.NoteType.PHOTO -> "Фотография"
            com.xtrack.data.model.NoteType.VIDEO -> "Видео"
            com.xtrack.data.model.NoteType.MIXED -> "Смешанная (текст + медиа)"
        }
    }

    private fun formatDateTime(instant: kotlinx.datetime.Instant): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(instant.epochSeconds * 1000))
    }

    private fun getCurrentDateString(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return formatter.format(Date())
    }
    
    private fun getCurrentDateStringForFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        return formatter.format(Date())
    }

    fun exportAllNotes(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _isExporting.value = true
                _error.value = null
                _exportResult.value = null
            }

            try {
                ErrorLogger.logMessage(
                    getApplication(),
                    "Starting export of all notes",
                    ErrorLogger.LogLevel.INFO
                )

                // Получаем все заметки напрямую
                val allNotes = noteRepository.getAllMapNotes().first()
                
                ErrorLogger.logMessage(
                    getApplication(),
                    "Found ${allNotes.size} notes for export",
                    ErrorLogger.LogLevel.INFO
                )

                // Проверяем наличие заметок
                if (allNotes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _exportResult.value = ExportResult(
                            success = false,
                            message = "Нет заметок для экспорта"
                        )
                        _isExporting.value = false
                    }
                    return@launch
                }

                // Получаем все треки для группировки заметок
                val allTracks = trackRepository.getAllTracks().first()


                // Создаем временную папку для архива в корне cache
                val tempDir = File(context.cacheDir, "temp")
                if (!tempDir.exists()) {
                    tempDir.mkdirs()
                }

                // Создаем ZIP архив
                val archiveName = "xtrack_notes_${getCurrentDateStringForFileName()}.zip"
                val archiveFile = File(tempDir, archiveName)

                createAllNotesArchive(allNotes, allTracks, archiveFile, context)

                // Создаем URI для файла
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    archiveFile
                )

                // Сохраняем имя файла для последующего копирования
                _lastArchiveName.value = archiveName

                withContext(Dispatchers.Main) {
                    _exportResult.value = ExportResult(
                        success = true,
                        message = "Все заметки успешно экспортированы: ${allNotes.size} заметок из ${allTracks.size} маршрутов",
                        fileUri = fileUri
                    )
                }

                ErrorLogger.logMessage(
                    getApplication(),
                    "All notes export completed successfully: ${allNotes.size} notes from ${allTracks.size} tracks",
                    ErrorLogger.LogLevel.INFO
                )

            } catch (e: Exception) {
                val errorMessage = "Ошибка экспорта всех заметок: ${e.message}"
                withContext(Dispatchers.Main) {
                    _error.value = errorMessage
                }
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to export all notes"
                )
            } finally {
                withContext(Dispatchers.Main) {
                    _isExporting.value = false
                }
            }
        }
    }

    private suspend fun createAllNotesArchive(
        allNotes: List<MapNote>,
        allTracks: List<Track>,
        archiveFile: File,
        context: Context
    ) = withContext(Dispatchers.IO) {
        android.util.Log.i("ExportNotesViewModel", "Starting to create archive with ${allNotes.size} notes and ${allTracks.size} tracks")
        
        FileOutputStream(archiveFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // Добавляем общую информацию
                addOverallInfoToArchive(zos, allNotes, allTracks)
                android.util.Log.i("ExportNotesViewModel", "Added overall info to archive")

                // Группируем заметки по трекам
                val notesByTrack = allNotes.groupBy { it.trackId }
                var noteCounter = 1
                
                notesByTrack.forEach { (trackId, trackNotes) ->
                    val track = allTracks.find { it.id == trackId }
                    if (track != null) {
                        // Добавляем информацию о треке
                        addTrackInfoToArchive(zos, track, trackNotes)
                        
                        // Обрабатываем заметки этого трека
                        trackNotes.forEach { note ->
                            try {
                                addNoteToArchive(zos, note, noteCounter, context)
                                noteCounter++
                            } catch (e: Exception) {
                                ErrorLogger.logError(
                                    getApplication(),
                                    e,
                                    "Failed to add note ${note.id} to archive"
                                )
                            }
                        }
                    } else {
                        // Обрабатываем заметки без трека
                        trackNotes.forEach { note ->
                            try {
                                addNoteToArchive(zos, note, noteCounter, context)
                                noteCounter++
                            } catch (e: Exception) {
                                ErrorLogger.logError(
                                    getApplication(),
                                    e,
                                    "Failed to add note ${note.id} to archive"
                                )
                            }
                        }
                    }
                }
                
                android.util.Log.i("ExportNotesViewModel", "Archive creation completed. Archive size: ${archiveFile.length()} bytes")
            }
        }
    }

    private fun addOverallInfoToArchive(
        zos: ZipOutputStream,
        allNotes: List<MapNote>,
        allTracks: List<Track>
    ) {
        val overallInfoContent = buildString {
            appendLine("=== ЭКСПОРТ ВСЕХ ЗАМЕТОК ===")
            appendLine("Дата экспорта: ${getCurrentDateString()}")
            appendLine("Общее количество заметок: ${allNotes.size}")
            appendLine("Количество маршрутов: ${allTracks.size}")
            appendLine()
            appendLine("=== СТАТИСТИКА ПО ТИПАМ ЗАМЕТОК ===")
            val notesByType = allNotes.groupBy { it.noteType }
            notesByType.forEach { (type, notes) ->
                appendLine("${getNoteTypeString(type)}: ${notes.size}")
            }
            appendLine()
            appendLine("=== СПИСОК МАРШРУТОВ ===")
            allTracks.forEachIndexed { index, track ->
                val trackNotesCount = allNotes.count { it.trackId == track.id }
                appendLine("${index + 1}. ${track.name} - ${trackNotesCount} заметок")
            }
            appendLine()
        }

        val overallInfoEntry = ZipEntry("00_overall_info.txt")
        zos.putNextEntry(overallInfoEntry)
        zos.write(overallInfoContent.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    fun clearResult() {
        _exportResult.value = null
        _error.value = null
    }

    fun copyFileToSelectedLocation(selectedUri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempDir = File(context.cacheDir, "temp")
                val archiveName = _lastArchiveName.value ?: "xtrack_notes_${getCurrentDateStringForFileName()}.zip"
                val sourceFile = File(tempDir, archiveName)
                
                if (sourceFile.exists()) {
                    context.contentResolver.openOutputStream(selectedUri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        _exportResult.value = ExportResult(
                            success = true,
                            message = "Файл успешно сохранен в выбранное место"
                        )
                    }
                    
                    android.util.Log.i("ExportNotesViewModel", "File copied successfully to: $selectedUri")
                } else {
                    withContext(Dispatchers.Main) {
                        _error.value = "Исходный файл не найден"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.value = "Ошибка при сохранении файла: ${e.message}"
                }
                android.util.Log.e("ExportNotesViewModel", "Failed to copy file", e)
            }
        }
    }
}

package com.xtrack.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xtrack.data.model.Track
import com.xtrack.data.model.TrackPoint
import com.xtrack.data.parser.TrackImportParser
import com.xtrack.data.repository.TrackRepository
import com.xtrack.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ImportTrackViewModel @Inject constructor(
    application: Application,
    private val trackRepository: TrackRepository,
    private val trackImportParser: TrackImportParser
) : AndroidViewModel(application) {

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun importTrack(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Обновляем UI состояние в главном потоке
            withContext(Dispatchers.Main) {
                _isImporting.value = true
                _error.value = null
                _importResult.value = null
            }

            try {
                ErrorLogger.logMessage(
                    getApplication(),
                    "Starting track import from file: $fileName",
                    ErrorLogger.LogLevel.INFO
                )

                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Не удалось открыть файл")

                // Определяем тип файла по расширению
                val fileExtension = fileName.lowercase()
                val result = when {
                    fileExtension.endsWith(".gpx") || fileExtension.endsWith(".gpx.gz") -> {
                        trackImportParser.importFromGpx(inputStream, fileName)
                    }
                    fileExtension.endsWith(".geojson") || fileExtension.endsWith(".json") -> {
                        trackImportParser.importFromGeoJson(inputStream, fileName)
                    }
                    else -> {
                        // Пытаемся определить тип по содержимому файла
                        inputStream.mark(1024)
                        val firstBytes = ByteArray(1024)
                        val bytesRead = inputStream.read(firstBytes)
                        inputStream.reset()
                        
                        val content = String(firstBytes, 0, bytesRead.coerceAtLeast(0))
                        when {
                            content.contains("<gpx") || content.contains("<?xml") -> {
                                trackImportParser.importFromGpx(inputStream, fileName)
                            }
                            content.contains("\"type\"") && content.contains("\"coordinates\"") -> {
                                trackImportParser.importFromGeoJson(inputStream, fileName)
                            }
                            else -> {
                                TrackImportParser.ImportResult.Error("Неподдерживаемый формат файла. Поддерживаются только GPX и GeoJSON файлы.")
                            }
                        }
                    }
                }

                when (result) {
                    is TrackImportParser.ImportResult.Success -> {
                        // Сохраняем трек и точки в базу данных
                        trackRepository.insertTrack(result.track)
                        result.trackPoints.forEach { trackPoint ->
                            trackRepository.insertTrackPoint(trackPoint)
                        }

                        // Обновляем UI в главном потоке
                        withContext(Dispatchers.Main) {
                            _importResult.value = ImportResult.Success(
                                track = result.track,
                                pointsCount = result.trackPoints.size
                            )
                        }

                        ErrorLogger.logMessage(
                            getApplication(),
                            "Track imported successfully: ${result.track.name}, ${result.trackPoints.size} points",
                            ErrorLogger.LogLevel.INFO
                        )
                    }
                    is TrackImportParser.ImportResult.Error -> {
                        // Обновляем UI в главном потоке
                        withContext(Dispatchers.Main) {
                            _error.value = result.message
                        }
                        ErrorLogger.logMessage(
                            getApplication(),
                            "Track import failed: ${result.message}",
                            ErrorLogger.LogLevel.ERROR
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMessage = "Ошибка импорта файла: ${e.message}"
                withContext(Dispatchers.Main) {
                    _error.value = errorMessage
                }
                ErrorLogger.logError(
                    getApplication(),
                    e,
                    "Failed to import track from file: $fileName"
                )
            } finally {
                withContext(Dispatchers.Main) {
                    _isImporting.value = false
                }
            }
        }
    }

    fun clearResult() {
        _importResult.value = null
        _error.value = null
    }

    sealed class ImportResult {
        data class Success(val track: Track, val pointsCount: Int) : ImportResult()
    }
}

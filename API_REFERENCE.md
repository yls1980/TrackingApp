# 📖 API Reference - XTrack

Краткий справочник по основным API и компонентам приложения.

---

## 🗄️ Data Models

### Track (Трек)

```kotlin
@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey
    val id: String,
    val name: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val distanceMeters: Double,
    val durationSec: Long,
    val elevationGainMeters: Double = 0.0,
    val gpxPath: String?,
    val geojsonPath: String?,
    val isRecording: Boolean = false
)
```

### TrackPoint (Точка трека)

```kotlin
@Entity(tableName = "track_points")
data class TrackPoint(
    @PrimaryKey
    val id: String,
    val trackId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val speed: Float?,
    val bearing: Float?,
    val accuracy: Float
)
```

### MapNote (Заметка на карте)

```kotlin
@Entity(tableName = "map_notes")
data class MapNote(
    @PrimaryKey
    val id: String,
    val trackId: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String? = null,
    val timestamp: Instant,
    val noteType: NoteType,
    val mediaPath: String? = null,
    val mediaType: MediaType? = null
)

enum class NoteType {
    TEXT,      // Текстовая заметка
    PHOTO,     // Заметка с фото
    VIDEO,     // Заметка с видео
    MIXED      // Заметка с текстом и медиа
}

enum class MediaType {
    PHOTO,
    VIDEO
}
```

### AppSettings (Настройки приложения)

```kotlin
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val locationAccuracy: LocationAccuracy = LocationAccuracy.BALANCED,
    val locationIntervalMs: Long = 5000L,
    val minDistanceMeters: Float = 10f,
    val accuracyThresholdMeters: Float = 50f,
    val autoPauseEnabled: Boolean = false,
    val autoPauseSpeedThreshold: Float = 1.0f,
    val autoPauseDurationSec: Long = 30L,
    val defaultExportFormat: ExportFormat = ExportFormat.GPX,
    val appExitState: AppExitState = AppExitState.STOPPED,
    val distanceNotificationIntervalMeters: Int = 1000,
    val distanceNotificationsEnabled: Boolean = false
)

enum class LocationAccuracy {
    HIGH_ACCURACY,  // Максимальная точность, высокий расход батареи
    BALANCED,       // Баланс точности и энергопотребления
    LOW_POWER       // Низкое энергопотребление, низкая точность
}

enum class ExportFormat {
    GPX,            // GPS Exchange Format
    GEOJSON         // GeoJSON format
}

enum class AppExitState {
    STOPPED,        // Приложение остановлено нормально
    RECORDING       // Приложение закрыто во время записи
}
```

---

## 📊 Repositories

### TrackRepository

```kotlin
class TrackRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val trackPointDao: TrackPointDao
)

// Методы:

// Получить все треки
suspend fun getAllTracks(): Flow<List<Track>>

// Получить трек по ID
suspend fun getTrackById(id: String): Flow<Track?>

// Получить активный трек (с isRecording = true)
suspend fun getActiveTrack(): Flow<Track?>

// Создать новый трек
suspend fun insertTrack(track: Track)

// Обновить трек
suspend fun updateTrack(track: Track)

// Удалить трек
suspend fun deleteTrack(trackId: String)

// Получить точки трека
suspend fun getTrackPoints(trackId: String): Flow<List<TrackPoint>>

// Добавить точку к треку
suspend fun insertTrackPoint(point: TrackPoint)

// Удалить все точки трека
suspend fun deleteTrackPoints(trackId: String)

// Получить статистику трека
suspend fun getTrackStatistics(trackId: String): TrackStatistics
```

### SettingsRepository

```kotlin
class SettingsRepository @Inject constructor(
    private val appSettingsDao: AppSettingsDao
)

// Методы:

// Получить настройки
suspend fun getSettings(): Flow<AppSettings>

// Обновить настройки
suspend fun updateSettings(settings: AppSettings)

// Получить точность геолокации
suspend fun getLocationAccuracy(): Flow<LocationAccuracy>

// Обновить точность геолокации
suspend fun updateLocationAccuracy(accuracy: LocationAccuracy)

// Получить интервал записи
suspend fun getLocationInterval(): Flow<Long>

// Обновить интервал записи
suspend fun updateLocationInterval(intervalMs: Long)

// Сбросить к настройкам по умолчанию
suspend fun resetToDefaults()
```

### MapNoteRepository

```kotlin
class MapNoteRepository @Inject constructor(
    private val mapNoteDao: MapNoteDao
)

// Методы:

// Получить все заметки
suspend fun getAllNotes(): Flow<List<MapNote>>

// Получить заметку по ID
suspend fun getNoteById(id: String): Flow<MapNote?>

// Получить заметки для трека
suspend fun getNotesForTrack(trackId: String): Flow<List<MapNote>>

// Создать новую заметку
suspend fun insertNote(note: MapNote)

// Обновить заметку
suspend fun updateNote(note: MapNote)

// Удалить заметку
suspend fun deleteNote(noteId: String)

// Удалить все заметки трека
suspend fun deleteNotesForTrack(trackId: String)
```

---

## 🎨 ViewModels

### MainViewModel

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val settingsRepository: SettingsRepository,
    private val application: Application
) : ViewModel()

// Состояние:
val currentTrack: StateFlow<Track?>
val isRecording: StateFlow<Boolean>
val isPaused: StateFlow<Boolean>
val currentMetrics: StateFlow<TrackMetrics>
val currentLocation: StateFlow<Point?>

// События:
val events: SharedFlow<MainEvent>

// Методы:

// Начать запись
fun startRecording()

// Остановить запись
fun stopRecording()

// Пауза записи
fun pauseRecording()

// Возобновить запись
fun resumeRecording()

// Добавить заметку
fun addNote(latitude: Double, longitude: Double)

// Обновить центр карты
fun updateMapCenter(point: Point)
```

### TracksListViewModel

```kotlin
@HiltViewModel
class TracksListViewModel @Inject constructor(
    private val trackRepository: TrackRepository
) : ViewModel()

// Состояние:
val tracks: StateFlow<List<Track>>
val searchQuery: StateFlow<String>
val sortOrder: StateFlow<SortOrder>
val isLoading: StateFlow<Boolean>

// Методы:

// Поиск треков
fun search(query: String)

// Сортировка
fun setSortOrder(order: SortOrder)

// Удалить трек
fun deleteTrack(trackId: String)

// Экспортировать трек
suspend fun exportTrack(trackId: String, format: ExportFormat): Uri

// Импортировать трек
suspend fun importTrack(uri: Uri): Track

enum class SortOrder {
    DATE_DESC,      // По дате (новые первые)
    DATE_ASC,       // По дате (старые первые)
    DISTANCE_DESC,  // По расстоянию (больше первые)
    DISTANCE_ASC,   // По расстоянию (меньше первые)
    NAME_ASC,       // По имени (А-Я)
    NAME_DESC       // По имени (Я-А)
}
```

### TrackDetailViewModel

```kotlin
@HiltViewModel
class TrackDetailViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel()

// Состояние:
val track: StateFlow<Track?>
val trackPoints: StateFlow<List<TrackPoint>>
val statistics: StateFlow<TrackStatistics>
val elevationProfile: StateFlow<List<ElevationPoint>>
val speedProfile: StateFlow<List<SpeedPoint>>

// Методы:

// Экспортировать трек
suspend fun exportTrack(format: ExportFormat): Uri

// Удалить трек
fun deleteTrack()

// Переименовать трек
fun renameTrack(newName: String)

// Получить центр трека для карты
fun getTrackCenter(): Point
```

### SettingsViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val application: Application
) : ViewModel()

// Состояние:
val settings: StateFlow<AppSettings>

// Методы:

// Обновить настройки
fun updateSettings(settings: AppSettings)

// Обновить точность GPS
fun updateLocationAccuracy(accuracy: LocationAccuracy)

// Обновить интервал записи
fun updateLocationInterval(intervalMs: Long)

// Обновить минимальное расстояние
fun updateMinDistance(distanceMeters: Float)

// Сбросить к настройкам по умолчанию
fun resetToDefaults()

// Экспортировать логи ошибок
suspend fun exportLogs(): Uri

// Очистить логи
fun clearLogs()

// Очистить кэш
fun clearCache()

// Получить размер кэша
suspend fun getCacheSize(): Long
```

---

## 🧩 Compose Components

### MapView

```kotlin
@Composable
fun MapView(
    modifier: Modifier = Modifier,
    currentLocation: Point? = null,
    trackPoints: List<TrackPoint> = emptyList(),
    mapNotes: List<MapNote> = emptyList(),
    onMapClick: (Point) -> Unit = {},
    onMapLongClick: (Point) -> Unit = {},
    followUser: Boolean = true,
    centerOnPoint: Point? = null,
    showUserLocation: Boolean = true,
    polylineColor: Int = Color.BLUE,
    polylineWidth: Float = 5.0f
)
```

**Параметры:**
- `currentLocation` - текущая позиция пользователя
- `trackPoints` - точки трека для отрисовки маршрута
- `mapNotes` - заметки для отображения на карте
- `onMapClick` - обработчик клика по карте
- `onMapLongClick` - обработчик долгого нажатия на карту
- `followUser` - следовать за пользователем
- `centerOnPoint` - центрировать на определенной точке
- `showUserLocation` - показывать текущую позицию пользователя

### StatisticsCard

```kotlin
@Composable
fun StatisticsCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    unit: String? = null,
    onClick: (() -> Unit)? = null
)
```

**Параметры:**
- `title` - заголовок карточки (например, "Расстояние")
- `value` - значение (например, "5.2")
- `icon` - иконка
- `unit` - единица измерения (например, "км")
- `onClick` - обработчик клика (опционально)

### TrackItem

```kotlin
@Composable
fun TrackItem(
    track: Track,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onExport: (ExportFormat) -> Unit = {},
    modifier: Modifier = Modifier,
    showActions: Boolean = true
)
```

**Параметры:**
- `track` - данные трека
- `onClick` - обработчик клика на элемент
- `onDelete` - обработчик удаления
- `onExport` - обработчик экспорта
- `showActions` - показывать действия (удалить, экспорт)

---

## 🛠️ Utilities

### LocationUtils

```kotlin
object LocationUtils {
    
    // Рассчитать расстояние между двумя точками (метры)
    fun calculateDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Float
    
    // Рассчитать азимут между двумя точками (градусы)
    fun calculateBearing(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Float
    
    // Рассчитать скорость (м/с)
    fun calculateSpeed(
        distanceMeters: Float,
        timeDeltaMs: Long
    ): Float
    
    // Форматировать расстояние (м или км)
    fun formatDistance(meters: Float): String
    // Примеры: "50 м", "1.2 км", "15.7 км"
    
    // Форматировать скорость (км/ч)
    fun formatSpeed(metersPerSecond: Float): String
    // Примеры: "5.4 км/ч", "15.2 км/ч"
    
    // Форматировать длительность (чч:мм:сс)
    fun formatDuration(seconds: Long): String
    // Примеры: "00:05:30", "01:15:42"
    
    // Рассчитать набор высоты
    fun calculateElevationGain(points: List<TrackPoint>): Double
    
    // Получить центр треков
    fun getTrackCenter(points: List<TrackPoint>): Point
    
    // Получить границы трека
    fun getTrackBounds(points: List<TrackPoint>): BoundingBox
}
```

### ErrorLogger

```kotlin
object ErrorLogger {
    
    enum class LogLevel {
        ERROR,
        WARNING,
        INFO
    }
    
    // Логировать ошибку
    fun logError(
        context: Context,
        exception: Throwable,
        message: String? = null
    )
    
    // Логировать сообщение
    fun logMessage(
        context: Context,
        message: String,
        level: LogLevel = LogLevel.INFO
    )
    
    // Получить файл с логами
    fun getLogFile(context: Context): File
    
    // Очистить логи
    fun clearLogs(context: Context)
    
    // Получить размер файла логов
    fun getLogFileSize(context: Context): Long
    
    // Экспортировать логи
    suspend fun exportLogs(context: Context): Uri
}
```

### NetworkUtils

```kotlin
object NetworkUtils {
    
    // Проверить доступность сети
    fun isNetworkAvailable(context: Context): Boolean
    
    // Получить тип сети
    fun getNetworkType(context: Context): NetworkType
    
    enum class NetworkType {
        WIFI,
        MOBILE,
        NONE
    }
    
    // Начать мониторинг сети
    fun startNetworkMonitoring(
        context: Context,
        onNetworkAvailable: () -> Unit,
        onNetworkLost: () -> Unit
    )
    
    // Остановить мониторинг сети
    fun stopNetworkMonitoring(context: Context)
}
```

### ServiceUtils

```kotlin
object ServiceUtils {
    
    // Проверить, запущен ли сервис
    fun isServiceRunning(
        context: Context,
        serviceClass: Class<*>
    ): Boolean
    
    // Запустить foreground сервис
    fun startForegroundService(
        context: Context,
        intent: Intent
    )
    
    // Остановить сервис
    fun stopService(
        context: Context,
        serviceClass: Class<*>
    )
}
```

---

## 📄 Parsers

### GpxGenerator

```kotlin
object GpxGenerator {
    
    // Сгенерировать GPX из трека
    fun generate(
        track: Track,
        points: List<TrackPoint>,
        includeMetadata: Boolean = true
    ): String
    
    // Сгенерировать GPX с заметками как waypoints
    fun generateWithNotes(
        track: Track,
        points: List<TrackPoint>,
        notes: List<MapNote>
    ): String
}
```

**Пример использования:**

```kotlin
val track = trackRepository.getTrackById(trackId).first()
val points = trackRepository.getTrackPoints(trackId).first()
val gpxContent = GpxGenerator.generate(track, points)

// Сохранить в файл
val file = File(context.filesDir, "tracks/${track.id}.gpx")
file.writeText(gpxContent)
```

### GpxParser

```kotlin
object GpxParser {
    
    // Распарсить GPX файл
    fun parse(gpxContent: String): GpxTrack
    
    // Распарсить GPX из InputStream
    fun parse(inputStream: InputStream): GpxTrack
}

data class GpxTrack(
    val name: String?,
    val description: String?,
    val startTime: Instant,
    val endTime: Instant,
    val distance: Double,
    val duration: Long,
    val elevationGain: Double,
    val points: List<GpxPoint>
)

data class GpxPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val timestamp: Long
)
```

### GeoJsonGenerator

```kotlin
object GeoJsonGenerator {
    
    // Сгенерировать GeoJSON из трека
    fun generate(
        track: Track,
        points: List<TrackPoint>
    ): String
    
    // Сгенерировать FeatureCollection с заметками
    fun generateWithNotes(
        track: Track,
        points: List<TrackPoint>,
        notes: List<MapNote>
    ): String
}
```

### GeoJsonParser

```kotlin
object GeoJsonParser {
    
    // Распарсить GeoJSON файл
    fun parse(jsonContent: String): GeoJsonTrack
    
    // Распарсить GeoJSON из InputStream
    fun parse(inputStream: InputStream): GeoJsonTrack
}

data class GeoJsonTrack(
    val name: String?,
    val properties: Map<String, Any>,
    val coordinates: List<GeoJsonCoordinate>
)

data class GeoJsonCoordinate(
    val longitude: Double,
    val latitude: Double,
    val altitude: Double? = null
)
```

---

## 🔔 Services

### LocationTrackingService

```kotlin
class LocationTrackingService : LifecycleService()

companion object {
    const val ACTION_START_RECORDING = "start_recording"
    const val ACTION_STOP_RECORDING = "stop_recording"
    const val ACTION_PAUSE_RECORDING = "pause_recording"
    const val ACTION_RESUME_RECORDING = "resume_recording"
    const val ACTION_EMERGENCY_STOP = "emergency_stop"
}
```

**Запуск сервиса:**

```kotlin
// Начать запись
val intent = Intent(context, LocationTrackingService::class.java).apply {
    action = LocationTrackingService.ACTION_START_RECORDING
}
context.startForegroundService(intent)

// Остановить запись
val intent = Intent(context, LocationTrackingService::class.java).apply {
    action = LocationTrackingService.ACTION_STOP_RECORDING
}
context.startService(intent)

// Пауза
val intent = Intent(context, LocationTrackingService::class.java).apply {
    action = LocationTrackingService.ACTION_PAUSE_RECORDING
}
context.startService(intent)

// Возобновить
val intent = Intent(context, LocationTrackingService::class.java).apply {
    action = LocationTrackingService.ACTION_RESUME_RECORDING
}
context.startService(intent)
```

### BufferedPointsSyncService

```kotlin
@Singleton
class BufferedPointsSyncService @Inject constructor(
    private val bufferedPointsRepository: BufferedTrackPointRepository,
    private val trackRepository: TrackRepository,
    private val networkUtils: NetworkUtils
)

// Методы:

// Синхронизировать буферизованные точки
suspend fun syncBufferedPoints()

// Получить количество несинхронизированных точек
suspend fun getUnsyncedPointsCount(): Int

// Начать автоматическую синхронизацию при появлении сети
fun startAutoSync(context: Context)

// Остановить автоматическую синхронизацию
fun stopAutoSync()
```

---

## 🗺️ Navigation Routes

```kotlin
// Главный экран (карта)
navController.navigate("main")

// Список треков
navController.navigate("tracks")

// Детали трека
navController.navigate("track_detail/$trackId")

// Импорт трека
navController.navigate("import_track")

// Настройки
navController.navigate("settings")

// Логи ошибок
navController.navigate("error_logs")

// Список заметок
navController.navigate("notes_list")

// Заметки на карте
navController.navigate("notes_map")

// Заметки на карте с центрированием
navController.navigate("notes_map_center")

// Заметки на карте с центрированием на заметке
navController.navigate("notes_map_note/$noteId")

// Детали заметки
navController.navigate("note_detail/$noteId")

// Добавление заметки
navController.navigate("add_note/$latitude/$longitude")

// Возврат назад
navController.popBackStack()

// Возврат на главный экран
navController.popBackStack("main", inclusive = false)
```

---

## 🎯 Constants

### Database

```kotlin
object DatabaseConstants {
    const val DATABASE_NAME = "tracking_database"
    const val DATABASE_VERSION = 11
    
    object Tables {
        const val TRACKS = "tracks"
        const val TRACK_POINTS = "track_points"
        const val MAP_NOTES = "map_notes"
        const val APP_SETTINGS = "app_settings"
        const val LAST_LOCATION = "last_location"
        const val BUFFERED_TRACK_POINTS = "buffered_track_points"
    }
}
```

### Location

```kotlin
object LocationConstants {
    const val DEFAULT_LOCATION_INTERVAL_MS = 5000L
    const val DEFAULT_MIN_DISTANCE_METERS = 10f
    const val DEFAULT_ACCURACY_THRESHOLD_METERS = 50f
    const val DEFAULT_AUTO_PAUSE_SPEED_THRESHOLD = 1.0f // м/с
    const val DEFAULT_AUTO_PAUSE_DURATION_SEC = 30L
    
    const val MIN_LOCATION_INTERVAL_MS = 1000L
    const val MAX_LOCATION_INTERVAL_MS = 60000L
    
    const val MIN_DISTANCE_METERS = 0f
    const val MAX_DISTANCE_METERS = 100f
    
    const val MIN_ACCURACY_THRESHOLD_METERS = 10f
    const val MAX_ACCURACY_THRESHOLD_METERS = 200f
}
```

### Notifications

```kotlin
object NotificationConstants {
    const val CHANNEL_ID = "route_recording_channel"
    const val CHANNEL_NAME = "Route Recording"
    const val NOTIFICATION_ID = 1
    
    const val DISTANCE_NOTIFICATION_CHANNEL_ID = "distance_notifications"
    const val DISTANCE_NOTIFICATION_CHANNEL_NAME = "Distance Notifications"
    const val DISTANCE_NOTIFICATION_ID = 2
    
    const val MIN_DISTANCE_NOTIFICATION_INTERVAL = 100 // метров
    const val MAX_DISTANCE_NOTIFICATION_INTERVAL = 5000 // метров
    const val DEFAULT_DISTANCE_NOTIFICATION_INTERVAL = 1000 // метров
}
```

### Files

```kotlin
object FileConstants {
    const val TRACKS_DIR = "tracks"
    const val MEDIA_DIR = "media"
    const val LOGS_DIR = "logs"
    const val EXPORT_DIR = "export"
    
    const val LOG_FILE_NAME = "error_log.txt"
    const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5 MB
}
```

---

## ⚙️ Configuration

### Разрешения

```kotlin
object Permissions {
    // Основные разрешения
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    // Android 10+
    val BACKGROUND_LOCATION = arrayOf(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )
    
    // Android 13+
    val NOTIFICATION_PERMISSION = arrayOf(
        Manifest.permission.POST_NOTIFICATIONS
    )
    
    // Опциональные
    val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    
    // Проверка разрешений
    fun hasLocationPermission(context: Context): Boolean {
        return LOCATION_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == 
                PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
```

---

## 📚 Примеры использования

### Запись трека

```kotlin
// В ViewModel
fun startRecording() {
    viewModelScope.launch {
        try {
            // Создать новый трек
            val track = Track(
                id = UUID.randomUUID().toString(),
                name = "Трек ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date}",
                startedAt = Clock.System.now(),
                endedAt = null,
                distanceMeters = 0.0,
                durationSec = 0,
                elevationGainMeters = 0.0,
                gpxPath = null,
                geojsonPath = null,
                isRecording = true
            )
            
            trackRepository.insertTrack(track)
            
            // Запустить сервис
            val intent = Intent(application, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START_RECORDING
            }
            application.startForegroundService(intent)
            
            _events.emit(MainEvent.RecordingStarted)
        } catch (e: Exception) {
            _events.emit(MainEvent.Error(e.message ?: "Unknown error"))
        }
    }
}
```

### Экспорт трека

```kotlin
// В ViewModel
suspend fun exportTrack(trackId: String, format: ExportFormat): Uri {
    val track = trackRepository.getTrackById(trackId).first() 
        ?: throw IllegalArgumentException("Track not found")
    val points = trackRepository.getTrackPoints(trackId).first()
    
    val content = when (format) {
        ExportFormat.GPX -> GpxGenerator.generate(track, points)
        ExportFormat.GEOJSON -> GeoJsonGenerator.generate(track, points)
    }
    
    val extension = when (format) {
        ExportFormat.GPX -> "gpx"
        ExportFormat.GEOJSON -> "geojson"
    }
    
    val file = File(application.filesDir, "export/${track.id}.$extension")
    file.parentFile?.mkdirs()
    file.writeText(content)
    
    return FileProvider.getUriForFile(
        application,
        "${application.packageName}.fileprovider",
        file
    )
}
```

### Создание заметки с фото

```kotlin
// В ViewModel
suspend fun createNoteWithPhoto(
    latitude: Double,
    longitude: Double,
    title: String,
    description: String?,
    photoUri: Uri,
    trackId: String
) {
    // Сохранить фото
    val mediaPath = saveNoteMedia(photoUri, noteId, MediaType.PHOTO)
    
    // Создать заметку
    val note = MapNote(
        id = UUID.randomUUID().toString(),
        trackId = trackId,
        latitude = latitude,
        longitude = longitude,
        title = title,
        description = description,
        timestamp = Clock.System.now(),
        noteType = if (description.isNullOrBlank()) NoteType.PHOTO else NoteType.MIXED,
        mediaPath = mediaPath,
        mediaType = MediaType.PHOTO
    )
    
    mapNoteRepository.insertNote(note)
}

private suspend fun saveNoteMedia(
    uri: Uri,
    noteId: String,
    mediaType: MediaType
): String {
    val extension = when (mediaType) {
        MediaType.PHOTO -> "jpg"
        MediaType.VIDEO -> "mp4"
    }
    
    val fileName = "${noteId}_${System.currentTimeMillis()}.$extension"
    val file = File(application.filesDir, "media/$fileName")
    file.parentFile?.mkdirs()
    
    application.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    
    return file.absolutePath
}
```

---

**Версия:** 1.0  
**Последнее обновление:** 10 октября 2024


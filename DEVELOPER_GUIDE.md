# 👨‍💻 Руководство разработчика XTrack

Практическое руководство для разработчиков, работающих с проектом.

---

## 📋 Содержание

1. [Начало работы](#начало-работы)
2. [Структура проекта](#структура-проекта)
3. [Coding Guidelines](#coding-guidelines)
4. [Работа с базой данных](#работа-с-базой-данных)
5. [Добавление нового функционала](#добавление-нового-функционала)
6. [Отладка](#отладка)
7. [Типичные задачи](#типичные-задачи)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)

---

## Начало работы

### Настройка окружения

#### 1. Требования

- **Android Studio**: Hedgehog | 2023.1.1 или новее
- **JDK**: 17 или новее
- **Android SDK**: API 34
- **Kotlin Plugin**: 1.9.24
- **Gradle**: 8.13

#### 2. Клонирование проекта

```bash
git clone https://github.com/your-org/xtrack.git
cd xtrack
```

#### 3. Настройка API ключей

Создайте файл `app/src/main/res/values/secrets.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="yandex_maps_key">YOUR_YANDEX_MAPS_API_KEY</string>
</resources>
```

**Получение API ключа Yandex MapKit:**
1. Перейдите на https://developer.tech.yandex.ru/
2. Зарегистрируйтесь/войдите
3. Создайте новый проект
4. Получите MapKit API ключ
5. Скопируйте в `secrets.xml`

#### 4. Сборка проекта

```bash
./gradlew clean build
```

#### 5. Запуск на эмуляторе/устройстве

```bash
./gradlew installDebug
```

### Настройка IDE

#### Рекомендуемые плагины

- **Kotlin** (встроенный)
- **Android Jetpack Compose** (встроенный)
- **Room** (для автокомплита)
- **.ignore** (для .gitignore)
- **Rainbow Brackets** (для читаемости)

#### Code Style

Используется официальный Kotlin Code Style:

`Settings → Editor → Code Style → Kotlin → Set from... → Kotlin style guide`

#### Live Templates

Создайте полезные шаблоны кода:

**ViewModel с Hilt:**
```kotlin
@HiltViewModel
class $NAME$ViewModel @Inject constructor(
    private val $REPO$: $REPOSITORY$
) : ViewModel() {
    
    private val _uiState = MutableStateFlow($STATE$())
    val uiState: StateFlow<$STATE$> = _uiState.asStateFlow()
    
    $END$
}
```

**Composable Screen:**
```kotlin
@Composable
fun $NAME$Screen(
    viewModel: $NAME$ViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$TITLE$") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        $END$
    }
}
```

---

## Структура проекта

### Модули

```
TrackingApp/
├── app/                      # Главный модуль приложения
│   ├── src/main/
│   │   ├── java/com/xtrack/
│   │   └── res/
│   └── build.gradle.kts
│
├── build.gradle.kts          # Корневой build script
├── settings.gradle.kts       # Настройки Gradle
└── gradle.properties         # Свойства проекта
```

### Пакеты

**Правило именования:**
- Все в lowercase
- Разделение точками
- Без подчеркиваний

**Структура:**
```
com.xtrack/
├── data/          # Все, что связано с данными
├── di/            # Dependency Injection
├── presentation/  # UI слой
├── service/       # Фоновые сервисы
└── utils/         # Утилиты и хелперы
```

---

## Coding Guidelines

### Kotlin Style Guide

#### Именование

```kotlin
// ✓ Классы - PascalCase
class TrackRepository

// ✓ Функции и переменные - camelCase
fun calculateDistance()
val totalDistance = 0.0

// ✓ Константы - SCREAMING_SNAKE_CASE
const val MAX_DISTANCE = 1000

// ✓ Приватные поля с подчеркиванием (для StateFlow)
private val _uiState = MutableStateFlow()
val uiState = _uiState.asStateFlow()

// ✗ Избегайте венгерской нотации
val strName = "Test"  // ✗ Плохо
val name = "Test"     // ✓ Хорошо
```

#### Null Safety

```kotlin
// ✓ Используйте nullable типы явно
var track: Track? = null

// ✓ Safe call operator
track?.name

// ✓ Elvis operator для default значений
val name = track?.name ?: "Unnamed"

// ✓ let для работы с non-null
track?.let { 
    println(it.name)
}

// ✗ Избегайте !! (null assertion)
track!!.name  // ✗ Опасно! Может упасть

// ✓ Вместо этого используйте проверку
if (track != null) {
    println(track.name)
}
```

#### Coroutines

```kotlin
// ✓ Всегда используйте structured concurrency
viewModelScope.launch {
    // coroutine code
}

// ✓ Обрабатывайте ошибки
viewModelScope.launch {
    try {
        val result = repository.getData()
    } catch (e: Exception) {
        _events.emit(Event.Error(e.message))
    }
}

// ✓ Используйте withContext для смены диспетчера
suspend fun loadData() = withContext(Dispatchers.IO) {
    // IO operations
}

// ✗ Не используйте GlobalScope
GlobalScope.launch { }  // ✗ Плохо - утечка памяти
```

#### Data Classes

```kotlin
// ✓ Используйте data classes для моделей
data class Track(
    val id: String,
    val name: String,
    val distance: Double
)

// ✓ Default значения для опциональных полей
data class TrackPoint(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,  // Опционально
    val speed: Float? = null
)

// ✓ copy() для иммутабельности
val updatedTrack = track.copy(name = "New Name")
```

### Compose Guidelines

#### Композиция

```kotlin
// ✓ Разбивайте на маленькие компоненты
@Composable
fun TrackDetailScreen() {
    Column {
        TrackHeader()
        TrackMap()
        TrackStatistics()
    }
}

// ✓ Извлекайте переиспользуемые компоненты
@Composable
fun StatisticsCard(
    title: String,
    value: String,
    icon: ImageVector
) {
    Card {
        // UI
    }
}

// ✗ Избегайте огромных Composable функций (> 200 строк)
```

#### State Management

```kotlin
// ✓ Поднимайте состояние вверх (State Hoisting)
@Composable
fun Screen() {
    var text by remember { mutableStateOf("") }
    
    SearchBar(
        value = text,
        onValueChange = { text = it }
    )
}

// ✓ Используйте remember для сохранения состояния
val scrollState = rememberScrollState()

// ✓ derivedStateOf для вычисляемого состояния
val isScrolled by remember {
    derivedStateOf { scrollState.value > 0 }
}

// ✓ LaunchedEffect для side effects
LaunchedEffect(key1 = trackId) {
    viewModel.loadTrack(trackId)
}
```

#### Производительность

```kotlin
// ✓ Используйте keys в списках
LazyColumn {
    items(
        items = tracks,
        key = { it.id }  // ← Обязательно!
    ) { track ->
        TrackItem(track)
    }
}

// ✓ Stable callbacks
val onClick = remember { { viewModel.onClick() } }

// ✓ Избегайте создания объектов в recomposition
// ✗ Плохо
Text(
    text = "Distance: ${track.distance} km",
    modifier = Modifier.padding(16.dp)  // ← Создается каждый раз
)

// ✓ Хорошо
val padding = remember { PaddingValues(16.dp) }
Text(
    text = "Distance: ${track.distance} km",
    modifier = Modifier.padding(padding)
)
```

---

## Работа с базой данных

### Создание новой Entity

```kotlin
// 1. Создайте data class
@Entity(tableName = "my_entity")
data class MyEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val timestamp: Instant
)

// 2. Создайте DAO
@Dao
interface MyEntityDao {
    @Query("SELECT * FROM my_entity")
    fun getAll(): Flow<List<MyEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MyEntity)
    
    @Delete
    suspend fun delete(entity: MyEntity)
}

// 3. Добавьте в Database
@Database(
    entities = [Track::class, TrackPoint::class, MyEntity::class],  // ← Добавить
    version = 12,  // ← Увеличить версию
    exportSchema = false
)
abstract class TrackingDatabase : RoomDatabase() {
    // ...
    abstract fun myEntityDao(): MyEntityDao  // ← Добавить
}

// 4. Создайте миграцию
private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE my_entity (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                timestamp TEXT NOT NULL
            )
        """.trimIndent())
    }
}

// 5. Добавьте миграцию в Database
.addMigrations(..., MIGRATION_11_12)
```

### Создание Repository

```kotlin
// 1. Создайте интерфейс (опционально)
interface MyEntityRepository {
    fun getAll(): Flow<List<MyEntity>>
    suspend fun insert(entity: MyEntity)
    suspend fun delete(entityId: String)
}

// 2. Реализация
class MyEntityRepositoryImpl @Inject constructor(
    private val dao: MyEntityDao
) : MyEntityRepository {
    
    override fun getAll(): Flow<List<MyEntity>> = dao.getAll()
    
    override suspend fun insert(entity: MyEntity) {
        withContext(Dispatchers.IO) {
            dao.insert(entity)
        }
    }
    
    override suspend fun delete(entityId: String) {
        withContext(Dispatchers.IO) {
            dao.delete(dao.getById(entityId))
        }
    }
}

// 3. Добавьте в DatabaseModule
@Provides
fun provideMyEntityDao(database: TrackingDatabase): MyEntityDao {
    return database.myEntityDao()
}

@Provides
@Singleton
fun provideMyEntityRepository(dao: MyEntityDao): MyEntityRepository {
    return MyEntityRepositoryImpl(dao)
}
```

### Тестирование Database

```kotlin
@RunWith(AndroidJUnit4::class)
class MyEntityDaoTest {
    
    private lateinit var database: TrackingDatabase
    private lateinit var dao: MyEntityDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TrackingDatabase::class.java
        ).build()
        
        dao = database.myEntityDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndGet() = runTest {
        val entity = MyEntity(
            id = "1",
            name = "Test",
            timestamp = Clock.System.now()
        )
        
        dao.insert(entity)
        val result = dao.getAll().first()
        
        assertEquals(1, result.size)
        assertEquals(entity, result[0])
    }
}
```

---

## Добавление нового функционала

### Пример: Добавление функции "Избранные треки"

#### 1. Обновите модель данных

```kotlin
// Track.kt
@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String,
    // ... existing fields
    val isFavorite: Boolean = false  // ← Новое поле
)
```

#### 2. Создайте миграцию

```kotlin
// DatabaseModule.kt
private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE tracks ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

#### 3. Добавьте методы в DAO

```kotlin
// TrackDao.kt
@Dao
interface TrackDao {
    // ... existing methods
    
    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY startedAt DESC")
    fun getFavoriteTracks(): Flow<List<Track>>
    
    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun updateFavoriteStatus(trackId: String, isFavorite: Boolean)
}
```

#### 4. Обновите Repository

```kotlin
// TrackRepository.kt
class TrackRepository @Inject constructor(
    private val trackDao: TrackDao
) {
    // ... existing methods
    
    fun getFavoriteTracks(): Flow<List<Track>> = trackDao.getFavoriteTracks()
    
    suspend fun toggleFavorite(trackId: String, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            trackDao.updateFavoriteStatus(trackId, isFavorite)
        }
    }
}
```

#### 5. Обновите ViewModel

```kotlin
// TracksListViewModel.kt
@HiltViewModel
class TracksListViewModel @Inject constructor(
    private val repository: TrackRepository
) : ViewModel() {
    
    // ... existing code
    
    val favoriteTracks: StateFlow<List<Track>> = repository
        .getFavoriteTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun toggleFavorite(trackId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(trackId, isFavorite)
            } catch (e: Exception) {
                _events.emit(Event.Error(e.message ?: "Failed to update favorite"))
            }
        }
    }
}
```

#### 6. Обновите UI

```kotlin
// TrackItem.kt
@Composable
fun TrackItem(
    track: Track,
    onToggleFavorite: (Boolean) -> Unit = {},
    // ... other params
) {
    Card {
        Row {
            // ... existing content
            
            IconButton(
                onClick = { onToggleFavorite(!track.isFavorite) }
            ) {
                Icon(
                    imageVector = if (track.isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = "Toggle Favorite",
                    tint = if (track.isFavorite) Color.Red else Color.Gray
                )
            }
        }
    }
}
```

#### 7. Добавьте экран избранного (опционально)

```kotlin
// FavoriteTracksScreen.kt
@Composable
fun FavoriteTracksScreen(
    viewModel: TracksListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val favorites by viewModel.favoriteTracks.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Избранные треки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding)
        ) {
            items(
                items = favorites,
                key = { it.id }
            ) { track ->
                TrackItem(
                    track = track,
                    onToggleFavorite = { isFavorite ->
                        viewModel.toggleFavorite(track.id, isFavorite)
                    }
                )
            }
        }
    }
}
```

#### 8. Добавьте в навигацию

```kotlin
// TrackingNavigation.kt
composable("favorites") {
    FavoriteTracksScreen(
        onNavigateBack = {
            navController.popBackStack()
        }
    )
}
```

---

## Отладка

### Логирование

```kotlin
// Используйте ErrorLogger вместо Log.*
ErrorLogger.logMessage(
    context,
    "Track started: ${track.id}",
    ErrorLogger.LogLevel.INFO
)

ErrorLogger.logError(
    context,
    exception,
    "Failed to save track"
)

// Rate-limited логирование (для частых событий)
RateLimitedLogger.log(
    tag = "LocationUpdate",
    message = "New location: $lat, $lng",
    intervalMs = 5000  // Максимум раз в 5 секунд
)
```

### Debugging GPS

```kotlin
// Включите mock locations в LocationCallback
private fun setupLocationCallback() {
    locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                ErrorLogger.logMessage(
                    this@LocationTrackingService,
                    """
                    Location Update:
                    - Lat/Lng: ${location.latitude}, ${location.longitude}
                    - Accuracy: ${location.accuracy}m
                    - Speed: ${location.speed}m/s
                    - Provider: ${location.provider}
                    - Is Mock: ${location.isFromMockProvider}
                    """.trimIndent(),
                    ErrorLogger.LogLevel.INFO
                )
                
                // ... rest of code
            }
        }
    }
}
```

### Debugging Room

```kotlin
// Включите SQL логирование
Room.databaseBuilder(context, TrackingDatabase::class.java, "tracking_database")
    .setQueryCallback(object : RoomDatabase.QueryCallback {
        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
            Log.d("RoomQuery", "SQL: $sqlQuery | Args: $bindArgs")
        }
    }, Executors.newSingleThreadExecutor())
    .build()
```

### Debugging Compose

```kotlin
// Логирование recompositions
@Composable
fun TrackItem(track: Track) {
    Log.d("Recomposition", "TrackItem recomposed for ${track.id}")
    
    // ... UI code
}

// Проверка, почему происходит recomposition
@Composable
fun Screen() {
    val state by viewModel.state.collectAsState()
    
    // Логируем изменения
    LaunchedEffect(state) {
        Log.d("StateChange", "State changed: $state")
    }
}
```

### Android Studio Profilers

#### CPU Profiler
1. Run → Profile 'app'
2. CPU → Record
3. Выполните действие
4. Stop → Analyze

#### Memory Profiler
1. Run → Profile 'app'
2. Memory → Force garbage collection
3. Heap Dump → Analyze leaks

#### Network Profiler
1. Run → Profile 'app'
2. Network → View requests
3. Проверьте Yandex Maps API calls

---

## Типичные задачи

### Добавление нового экрана

```kotlin
// 1. Создайте ViewModel
@HiltViewModel
class MyScreenViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    // State and logic
}

// 2. Создайте Composable
@Composable
fun MyScreen(
    viewModel: MyScreenViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    Scaffold { padding ->
        // UI
    }
}

// 3. Добавьте маршрут в навигацию
composable("my_screen") {
    MyScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}

// 4. Навигация к экрану
navController.navigate("my_screen")
```

### Добавление нового парсера

```kotlin
// 1. Создайте модель данных
data class KmlTrack(
    val name: String,
    val points: List<KmlPoint>
)

// 2. Создайте парсер
object KmlParser {
    fun parse(kmlContent: String): KmlTrack {
        // XML parsing logic
        return KmlTrack(...)
    }
}

// 3. Создайте генератор
object KmlGenerator {
    fun generate(track: Track, points: List<TrackPoint>): String {
        // XML generation logic
        return xmlString
    }
}

// 4. Добавьте в ParserModule (если нужен DI)
@Module
@InstallIn(SingletonComponent::class)
object ParserModule {
    @Provides
    @Singleton
    fun provideKmlParser(): KmlParser = KmlParser
}
```

### Добавление нового типа настройки

```kotlin
// 1. Добавьте поле в AppSettings
@Entity(tableName = "app_settings")
data class AppSettings(
    // ... existing fields
    val myNewSetting: Boolean = false
)

// 2. Создайте миграцию
private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE app_settings ADD COLUMN myNewSetting INTEGER NOT NULL DEFAULT 0"
        )
    }
}

// 3. Добавьте UI в SettingsScreen
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    
    // ... existing settings
    
    SwitchPreference(
        title = "My New Setting",
        checked = settings.myNewSetting,
        onCheckedChange = { enabled ->
            viewModel.updateSettings(
                settings.copy(myNewSetting = enabled)
            )
        }
    )
}
```

---

## Best Practices

### 1. Всегда используйте Dependency Injection

```kotlin
// ✓ Хорошо - инжектируем зависимости
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel()

// ✗ Плохо - создаем зависимости напрямую
class MyViewModel : ViewModel() {
    private val repository = MyRepository(MyDao())  // ✗
}
```

### 2. Используйте Flow вместо LiveData

```kotlin
// ✓ Хорошо - Flow
val tracks: Flow<List<Track>> = trackDao.getAllTracks()

// ✗ Устаревший подход - LiveData
val tracks: LiveData<List<Track>> = trackDao.getAllTracks()
```

### 3. Разделяйте UI State и Events

```kotlin
// ✓ Хорошо
data class UiState(
    val isLoading: Boolean,
    val data: List<Track>
)

sealed class Event {
    data class ShowSnackbar(val message: String) : Event()
    object NavigateBack : Event()
}

// ✗ Плохо - все в одном State
data class UiState(
    val isLoading: Boolean,
    val data: List<Track>,
    val snackbarMessage: String?,  // ✗ Event, не State
    val shouldNavigateBack: Boolean // ✗ Event, не State
)
```

### 4. Используйте sealed classes для состояний

```kotlin
// ✓ Хорошо
sealed class LoadingState<out T> {
    object Loading : LoadingState<Nothing>()
    data class Success<T>(val data: T) : LoadingState<T>()
    data class Error(val message: String) : LoadingState<Nothing>()
}

// Использование
when (state) {
    is LoadingState.Loading -> ShowLoading()
    is LoadingState.Success -> ShowData(state.data)
    is LoadingState.Error -> ShowError(state.message)
}
```

### 5. Проверяйте разрешения правильно

```kotlin
// ✓ Хорошо - используйте Accompanist Permissions
@Composable
fun RequestLocationPermission(
    onPermissionGranted: () -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            onPermissionGranted()
        }
    }
    
    if (!permissionState.allPermissionsGranted) {
        PermissionDialog(
            onRequestPermission = { permissionState.launchMultiplePermissionRequest() }
        )
    }
}
```

### 6. Обрабатывайте ошибки во всех слоях

```kotlin
// ViewModel
fun loadData() {
    viewModelScope.launch {
        try {
            _uiState.value = UiState.Loading
            val data = repository.getData()
            _uiState.value = UiState.Success(data)
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Unknown error")
            ErrorLogger.logError(application, e, "Failed to load data")
        }
    }
}

// Repository
suspend fun getData(): List<Data> = withContext(Dispatchers.IO) {
    try {
        dao.getAllData()
    } catch (e: SQLException) {
        ErrorLogger.logError(context, e, "Database error")
        throw DatabaseException("Failed to load data", e)
    }
}
```

### 7. Используйте Type-Safe Navigation (если возможно)

```kotlin
// Определите routes как константы
object Routes {
    const val MAIN = "main"
    const val TRACKS = "tracks"
    const val TRACK_DETAIL = "track_detail"
    
    fun trackDetail(trackId: String) = "$TRACK_DETAIL/$trackId"
}

// Использование
navController.navigate(Routes.TRACKS)
navController.navigate(Routes.trackDetail(trackId))
```

### 8. Документируйте сложную логику

```kotlin
/**
 * Рассчитывает набор высоты для трека.
 * 
 * Алгоритм:
 * 1. Фильтрует точки с altitude != null
 * 2. Сортирует по timestamp
 * 3. Суммирует только положительные изменения высоты
 * 
 * @param points Список точек трека
 * @return Набор высоты в метрах
 */
fun calculateElevationGain(points: List<TrackPoint>): Double {
    return points
        .filter { it.altitude != null }
        .sortedBy { it.timestamp }
        .zipWithNext { a, b -> (b.altitude!! - a.altitude!!).coerceAtLeast(0.0) }
        .sum()
}
```

---

## Troubleshooting

### Проблема: Room migration failed

**Симптомы:**
```
A migration from 11 to 12 was required but not found
```

**Решение:**
1. Убедитесь, что миграция добавлена в `.addMigrations()`
2. Проверьте, что версия БД увеличена
3. Если тестируете - удалите и переустановите приложение
4. Проверьте SQL в миграции:
```kotlin
database.execSQL("SELECT * FROM sqlite_master WHERE type='table'")
```

### Проблема: LocationTrackingService не запускается

**Симптомы:**
- Сервис не показывает уведомление
- GPS точки не записываются

**Решение:**
1. Проверьте разрешения:
```kotlin
if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    != PackageManager.PERMISSION_GRANTED) {
    // Request permission
}
```

2. Проверьте, что `startForeground()` вызывается в течение 5 секунд:
```kotlin
override fun onStartCommand(...): Int {
    super.onStartCommand(intent, flags, startId)
    startForeground(NOTIFICATION_ID, createNotification())  // ← Сразу
    // ... rest of code
}
```

3. Проверьте настройки Android:
- Settings → Apps → XTrack → Permissions → Location → Allow all the time
- Settings → Apps → XTrack → Battery → Unrestricted

### Проблема: Compose recomposition слишком частая

**Симптомы:**
- Лаги в UI
- Высокая нагрузка на CPU

**Решение:**
1. Используйте Layout Inspector: Tools → Layout Inspector
2. Включите Composition Counts:
```kotlin
// В build.gradle.kts
composeOptions {
    kotlinCompilerExtensionVersion = "1.5.14"
}

// В код
import androidx.compose.runtime.currentRecomposeScope
```

3. Проверьте keys в LazyColumn:
```kotlin
LazyColumn {
    items(
        items = tracks,
        key = { it.id }  // ← Обязательно!
    ) { track ->
        TrackItem(track)
    }
}
```

4. Используйте `remember` для stable объектов:
```kotlin
val padding = remember { PaddingValues(16.dp) }
```

### Проблема: Yandex Maps не отображается

**Симптомы:**
- Белый экран вместо карты
- Ошибка "API key not set"

**Решение:**
1. Проверьте API ключ в `strings.xml`
2. Проверьте инициализацию в `XTrackApplication`:
```kotlin
MapKitFactory.setApiKey(apiKey)
MapKitFactory.initialize(this)
```

3. Проверьте, что карта запускается/останавливается:
```kotlin
DisposableEffect(Unit) {
    mapView.onStart()
    MapKitFactory.getInstance().onStart()
    onDispose {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
    }
}
```

4. Проверьте разрешения:
- `INTERNET`
- `ACCESS_NETWORK_STATE`

### Проблема: Gradle build failed

**Симптомы:**
```
Could not resolve dependencies
```

**Решение:**
1. Очистите кэш:
```bash
./gradlew clean
./gradlew --stop
rm -rf ~/.gradle/caches/
```

2. Синхронизируйте Gradle:
File → Sync Project with Gradle Files

3. Invalidate Caches:
File → Invalidate Caches / Restart

4. Проверьте версии в `build.gradle.kts`

---

## Полезные команды

```bash
# Сборка
./gradlew assembleDebug
./gradlew assembleRelease

# Установка
./gradlew installDebug
./gradlew installRelease

# Тесты
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests

# Очистка
./gradlew clean

# Проверка зависимостей
./gradlew dependencies

# Генерация отчета о размере APK
./gradlew app:buildAnalyzer

# Проверка lint
./gradlew lint

# Проверка кода
./gradlew detekt  # Если настроен Detekt
```

---

## Контрольный список перед коммитом

- [ ] Код компилируется без ошибок
- [ ] Все тесты проходят
- [ ] Нет lint warnings
- [ ] Код отформатирован (Ctrl+Alt+L)
- [ ] Удалены неиспользуемые импорты (Ctrl+Alt+O)
- [ ] Добавлены комментарии к сложной логике
- [ ] Обновлена документация (если нужно)
- [ ] Протестировано на реальном устройстве
- [ ] Проверены разрешения
- [ ] Логирование добавлено для важных операций

---

## Полезные ссылки

### Официальная документация

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Yandex MapKit](https://yandex.ru/dev/maps/mapkit/doc/)

### Инструменты

- [Android Studio](https://developer.android.com/studio)
- [Gradle](https://docs.gradle.org/)
- [Git](https://git-scm.com/doc)

### Сообщество

- [Stack Overflow - Android](https://stackoverflow.com/questions/tagged/android)
- [Kotlin Slack](https://kotlinlang.slack.com/)
- [Reddit - AndroidDev](https://www.reddit.com/r/androiddev/)

---

**Версия:** 1.0  
**Последнее обновление:** 10 октября 2024  

**Вопросы?** Создайте issue в репозитории или спросите в команде.


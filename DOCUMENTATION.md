# 📚 Полная документация проекта XTrack

## 📋 Содержание

1. [Обзор проекта](#обзор-проекта)
2. [Архитектура приложения](#архитектура-приложения)
3. [Модули и компоненты](#модули-и-компоненты)
4. [Функциональные возможности](#функциональные-возможности)
5. [Структура базы данных](#структура-базы-данных)
6. [Навигация](#навигация)
7. [Сервисы и фоновая работа](#сервисы-и-фоновая-работа)
8. [API и интеграции](#api-и-интеграции)
9. [Управление состоянием](#управление-состоянием)
10. [Работа с файлами](#работа-с-файлами)
11. [Настройка и конфигурация](#настройка-и-конфигурация)
12. [Тестирование](#тестирование)

---

## Обзор проекта

**XTrack** - это современное Android приложение для отслеживания GPS-треков с расширенными возможностями создания заметок, экспорта данных и детальной аналитики маршрутов.

### Основные характеристики

- **Платформа**: Android (минимум API 24, целевой API 34)
- **Язык**: Kotlin
- **UI Framework**: Jetpack Compose
- **Архитектура**: MVVM + Clean Architecture
- **DI**: Hilt/Dagger
- **База данных**: Room
- **Асинхронность**: Kotlin Coroutines + Flow
- **Карты**: Yandex MapKit

### Технологический стек

| Категория | Технология | Версия |
|-----------|-----------|---------|
| **Core** | Kotlin | 1.9.24 |
| **UI** | Jetpack Compose | 2024.02.00 |
| **DI** | Hilt | 2.48 |
| **Database** | Room | 2.6.1 |
| **Navigation** | Navigation Compose | 2.7.5 |
| **Maps** | Yandex MapKit | 4.5.1-full |
| **Location** | Google Play Services Location | 21.0.1 |
| **Serialization** | Gson | 2.10.1 |
| **Async** | Coroutines | 1.7.3 |
| **Background** | WorkManager | 2.9.0 |

---

## Архитектура приложения

### Паттерн MVVM

```
┌─────────────────────────────────────────────────────────────┐
│                        Presentation Layer                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Screens    │  │  ViewModels  │  │  Components  │      │
│  │  (Compose)   │←→│   (State)    │←→│   (Reusable) │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                           ↕
┌─────────────────────────────────────────────────────────────┐
│                        Domain Layer                          │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │ Repositories │  │   Use Cases  │                         │
│  └──────────────┘  └──────────────┘                         │
└─────────────────────────────────────────────────────────────┘
                           ↕
┌─────────────────────────────────────────────────────────────┐
│                         Data Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Database   │  │   Parsers    │  │   Services   │      │
│  │    (Room)    │  │ (GPX/JSON)   │  │  (Location)  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### Структура пакетов

```
com.xtrack/
├── data/                          # Слой данных
│   ├── database/                  # Room DAO и Database
│   │   ├── AppSettingsDao.kt
│   │   ├── TrackDao.kt
│   │   ├── TrackPointDao.kt
│   │   ├── MapNoteDao.kt
│   │   ├── BufferedTrackPointDao.kt
│   │   ├── LastLocationDao.kt
│   │   ├── Converters.kt          # Type converters для Room
│   │   └── TrackingDatabase.kt    # Главная БД
│   ├── model/                     # Модели данных (Entities)
│   │   ├── Track.kt               # Трек (маршрут)
│   │   ├── TrackPoint.kt          # Точка трека
│   │   ├── MapNote.kt             # Заметка на карте
│   │   ├── AppSettings.kt         # Настройки приложения
│   │   ├── BufferedTrackPoint.kt  # Буферизованная точка
│   │   ├── LastLocation.kt        # Последняя позиция
│   │   ├── Point.kt               # Простая точка (lat/lng)
│   │   ├── GpxTrack.kt            # GPX структуры
│   │   └── GeoJsonTrack.kt        # GeoJSON структуры
│   ├── parser/                    # Парсеры файлов
│   │   ├── GpxParser.kt           # Парсинг GPX
│   │   ├── GpxGenerator.kt        # Генерация GPX
│   │   ├── GeoJsonParser.kt       # Парсинг GeoJSON
│   │   ├── GeoJsonGenerator.kt    # Генерация GeoJSON
│   │   └── TrackImportParser.kt   # Импорт треков
│   └── repository/                # Репозитории (Data Access)
│       ├── TrackRepository.kt
│       ├── SettingsRepository.kt
│       ├── MapNoteRepository.kt
│       ├── BufferedTrackPointRepository.kt
│       └── LastLocationRepository.kt
│
├── di/                            # Dependency Injection
│   ├── DatabaseModule.kt          # Модуль БД
│   └── ParserModule.kt            # Модуль парсеров
│
├── presentation/                  # Слой представления
│   ├── screen/                    # Экраны Compose
│   │   ├── MainScreen.kt          # Главный экран (карта + запись)
│   │   ├── TracksListScreen.kt    # Список треков
│   │   ├── TrackDetailScreen.kt   # Детали трека
│   │   ├── SettingsScreen.kt      # Настройки
│   │   ├── NotesListScreen.kt     # Список заметок
│   │   ├── NotesMapScreen.kt      # Заметки на карте
│   │   ├── NoteDetailScreen.kt    # Детали заметки
│   │   ├── AddNoteScreen.kt       # Добавление заметки
│   │   ├── ImportTrackScreen.kt   # Импорт трека
│   │   └── ErrorLogScreen.kt      # Просмотр логов ошибок
│   ├── viewmodel/                 # ViewModel'ы
│   │   ├── MainViewModel.kt
│   │   ├── TracksListViewModel.kt
│   │   ├── TrackDetailViewModel.kt
│   │   ├── SettingsViewModel.kt
│   │   ├── NotesListViewModel.kt
│   │   ├── NotesMapViewModel.kt
│   │   ├── NoteDetailViewModel.kt
│   │   ├── AddNoteViewModel.kt
│   │   ├── ImportTrackViewModel.kt
│   │   └── ExportNotesViewModel.kt
│   ├── components/                # Переиспользуемые компоненты
│   │   ├── MapView.kt             # Компонент карты Yandex
│   │   ├── StatisticsCard.kt      # Карточка статистики
│   │   └── TrackItem.kt           # Элемент списка треков
│   ├── navigation/                # Навигация
│   │   └── TrackingNavigation.kt  # Граф навигации
│   ├── theme/                     # Тема Material Design 3
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── MainActivity.kt            # Главная Activity
│
├── service/                       # Фоновые сервисы
│   ├── LocationTrackingService.kt # Сервис записи трека
│   └── BufferedPointsSyncService.kt # Синхронизация точек
│
├── utils/                         # Утилиты
│   ├── ErrorLogger.kt             # Логирование ошибок в файл
│   ├── GlobalExceptionHandler.kt  # Глобальный обработчик ошибок
│   ├── LocationUtils.kt           # Утилиты для геолокации
│   ├── NetworkUtils.kt            # Утилиты для сети
│   ├── RateLimitedLogger.kt       # Лимитированное логирование
│   └── ServiceUtils.kt            # Утилиты для сервисов
│
└── XTrackApplication.kt           # Application класс
```

---

## Модули и компоненты

### 1. Data Layer (Слой данных)

#### 1.1 Database Module

**TrackingDatabase** - главная база данных на Room с версией 11.

**Таблицы:**
- `tracks` - треки/маршруты
- `track_points` - GPS точки треков
- `map_notes` - заметки на карте
- `app_settings` - настройки приложения
- `last_location` - последняя известная позиция
- `buffered_track_points` - буферизованные точки для синхронизации

**Миграции:**
- Версии 1-11 с полной миграционной цепочкой
- Поддержка обновления схемы БД без потери данных
- Fallback на destructive migration при критических ошибках

#### 1.2 Repositories

**TrackRepository:**
```kotlin
// Основные операции с треками
- getAllTracks(): Flow<List<Track>>
- getTrackById(id: String): Flow<Track?>
- getActiveTrack(): Flow<Track?>
- insertTrack(track: Track)
- updateTrack(track: Track)
- deleteTrack(trackId: String)
- getTrackPoints(trackId: String): Flow<List<TrackPoint>>
- insertTrackPoint(point: TrackPoint)
- deleteTrackPoints(trackId: String)
```

**SettingsRepository:**
```kotlin
// Управление настройками
- getSettings(): Flow<AppSettings>
- updateSettings(settings: AppSettings)
- getLocationAccuracy(): Flow<LocationAccuracy>
- updateLocationAccuracy(accuracy: LocationAccuracy)
```

**MapNoteRepository:**
```kotlin
// Работа с заметками на карте
- getAllNotes(): Flow<List<MapNote>>
- getNoteById(id: String): Flow<MapNote?>
- getNotesForTrack(trackId: String): Flow<List<MapNote>>
- insertNote(note: MapNote)
- updateNote(note: MapNote)
- deleteNote(noteId: String)
```

**BufferedTrackPointRepository:**
```kotlin
// Буферизация точек при отсутствии сети
- insertBufferedPoint(point: BufferedTrackPoint)
- getUnsyncedPoints(): List<BufferedTrackPoint>
- markAsSynced(pointId: String)
- deleteBufferedPoints(trackId: String)
```

#### 1.3 Parsers

**GpxParser & GpxGenerator:**
- Парсинг GPX 1.1 файлов
- Генерация GPX с метаданными (треки, waypoints)
- Поддержка расширений (extensions)

**GeoJsonParser & GeoJsonGenerator:**
- Парсинг GeoJSON Feature Collections
- Генерация GeoJSON LineString для треков
- Поддержка properties для метаданных

**TrackImportParser:**
- Универсальный импорт GPX и GeoJSON
- Автоматическое определение формата
- Валидация данных

### 2. Presentation Layer (Слой представления)

#### 2.1 Screens (Экраны)

**MainScreen** - главный экран приложения
```kotlin
Функционал:
- Отображение интерактивной карты (Yandex MapKit)
- Кнопки управления записью (Старт/Стоп/Пауза)
- Отображение текущих метрик (расстояние, время, скорость)
- Добавление заметок на карте (долгое нажатие)
- Отслеживание текущей позиции
- Отображение записываемого маршрута в реальном времени
```

**TracksListScreen** - список сохраненных треков
```kotlin
Функционал:
- Список всех треков с превью
- Сортировка по дате
- Поиск по названию
- Удаление треков
- Экспорт треков (GPX, GeoJSON)
- Переход к деталям трека
- Импорт треков
```

**TrackDetailScreen** - детали трека
```kotlin
Функционал:
- Отображение маршрута на карте
- Детальная статистика:
  * Расстояние
  * Время
  * Средняя/максимальная скорость
  * Набор высоты
  * График высоты/скорости
- Список точек маршрута
- Экспорт в GPX/GeoJSON
- Удаление трека
```

**SettingsScreen** - настройки приложения
```kotlin
Функционал:
- Настройки GPS:
  * Точность (HIGH/BALANCED/LOW_POWER)
  * Интервал записи (мс)
  * Минимальное расстояние (м)
  * Порог точности (м)
- Автопауза:
  * Включение/выключение
  * Порог скорости
  * Длительность паузы
- Экспорт:
  * Формат по умолчанию (GPX/GeoJSON)
- Уведомления:
  * Интервал уведомлений о расстоянии
  * Включение/выключение уведомлений
- Диагностика:
  * Просмотр логов ошибок
  * Очистка кэша
  * Информация о приложении
- Управление заметками:
  * Просмотр всех заметок
  * Экспорт заметок
```

**NotesListScreen** - список заметок
```kotlin
Функционал:
- Список всех заметок с превью
- Фильтрация по типу (текст/фото/видео)
- Переход к деталям заметки
- Переход к заметке на карте
- Удаление заметок
- Экспорт заметок
```

**NotesMapScreen** - заметки на карте
```kotlin
Функционал:
- Отображение всех заметок на карте
- Кластеризация заметок
- Центрирование на выбранной заметке
- Превью заметки при клике
- Переход к деталям заметки
```

**NoteDetailScreen** - детали заметки
```kotlin
Функционал:
- Отображение полной информации о заметке
- Просмотр фото/видео (если есть)
- Координаты и время создания
- Переход к заметке на карте
- Редактирование заметки
- Удаление заметки
```

**AddNoteScreen** - добавление заметки
```kotlin
Функционал:
- Создание текстовой заметки
- Прикрепление фото (камера/галерея)
- Прикрепление видео (камера/галерея)
- Автоматическая привязка к текущему треку
- Сохранение координат
```

**ImportTrackScreen** - импорт трека
```kotlin
Функционал:
- Выбор GPX/GeoJSON файла
- Предпросмотр трека
- Валидация данных
- Импорт в базу данных
```

**ErrorLogScreen** - просмотр логов ошибок
```kotlin
Функционал:
- Просмотр всех логов ошибок
- Фильтрация по уровню (ERROR/WARNING/INFO)
- Поиск по тексту
- Экспорт логов
- Очистка логов
```

#### 2.2 ViewModels

Все ViewModel'ы используют:
- `StateFlow` для состояния UI
- `SharedFlow` для одноразовых событий
- Корутины для асинхронных операций
- Hilt для Dependency Injection

**Основные ViewModel'ы:**

```kotlin
MainViewModel:
- currentTrack: StateFlow<Track?>
- isRecording: StateFlow<Boolean>
- isPaused: StateFlow<Boolean>
- currentMetrics: StateFlow<TrackMetrics>
- startRecording()
- stopRecording()
- pauseRecording()
- resumeRecording()
- addNote(lat: Double, lng: Double)

TracksListViewModel:
- tracks: StateFlow<List<Track>>
- searchQuery: StateFlow<String>
- deleteTrack(trackId: String)
- exportTrack(trackId: String, format: ExportFormat)
- importTrack(uri: Uri)

TrackDetailViewModel:
- track: StateFlow<Track?>
- trackPoints: StateFlow<List<TrackPoint>>
- statistics: StateFlow<TrackStatistics>
- exportTrack(format: ExportFormat)
- deleteTrack()

SettingsViewModel:
- settings: StateFlow<AppSettings>
- updateSettings(settings: AppSettings)
- resetToDefaults()
- exportLogs()
- clearCache()
```

#### 2.3 Components

**MapView** - компонент карты Yandex MapKit
```kotlin
@Composable
fun MapView(
    modifier: Modifier = Modifier,
    currentLocation: Point?,
    trackPoints: List<TrackPoint>,
    mapNotes: List<MapNote>,
    onMapClick: (Point) -> Unit,
    onMapLongClick: (Point) -> Unit,
    followUser: Boolean = true,
    centerOnPoint: Point? = null
)
```

**StatisticsCard** - карточка статистики
```kotlin
@Composable
fun StatisticsCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
)
```

**TrackItem** - элемент списка треков
```kotlin
@Composable
fun TrackItem(
    track: Track,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: (ExportFormat) -> Unit
)
```

### 3. Service Layer (Слой сервисов)

#### 3.1 LocationTrackingService

**Foreground сервис** для записи GPS-треков в фоновом режиме.

**Возможности:**
- Непрерывная запись GPS-координат
- Работа в фоне с уведомлением
- Поддержка паузы/возобновления
- Буферизация точек при отсутствии сети
- Автоматическая пауза при низкой скорости
- Обновление уведомления с метриками
- Экстренная остановка при критических ошибках

**Настройки точности:**
```kotlin
LocationRequest.PRIORITY_HIGH_ACCURACY     // Высокая точность (GPS)
LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY  // Баланс (GPS + Network)
LocationRequest.PRIORITY_LOW_POWER         // Низкое энергопотребление
```

**Логика записи точек:**
```kotlin
1. Получение GPS-координат от FusedLocationProvider
2. Фильтрация по точности (accuracyThresholdMeters)
3. Фильтрация по расстоянию (minDistanceMeters)
4. Сохранение в основную БД (track_points)
5. Буферизация при отсутствии сети (buffered_track_points)
6. Обновление метрик трека (расстояние, время, высота)
7. Обновление уведомления
```

**Уведомления:**
- Постоянное foreground уведомление
- Обновление метрик каждые 5 секунд
- Кнопки управления (Пауза/Стоп) в уведомлении
- Уведомления о пройденном расстоянии (опционально)

#### 3.2 BufferedPointsSyncService

**Сервис синхронизации** буферизованных точек.

**Функционал:**
- Мониторинг сетевого подключения
- Автоматическая синхронизация при появлении сети
- Перенос точек из buffered_track_points в track_points
- Обновление метрик треков после синхронизации
- Rate limiting для логирования

### 4. Utils Layer (Утилиты)

#### 4.1 ErrorLogger

**Система логирования ошибок** в файл.

```kotlin
ErrorLogger.logError(context, exception, message)
ErrorLogger.logMessage(context, message, LogLevel.INFO)
ErrorLogger.getLogFile(context): File
ErrorLogger.clearLogs(context)
```

**Уровни логов:**
- `ERROR` - критические ошибки
- `WARNING` - предупреждения
- `INFO` - информационные сообщения

**Формат лога:**
```
2024-10-10 15:30:45 [ERROR] MainActivity: Ошибка инициализации карты
java.lang.Exception: MapKit not initialized
    at com.xtrack.presentation.MainActivity.onCreate(...)
    ...
```

#### 4.2 GlobalExceptionHandler

**Глобальный обработчик** необработанных исключений.

```kotlin
GlobalExceptionHandler.install(application)
```

Перехватывает все необработанные исключения и логирует их в файл перед падением приложения.

#### 4.3 LocationUtils

**Утилиты для работы с геолокацией:**

```kotlin
LocationUtils.calculateDistance(lat1, lng1, lat2, lng2): Float
LocationUtils.calculateBearing(lat1, lng1, lat2, lng2): Float
LocationUtils.calculateSpeed(distance, timeDelta): Float
LocationUtils.formatDistance(meters): String
LocationUtils.formatSpeed(mps): String
LocationUtils.formatDuration(seconds): String
```

#### 4.4 NetworkUtils

**Утилиты для работы с сетью:**

```kotlin
NetworkUtils.isNetworkAvailable(context): Boolean
NetworkUtils.startNetworkMonitoring(context, callback)
NetworkUtils.stopNetworkMonitoring(context)
```

#### 4.5 RateLimitedLogger

**Логирование с ограничением частоты:**

```kotlin
RateLimitedLogger.log(tag, message, intervalMs)
```

Предотвращает спам в логах при частых событиях.

#### 4.6 ServiceUtils

**Утилиты для работы с сервисами:**

```kotlin
ServiceUtils.isServiceRunning(context, serviceClass): Boolean
ServiceUtils.startForegroundService(context, intent)
ServiceUtils.stopService(context, serviceClass)
```

---

## Функциональные возможности

### 1. Запись GPS-треков

**Процесс записи:**

1. Пользователь нажимает кнопку "Старт"
2. Запрашиваются разрешения на геолокацию
3. Создается новый трек в БД
4. Запускается LocationTrackingService
5. Сервис получает GPS-координаты
6. Точки фильтруются и сохраняются
7. Обновляются метрики в реальном времени
8. При остановке:
   - Завершается запись
   - Обновляются финальные метрики
   - Генерируются GPX/GeoJSON файлы (опционально)

**Фильтрация точек:**
- По точности (accuracy < accuracyThresholdMeters)
- По расстоянию (distance > minDistanceMeters)
- По времени (interval >= locationIntervalMs)

**Метрики трека:**
- Общее расстояние (км)
- Время в движении (чч:мм:сс)
- Средняя скорость (км/ч)
- Максимальная скорость (км/ч)
- Набор высоты (м)
- Количество точек

### 2. Работа с заметками

**Типы заметок:**
- **TEXT** - текстовая заметка
- **PHOTO** - заметка с фото
- **VIDEO** - заметка с видео
- **MIXED** - текст + медиа

**Создание заметки:**
1. Долгое нажатие на карту или кнопка "Добавить заметку"
2. Открывается AddNoteScreen
3. Вводится текст заметки
4. Опционально прикрепляется фото/видео
5. Сохраняется с текущими координатами
6. Привязывается к текущему треку (если идет запись)

**Просмотр заметок:**
- Список всех заметок (NotesListScreen)
- Заметки на карте (NotesMapScreen) с кластеризацией
- Детальный просмотр (NoteDetailScreen)
- Фильтрация по типу и треку

### 3. Экспорт/Импорт

**Экспорт треков:**

**GPX формат:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="XTrack">
  <metadata>
    <name>Трек 2024-10-10</name>
    <time>2024-10-10T12:00:00Z</time>
  </metadata>
  <trk>
    <name>Трек 2024-10-10</name>
    <trkseg>
      <trkpt lat="55.751244" lon="37.618423">
        <ele>150.5</ele>
        <time>2024-10-10T12:00:00Z</time>
      </trkpt>
      ...
    </trkseg>
  </trk>
</gpx>
```

**GeoJSON формат:**
```json
{
  "type": "FeatureCollection",
  "features": [{
    "type": "Feature",
    "geometry": {
      "type": "LineString",
      "coordinates": [
        [37.618423, 55.751244, 150.5],
        ...
      ]
    },
    "properties": {
      "name": "Трек 2024-10-10",
      "time": "2024-10-10T12:00:00Z",
      "distance": 5420.5,
      "duration": 1800
    }
  }]
}
```

**Импорт треков:**
- Поддержка GPX 1.0/1.1
- Поддержка GeoJSON FeatureCollection
- Автоматическое определение формата
- Валидация данных
- Сохранение в БД

### 4. Настройки GPS

**Точность геолокации:**
- **HIGH_ACCURACY** - максимальная точность, высокое энергопотребление (GPS)
- **BALANCED** - баланс точности и энергопотребления (GPS + Network)
- **LOW_POWER** - низкое энергопотребление, низкая точность

**Интервал записи:**
- Минимум: 1000 мс (1 сек)
- По умолчанию: 5000 мс (5 сек)
- Максимум: 60000 мс (1 мин)

**Минимальное расстояние:**
- Минимум: 0 м
- По умолчанию: 10 м
- Максимум: 100 м

**Порог точности:**
- Минимум: 10 м
- По умолчанию: 50 м
- Максимум: 200 м

### 5. Автопауза

**Логика автопаузы:**
1. Скорость падает ниже порога (autoPauseSpeedThreshold)
2. Ожидание autoPauseDurationSec секунд
3. Если скорость не восстановилась - пауза
4. При превышении порога скорости - автоматическое возобновление

**Настройки:**
- **Порог скорости**: 0.5 - 5.0 м/с (по умолчанию 1.0 м/с)
- **Длительность паузы**: 10 - 120 сек (по умолчанию 30 сек)

### 6. Уведомления о расстоянии

**Функционал:**
- Периодические уведомления о пройденном расстоянии
- Настраиваемый интервал (100-5000 м)
- Включение/выключение
- Отображение текущих метрик в уведомлении

---

## Структура базы данных

### Схема базы данных (версия 11)

#### Таблица: tracks
```sql
CREATE TABLE tracks (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    startedAt TEXT NOT NULL,        -- ISO 8601 timestamp
    endedAt TEXT,                    -- ISO 8601 timestamp
    distanceMeters REAL NOT NULL,
    durationSec INTEGER NOT NULL,
    elevationGainMeters REAL NOT NULL DEFAULT 0.0,
    gpxPath TEXT,
    geojsonPath TEXT,
    isRecording INTEGER NOT NULL DEFAULT 0
)
```

**Индексы:**
- PRIMARY KEY on `id`
- INDEX on `startedAt` (для сортировки)
- INDEX on `isRecording` (для поиска активного трека)

#### Таблица: track_points
```sql
CREATE TABLE track_points (
    id TEXT PRIMARY KEY NOT NULL,
    trackId TEXT NOT NULL,           -- FK to tracks
    timestamp INTEGER NOT NULL,       -- Unix timestamp (ms)
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    altitude REAL,
    speed REAL,
    bearing REAL,
    accuracy REAL NOT NULL,
    FOREIGN KEY (trackId) REFERENCES tracks(id) ON DELETE CASCADE
)
```

**Индексы:**
- PRIMARY KEY on `id`
- INDEX on `trackId` (для быстрого поиска точек трека)
- INDEX on `timestamp` (для сортировки)

#### Таблица: map_notes
```sql
CREATE TABLE map_notes (
    id TEXT PRIMARY KEY NOT NULL,
    trackId TEXT NOT NULL,           -- FK to tracks
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    timestamp TEXT NOT NULL,         -- ISO 8601 timestamp
    noteType TEXT NOT NULL,          -- TEXT, PHOTO, VIDEO, MIXED
    mediaPath TEXT,
    mediaType TEXT,                  -- PHOTO, VIDEO
    FOREIGN KEY (trackId) REFERENCES tracks(id) ON DELETE CASCADE
)
```

**Индексы:**
- PRIMARY KEY on `id`
- INDEX on `trackId`
- INDEX on `noteType`

#### Таблица: app_settings
```sql
CREATE TABLE app_settings (
    id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
    locationAccuracy TEXT NOT NULL DEFAULT 'BALANCED',
    locationIntervalMs INTEGER NOT NULL DEFAULT 5000,
    minDistanceMeters REAL NOT NULL DEFAULT 10.0,
    accuracyThresholdMeters REAL NOT NULL DEFAULT 50.0,
    autoPauseEnabled INTEGER NOT NULL DEFAULT 0,
    autoPauseSpeedThreshold REAL NOT NULL DEFAULT 1.0,
    autoPauseDurationSec INTEGER NOT NULL DEFAULT 30,
    defaultExportFormat TEXT NOT NULL DEFAULT 'GPX',
    appExitState TEXT NOT NULL DEFAULT 'STOPPED',
    distanceNotificationIntervalMeters INTEGER NOT NULL DEFAULT 1000,
    distanceNotificationsEnabled INTEGER NOT NULL DEFAULT 0
)
```

**Примечание:** Всегда содержит одну запись с `id = 1`.

#### Таблица: last_location
```sql
CREATE TABLE last_location (
    id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    timestamp TEXT NOT NULL,         -- ISO 8601 timestamp
    accuracy REAL
)
```

**Примечание:** Всегда содержит одну запись с `id = 1` (последняя известная позиция).

#### Таблица: buffered_track_points
```sql
CREATE TABLE buffered_track_points (
    id TEXT PRIMARY KEY NOT NULL,
    trackId TEXT NOT NULL,
    timestamp INTEGER NOT NULL,      -- Unix timestamp (ms)
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    altitude REAL,
    speed REAL,
    bearing REAL,
    accuracy REAL NOT NULL,
    isSynced INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL       -- Unix timestamp (ms)
)
```

**Индексы:**
- PRIMARY KEY on `id`
- INDEX on `trackId`
- INDEX on `isSynced` (для поиска несинхронизированных точек)

### Связи между таблицами

```
tracks (1) ──< (N) track_points
tracks (1) ──< (N) map_notes
tracks (1) ──< (N) buffered_track_points
```

### Миграции базы данных

**История миграций:**

| Версия | Описание |
|--------|----------|
| 1→2 | Добавлена таблица `map_notes` |
| 2→3 | Добавлена таблица `last_location` |
| 3→4 | Добавлено поле `wasRecordingOnExit` в `app_settings` |
| 4→5 | Добавлена таблица `buffered_track_points` |
| 5→6 | Добавлены поля уведомлений о расстоянии |
| 6→7 | Исправление целостности таблицы `app_settings` |
| 7→8 | Добавлено поле `elevationGainMeters` в `tracks` |
| 8→9 | Обновление хэша схемы |
| 9→10 | Добавлены TypeConverters для enum |
| 10→11 | Замена `wasRecordingOnExit` на `appExitState` |

---

## Навигация

### Граф навигации

```
                    ┌─────────────┐
                    │    main     │ (MainScreen)
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
  ┌──────────┐      ┌──────────┐      ┌──────────────┐
  │  tracks  │      │ settings │      │add_note/{lat}│
  │          │      │          │      │     /{lng}   │
  └────┬─────┘      └────┬─────┘      └──────────────┘
       │                 │
       │                 ├─────► error_logs
       │                 │
       │                 ├─────► notes_list ──┬──► note_detail/{id}
       │                 │                    │
       │                 └─────► notes_map ───┘
       │
       ├─────► track_detail/{id}
       │
       └─────► import_track
```

### Маршруты навигации

| Маршрут | Параметры | Экран |
|---------|-----------|-------|
| `main` | - | MainScreen |
| `tracks` | - | TracksListScreen |
| `track_detail/{trackId}` | trackId: String | TrackDetailScreen |
| `import_track` | - | ImportTrackScreen |
| `settings` | - | SettingsScreen |
| `error_logs` | - | ErrorLogScreen |
| `notes_list` | - | NotesListScreen |
| `notes_map` | - | NotesMapScreen |
| `notes_map_center` | - | NotesMapScreen (centered) |
| `notes_map_note/{noteId}` | noteId: String | NotesMapScreen (centered on note) |
| `note_detail/{noteId}` | noteId: String | NoteDetailScreen |
| `add_note/{lat}/{lng}` | lat: Double, lng: Double | AddNoteScreen |

### Навигационные действия

```kotlin
// Переход к списку треков
navController.navigate("tracks")

// Переход к деталям трека
navController.navigate("track_detail/$trackId")

// Переход к добавлению заметки
navController.navigate("add_note/$latitude/$longitude")

// Возврат назад
navController.popBackStack()

// Возврат на главный экран
navController.popBackStack("main", inclusive = false)
```

---

## Сервисы и фоновая работа

### LocationTrackingService

**Жизненный цикл:**

```
onCreate()
   ├─► Создание NotificationChannel
   ├─► Инициализация FusedLocationProviderClient
   ├─► Настройка LocationCallback
   └─► Запуск мониторинга сети

onStartCommand(intent)
   ├─► startForeground() - обязательно в течение 5 сек
   └─► ACTION_START_RECORDING
       ├─► Создание нового трека
       ├─► Запрос GPS-обновлений
       └─► Обновление уведомления
   
   └─► ACTION_STOP_RECORDING
       ├─► Остановка GPS-обновлений
       ├─► Завершение трека
       ├─► Генерация GPX/GeoJSON
       └─► stopForeground()

onDestroy()
   ├─► Остановка GPS-обновлений
   ├─► Остановка мониторинга сети
   └─► Очистка ресурсов
```

**LocationCallback:**

```kotlin
override fun onLocationResult(result: LocationResult) {
    for (location in result.locations) {
        // 1. Проверка точности
        if (location.accuracy > accuracyThreshold) continue
        
        // 2. Расчет расстояния от последней точки
        val distance = calculateDistance(lastLocation, location)
        
        // 3. Фильтрация по расстоянию
        if (distance < minDistance) continue
        
        // 4. Создание TrackPoint
        val point = TrackPoint(
            id = UUID.randomUUID().toString(),
            trackId = currentTrack.id,
            timestamp = location.time,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            speed = location.speed,
            bearing = location.bearing,
            accuracy = location.accuracy
        )
        
        // 5. Сохранение
        if (isNetworkAvailable()) {
            trackRepository.insertTrackPoint(point)
        } else {
            bufferedPointsRepository.insertBufferedPoint(point)
        }
        
        // 6. Обновление метрик
        updateTrackMetrics(distance, location)
        
        // 7. Обновление уведомления
        updateNotification()
        
        lastLocation = location
    }
}
```

**Foreground Notification:**

```kotlin
NotificationCompat.Builder(context, CHANNEL_ID)
    .setContentTitle("Запись трека")
    .setContentText("Расстояние: 5.2 км • Время: 01:15:30")
    .setSmallIcon(R.drawable.ic_location)
    .setOngoing(true)
    .setPriority(NotificationCompat.PRIORITY_LOW)
    .addAction(pauseAction)  // Пауза
    .addAction(stopAction)   // Стоп
    .setContentIntent(mainActivityIntent)
```

### BufferedPointsSyncService

**Логика синхронизации:**

```kotlin
class BufferedPointsSyncService @Inject constructor(
    private val bufferedPointsRepository: BufferedTrackPointRepository,
    private val trackRepository: TrackRepository,
    private val networkUtils: NetworkUtils
) {
    
    suspend fun syncBufferedPoints() {
        if (!networkUtils.isNetworkAvailable()) {
            return
        }
        
        val unsyncedPoints = bufferedPointsRepository.getUnsyncedPoints()
        
        for (point in unsyncedPoints) {
            try {
                // Конвертация в TrackPoint
                val trackPoint = TrackPoint(
                    id = point.id,
                    trackId = point.trackId,
                    timestamp = point.timestamp,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    altitude = point.altitude,
                    speed = point.speed,
                    bearing = point.bearing,
                    accuracy = point.accuracy
                )
                
                // Сохранение в основную БД
                trackRepository.insertTrackPoint(trackPoint)
                
                // Пометка как синхронизированная
                bufferedPointsRepository.markAsSynced(point.id)
                
            } catch (e: Exception) {
                ErrorLogger.logError(context, e, "Failed to sync point ${point.id}")
            }
        }
        
        // Обновление метрик треков
        updateTrackMetrics()
    }
}
```

**Мониторинг сети:**

```kotlin
private fun startNetworkMonitoring() {
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            lifecycleScope.launch {
                bufferedPointsSyncService.syncBufferedPoints()
            }
        }
    }
    
    val connectivityManager = getSystemService(ConnectivityManager::class.java)
    connectivityManager.registerDefaultNetworkCallback(networkCallback)
}
```

---

## API и интеграции

### Yandex MapKit

**Инициализация:**

```kotlin
// В XTrackApplication.onCreate()
MapKitFactory.setApiKey(apiKey)
MapKitFactory.initialize(this)
```

**Использование в Compose:**

```kotlin
@Composable
fun MapView(
    modifier: Modifier = Modifier,
    currentLocation: Point?,
    trackPoints: List<TrackPoint>,
    mapNotes: List<MapNote>,
    onMapClick: (Point) -> Unit = {},
    onMapLongClick: (Point) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                map.apply {
                    // Настройка карты
                    isRotateGesturesEnabled = true
                    isScrollGesturesEnabled = true
                    isZoomGesturesEnabled = true
                    isTiltGesturesEnabled = true
                    
                    // Обработчики событий
                    addInputListener(object : InputListener {
                        override fun onMapTap(map: Map, point: Point) {
                            onMapClick(point)
                        }
                        
                        override fun onMapLongTap(map: Map, point: Point) {
                            onMapLongClick(point)
                        }
                    })
                }
            }
        },
        update = { mapView ->
            // Обновление маршрута
            updatePolyline(mapView, trackPoints)
            
            // Обновление маркеров заметок
            updateMarkers(mapView, mapNotes)
            
            // Центрирование на текущей позиции
            currentLocation?.let {
                mapView.map.move(
                    CameraPosition(it, 16.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1.0f),
                    null
                )
            }
        }
    )
}
```

**Отображение маршрута:**

```kotlin
private fun updatePolyline(mapView: MapView, points: List<TrackPoint>) {
    val polylinePoints = points.map { Point(it.latitude, it.longitude) }
    
    val polyline = Polyline(polylinePoints)
    
    val polylineOptions = PolylineMapObject().apply {
        geometry = polyline
        strokeColor = Color.BLUE
        strokeWidth = 5.0f
        outlineColor = Color.WHITE
        outlineWidth = 1.0f
    }
    
    mapView.map.mapObjects.addPolyline(polylineOptions)
}
```

**Маркеры заметок:**

```kotlin
private fun updateMarkers(mapView: MapView, notes: List<MapNote>) {
    notes.forEach { note ->
        val marker = mapView.map.mapObjects.addPlacemark(
            Point(note.latitude, note.longitude)
        ).apply {
            // Иконка в зависимости от типа
            setIcon(ImageProvider.fromResource(context, getIconForNoteType(note.noteType)))
            
            // Обработчик клика
            addTapListener { _, _ ->
                onNoteClick(note)
                true
            }
            
            // Текст на маркере
            setText(note.title)
        }
    }
}
```

### Google Play Services Location

**Настройка LocationRequest:**

```kotlin
val locationRequest = LocationRequest.create().apply {
    interval = settings.locationIntervalMs
    fastestInterval = settings.locationIntervalMs / 2
    priority = when (settings.locationAccuracy) {
        LocationAccuracy.HIGH_ACCURACY -> LocationRequest.PRIORITY_HIGH_ACCURACY
        LocationAccuracy.BALANCED -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        LocationAccuracy.LOW_POWER -> LocationRequest.PRIORITY_LOW_POWER
    }
    maxWaitTime = settings.locationIntervalMs * 2
}
```

**Запрос обновлений:**

```kotlin
if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
    == PackageManager.PERMISSION_GRANTED) {
    
    fusedLocationClient.requestLocationUpdates(
        locationRequest,
        locationCallback,
        Looper.getMainLooper()
    )
}
```

### Room Database

**TypeConverters:**

```kotlin
class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()
    
    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let { Instant.parse(it) }
    
    @TypeConverter
    fun fromLocationAccuracy(value: LocationAccuracy): String = value.name
    
    @TypeConverter
    fun toLocationAccuracy(value: String): LocationAccuracy = 
        LocationAccuracy.valueOf(value)
    
    @TypeConverter
    fun fromExportFormat(value: ExportFormat): String = value.name
    
    @TypeConverter
    fun toExportFormat(value: String): ExportFormat = 
        ExportFormat.valueOf(value)
    
    @TypeConverter
    fun fromNoteType(value: NoteType): String = value.name
    
    @TypeConverter
    fun toNoteType(value: String): NoteType = 
        NoteType.valueOf(value)
    
    @TypeConverter
    fun fromMediaType(value: MediaType?): String? = value?.name
    
    @TypeConverter
    fun toMediaType(value: String?): MediaType? = 
        value?.let { MediaType.valueOf(it) }
}
```

---

## Управление состоянием

### StateFlow и SharedFlow

**Паттерн использования в ViewModel:**

```kotlin
class MainViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    // Состояние UI (StateFlow)
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    // Одноразовые события (SharedFlow)
    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()
    
    // Данные из репозитория (StateFlow)
    val currentTrack: StateFlow<Track?> = trackRepository
        .getActiveTrack()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    // Функции управления состоянием
    fun startRecording() {
        viewModelScope.launch {
            try {
                val track = createNewTrack()
                trackRepository.insertTrack(track)
                startLocationService()
                _events.emit(MainEvent.RecordingStarted)
            } catch (e: Exception) {
                _events.emit(MainEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }
}
```

**UI State:**

```kotlin
data class MainUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val currentDistance: Float = 0f,
    val currentDuration: Long = 0L,
    val currentSpeed: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

**Events:**

```kotlin
sealed class MainEvent {
    object RecordingStarted : MainEvent()
    object RecordingStopped : MainEvent()
    object RecordingPaused : MainEvent()
    object RecordingResumed : MainEvent()
    data class Error(val message: String) : MainEvent()
    data class Success(val message: String) : MainEvent()
}
```

### Использование в Compose

```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    
    // Обработка событий
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.RecordingStarted -> {
                    // Показать уведомление
                }
                is MainEvent.Error -> {
                    // Показать ошибку
                }
                // ...
            }
        }
    }
    
    // UI
    Column {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        }
        
        currentTrack?.let { track ->
            TrackInfo(track)
        }
        
        Button(onClick = { viewModel.startRecording() }) {
            Text(if (uiState.isRecording) "Стоп" else "Старт")
        }
    }
}
```

---

## Работа с файлами

### FileProvider

**Конфигурация (file_paths.xml):**

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Треки (GPX/GeoJSON) -->
    <files-path name="tracks" path="tracks/" />
    
    <!-- Логи ошибок -->
    <files-path name="logs" path="logs/" />
    
    <!-- Медиа заметок (фото/видео) -->
    <files-path name="media" path="media/" />
    
    <!-- Экспорт данных -->
    <external-files-path name="export" path="export/" />
</paths>
```

### Экспорт GPX

```kotlin
suspend fun exportTrackAsGpx(track: Track): Uri {
    val points = trackRepository.getTrackPoints(track.id).first()
    val gpxContent = GpxGenerator.generate(track, points)
    
    val file = File(context.filesDir, "tracks/${track.id}.gpx")
    file.parentFile?.mkdirs()
    file.writeText(gpxContent)
    
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
```

### Импорт GPX

```kotlin
suspend fun importTrackFromGpx(uri: Uri): Track {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Cannot open file")
    
    val gpxContent = inputStream.bufferedReader().use { it.readText() }
    val gpxTrack = GpxParser.parse(gpxContent)
    
    // Конвертация в Track и TrackPoint
    val track = Track(
        id = UUID.randomUUID().toString(),
        name = gpxTrack.name ?: "Imported Track",
        startedAt = gpxTrack.startTime,
        endedAt = gpxTrack.endTime,
        distanceMeters = gpxTrack.distance,
        durationSec = gpxTrack.duration,
        elevationGainMeters = gpxTrack.elevationGain,
        gpxPath = null,
        geojsonPath = null,
        isRecording = false
    )
    
    trackRepository.insertTrack(track)
    
    gpxTrack.points.forEach { point ->
        trackRepository.insertTrackPoint(
            TrackPoint(
                id = UUID.randomUUID().toString(),
                trackId = track.id,
                timestamp = point.timestamp,
                latitude = point.latitude,
                longitude = point.longitude,
                altitude = point.altitude,
                speed = null,
                bearing = null,
                accuracy = 0f
            )
        )
    }
    
    return track
}
```

### Сохранение медиа заметок

```kotlin
suspend fun saveNoteMedia(uri: Uri, noteId: String, mediaType: MediaType): String {
    val extension = when (mediaType) {
        MediaType.PHOTO -> "jpg"
        MediaType.VIDEO -> "mp4"
    }
    
    val fileName = "${noteId}_${System.currentTimeMillis()}.$extension"
    val file = File(context.filesDir, "media/$fileName")
    file.parentFile?.mkdirs()
    
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    
    return file.absolutePath
}
```

---

## Настройка и конфигурация

### Разрешения (AndroidManifest.xml)

```xml
<!-- Обязательные -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />

<!-- Android 10+ -->
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<!-- Android 13+ -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Опциональные -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### Gradle конфигурация

**build.gradle.kts (app):**

```kotlin
android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.xtrack"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        // NDK фильтры для Yandex MapKit
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kapt {
    correctErrorTypes = true
}
```

**gradle.properties:**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
kotlin.code.style=official
```

### ProGuard Rules

**proguard-rules.pro:**

```proguard
# Yandex MapKit
-keep class com.yandex.mapkit.** { *; }
-keep class com.yandex.runtime.** { *; }
-dontwarn com.yandex.mapkit.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.xtrack.data.model.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
```

---

## Тестирование

### Unit Tests

**TrackRepositoryTest:**

```kotlin
@RunWith(MockitoJUnitRunner::class)
class TrackRepositoryTest {
    
    @Mock
    private lateinit var trackDao: TrackDao
    
    @Mock
    private lateinit var trackPointDao: TrackPointDao
    
    private lateinit var repository: TrackRepository
    
    @Before
    fun setup() {
        repository = TrackRepository(trackDao, trackPointDao)
    }
    
    @Test
    fun `getAllTracks returns flow of tracks`() = runTest {
        val tracks = listOf(
            Track(
                id = "1",
                name = "Test Track",
                startedAt = Clock.System.now(),
                endedAt = null,
                distanceMeters = 0.0,
                durationSec = 0,
                elevationGainMeters = 0.0,
                gpxPath = null,
                geojsonPath = null,
                isRecording = true
            )
        )
        
        `when`(trackDao.getAllTracks()).thenReturn(flowOf(tracks))
        
        val result = repository.getAllTracks().first()
        
        assertEquals(tracks, result)
        verify(trackDao).getAllTracks()
    }
}
```

**LocationUtilsTest:**

```kotlin
class LocationUtilsTest {
    
    @Test
    fun `calculateDistance returns correct distance`() {
        // Москва - Санкт-Петербург примерно 634 км
        val moscowLat = 55.751244
        val moscowLng = 37.618423
        val spbLat = 59.934280
        val spbLng = 30.335098
        
        val distance = LocationUtils.calculateDistance(
            moscowLat, moscowLng, spbLat, spbLng
        )
        
        // Допуск ±10 км
        assertTrue(distance in 624000f..644000f)
    }
    
    @Test
    fun `formatDistance formats correctly`() {
        assertEquals("0 м", LocationUtils.formatDistance(0f))
        assertEquals("50 м", LocationUtils.formatDistance(50f))
        assertEquals("1.0 км", LocationUtils.formatDistance(1000f))
        assertEquals("5.4 км", LocationUtils.formatDistance(5432f))
    }
}
```

### UI Tests (Espresso + Compose)

**MainScreenTest:**

```kotlin
@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun startButton_isDisplayed() {
        composeTestRule.setContent {
            MainScreen(
                viewModel = FakeMainViewModel(),
                onNavigateToTracks = {},
                onNavigateToSettings = {}
            )
        }
        
        composeTestRule
            .onNodeWithText("Старт")
            .assertIsDisplayed()
    }
    
    @Test
    fun clickStartButton_startsRecording() {
        val viewModel = FakeMainViewModel()
        
        composeTestRule.setContent {
            MainScreen(
                viewModel = viewModel,
                onNavigateToTracks = {},
                onNavigateToSettings = {}
            )
        }
        
        composeTestRule
            .onNodeWithText("Старт")
            .performClick()
        
        assertTrue(viewModel.isRecordingCalled)
    }
}
```

---

## Заключение

**XTrack** - это полнофункциональное Android приложение для отслеживания GPS-треков, построенное с использованием современных технологий и best practices Android разработки.

### Ключевые особенности архитектуры:

1. **Clean Architecture** - четкое разделение слоев (Data, Domain, Presentation)
2. **MVVM паттерн** - разделение бизнес-логики и UI
3. **Dependency Injection (Hilt)** - управление зависимостями
4. **Room Database** - надежное локальное хранилище
5. **Jetpack Compose** - современный декларативный UI
6. **Coroutines & Flow** - реактивное управление данными
7. **Foreground Services** - надежная фоновая работа

### Производительность и оптимизация:

- Буферизация точек при отсутствии сети
- Фильтрация GPS-точек по точности и расстоянию
- Оптимизированные запросы к БД с индексами
- Ленивая загрузка данных с Paging
- Rate-limited логирование

### Безопасность и надежность:

- Глобальный обработчик исключений
- Детальное логирование ошибок
- Миграции БД без потери данных
- Валидация импортируемых данных
- Корректная обработка разрешений

### Расширяемость:

- Модульная архитектура
- Dependency Injection для подмены реализаций
- Интерфейсы для парсеров и репозиториев
- Гибкая система настроек

---

**Версия документации:** 1.0  
**Дата:** 10 октября 2024  
**Автор:** XTrack Development Team


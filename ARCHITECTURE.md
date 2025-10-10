# 🏗️ Архитектура XTrack

Детальное описание архитектуры приложения с диаграммами и схемами.

---

## 📐 Общая архитектура

### Clean Architecture + MVVM

```
┌───────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                       │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐      │
│  │   Screens   │───►│  ViewModels │◄──►│    Theme    │      │
│  │  (Compose)  │    │   (State)   │    │  (Material) │      │
│  └──────┬──────┘    └──────┬──────┘    └─────────────┘      │
│         │                  │                                  │
│         └──────────────────┘                                  │
│                   ▲                                           │
└───────────────────┼───────────────────────────────────────────┘
                    │
                    │ Events / State
                    │
┌───────────────────┼───────────────────────────────────────────┐
│                   ▼         DOMAIN LAYER                      │
│         ┌──────────────────┐                                  │
│         │   Repositories   │◄─────────────┐                  │
│         │   (Interfaces)   │              │                  │
│         └─────────┬────────┘              │                  │
│                   │                       │                  │
│         ┌─────────▼────────┐              │                  │
│         │    Use Cases     │              │                  │
│         │  (Business Logic)│              │                  │
│         └──────────────────┘              │                  │
└───────────────────┬───────────────────────┼───────────────────┘
                    │                       │
                    │ Data Flow             │
                    │                       │
┌───────────────────▼───────────────────────┼───────────────────┐
│                    DATA LAYER             │                   │
│  ┌──────────────┐  ┌──────────────┐  ┌───┴──────────┐       │
│  │   Database   │  │   Parsers    │  │  Services    │       │
│  │    (Room)    │  │ (GPX/JSON)   │  │  (Location)  │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                 │                  │                │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐       │
│  │     DAO      │  │  Generators  │  │   Sensors    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└───────────────────────────────────────────────────────────────┘
```

---

## 🔄 Поток данных

### 1. Запись GPS-трека

```
┌─────────────┐
│    User     │
│  (UI Layer) │
└──────┬──────┘
       │ 1. Click "Start"
       ▼
┌─────────────────┐
│  MainViewModel  │
└──────┬──────────┘
       │ 2. startRecording()
       ▼
┌──────────────────────┐
│  TrackRepository     │
│  - insertTrack()     │
└──────┬───────────────┘
       │ 3. Create Track
       ▼
┌──────────────────────┐
│   TrackDao (Room)    │
│   - insert()         │
└──────┬───────────────┘
       │ 4. Save to DB
       ▼
┌──────────────────────┐
│   SQLite Database    │
└──────────────────────┘

       │ 5. Start Service
       ▼
┌──────────────────────────────┐
│ LocationTrackingService      │
│ - startForeground()          │
│ - requestLocationUpdates()   │
└──────┬───────────────────────┘
       │ 6. Request updates
       ▼
┌──────────────────────────────┐
│ FusedLocationProvider (GPS)  │
└──────┬───────────────────────┘
       │ 7. GPS Location
       ▼
┌──────────────────────────────┐
│ LocationCallback             │
│ - onLocationResult()         │
│   ├─► Filter by accuracy     │
│   ├─► Filter by distance     │
│   └─► Calculate metrics      │
└──────┬───────────────────────┘
       │ 8. Valid point
       ▼
┌──────────────────────────────┐
│ Network Check                │
└──────┬───────────────────────┘
       │
       ├─► Yes (network available)
       │   ▼
       │   ┌──────────────────────┐
       │   │ TrackPointDao        │
       │   │ - insert()           │
       │   └──────────────────────┘
       │
       └─► No (offline)
           ▼
           ┌──────────────────────────┐
           │ BufferedTrackPointDao    │
           │ - insertBuffered()       │
           └──────────────────────────┘

       │ 9. Update UI
       ▼
┌──────────────────────────────┐
│ Flow<Track>                  │
│ - collectAsState()           │
└──────┬───────────────────────┘
       │ 10. Recompose
       ▼
┌──────────────────────────────┐
│ MainScreen (Compose)         │
│ - Updated metrics            │
│ - Updated polyline on map    │
└──────────────────────────────┘
```

### 2. Синхронизация буферизованных точек

```
┌──────────────────────────────┐
│ NetworkCallback              │
│ - onAvailable()              │
└──────┬───────────────────────┘
       │ 1. Network restored
       ▼
┌──────────────────────────────┐
│ BufferedPointsSyncService    │
│ - syncBufferedPoints()       │
└──────┬───────────────────────┘
       │ 2. Get unsynced
       ▼
┌──────────────────────────────┐
│ BufferedTrackPointRepository │
│ - getUnsyncedPoints()        │
└──────┬───────────────────────┘
       │ 3. List<BufferedPoint>
       ▼
┌──────────────────────────────┐
│ For each buffered point      │
│   ├─► Convert to TrackPoint  │
│   ├─► Insert to track_points │
│   └─► Mark as synced         │
└──────┬───────────────────────┘
       │ 4. Update metrics
       ▼
┌──────────────────────────────┐
│ TrackRepository              │
│ - updateTrackMetrics()       │
└──────────────────────────────┘
```

### 3. Экспорт трека в GPX

```
┌─────────────────┐
│  User           │
│  - Click Export │
└──────┬──────────┘
       │ 1. exportTrack()
       ▼
┌──────────────────────────────┐
│ TrackDetailViewModel         │
│ - exportTrack(GPX)           │
└──────┬───────────────────────┘
       │ 2. Get data
       ▼
┌──────────────────────────────┐
│ TrackRepository              │
│ - getTrackById()             │
│ - getTrackPoints()           │
└──────┬───────────────────────┘
       │ 3. Track + Points
       ▼
┌──────────────────────────────┐
│ GpxGenerator                 │
│ - generate()                 │
│   ├─► Create metadata        │
│   ├─► Create track segment   │
│   └─► Add trackpoints        │
└──────┬───────────────────────┘
       │ 4. GPX XML String
       ▼
┌──────────────────────────────┐
│ File I/O                     │
│ - Write to file              │
└──────┬───────────────────────┘
       │ 5. File created
       ▼
┌──────────────────────────────┐
│ FileProvider                 │
│ - getUriForFile()            │
└──────┬───────────────────────┘
       │ 6. Content URI
       ▼
┌──────────────────────────────┐
│ Share Intent                 │
│ - ACTION_SEND                │
└──────────────────────────────┘
```

---

## 🗃️ Database Architecture

### Entity Relationships

```
┌─────────────────────────────────────────────────────────────┐
│                        DATABASE SCHEMA                       │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────┐
│      tracks         │
│─────────────────────│
│ PK  id: String      │
│     name: String    │
│     startedAt       │
│     endedAt         │
│     distanceMeters  │
│     durationSec     │
│     elevationGain   │
│     gpxPath         │
│     geojsonPath     │
│     isRecording     │
└──────────┬──────────┘
           │
           │ 1:N
           │
    ┌──────┴─────────────────────────────┬──────────────────────┐
    │                                    │                      │
    ▼                                    ▼                      ▼
┌─────────────────────┐    ┌──────────────────────┐  ┌─────────────────────┐
│   track_points      │    │     map_notes        │  │ buffered_points     │
│─────────────────────│    │──────────────────────│  │─────────────────────│
│ PK  id: String      │    │ PK  id: String       │  │ PK  id: String      │
│ FK  trackId         │    │ FK  trackId          │  │ FK  trackId         │
│     timestamp       │    │     latitude         │  │     timestamp       │
│     latitude        │    │     longitude        │  │     latitude        │
│     longitude       │    │     title            │  │     longitude       │
│     altitude        │    │     description      │  │     altitude        │
│     speed           │    │     timestamp        │  │     speed           │
│     bearing         │    │     noteType         │  │     bearing         │
│     accuracy        │    │     mediaPath        │  │     accuracy        │
└─────────────────────┘    │     mediaType        │  │     isSynced        │
                           └──────────────────────┘  │     createdAt       │
                                                     └─────────────────────┘

┌─────────────────────┐    ┌──────────────────────┐
│   app_settings      │    │   last_location      │
│─────────────────────│    │──────────────────────│
│ PK  id: Int (1)     │    │ PK  id: Int (1)      │
│     locationAccuracy│    │     latitude         │
│     intervalMs      │    │     longitude        │
│     minDistance     │    │     timestamp        │
│     accuracyThresh. │    │     accuracy         │
│     autoPauseEnabl. │    └──────────────────────┘
│     autoPauseSpeed  │
│     autoPauseDurat. │    Singleton tables
│     exportFormat    │    (always 1 row)
│     appExitState    │
│     distNotifInterv.│
│     distNotifEnabl. │
└─────────────────────┘
```

### Database Migrations Timeline

```
v1 (Initial)
  ├─ tracks
  ├─ track_points
  └─ app_settings

v2 (Map Notes)
  ├─ Added: map_notes table
  └─ Purpose: Store user notes on map

v3 (Last Location)
  ├─ Added: last_location table
  └─ Purpose: Remember last known position

v4 (Exit State)
  ├─ Added: wasRecordingOnExit field
  └─ Purpose: Restore recording state on app restart

v5 (Buffering)
  ├─ Added: buffered_track_points table
  └─ Purpose: Offline point buffering

v6 (Distance Notifications)
  ├─ Added: distanceNotificationIntervalMeters
  ├─ Added: distanceNotificationsEnabled
  └─ Purpose: Periodic distance notifications

v7 (Data Integrity)
  ├─ Fixed: app_settings table corruption
  └─ Purpose: Data integrity improvements

v8 (Elevation)
  ├─ Added: elevationGainMeters field
  └─ Purpose: Track elevation gain

v9 (Schema Update)
  └─ Purpose: Internal schema hash update

v10 (Type Converters)
  ├─ Added: TypeConverters for enums
  └─ Purpose: Proper enum serialization

v11 (Exit State Enum) ← Current
  ├─ Changed: wasRecordingOnExit → appExitState
  └─ Purpose: Better state management
```

---

## 🔌 Dependency Injection (Hilt)

### Component Hierarchy

```
┌────────────────────────────────────────┐
│      SingletonComponent                │
│  (Application Lifecycle)               │
│  ┌──────────────────────────────────┐ │
│  │ DatabaseModule                   │ │
│  │ - TrackingDatabase               │ │
│  │ - All DAOs                       │ │
│  └──────────────────────────────────┘ │
│  ┌──────────────────────────────────┐ │
│  │ ParserModule                     │ │
│  │ - GpxParser                      │ │
│  │ - GpxGenerator                   │ │
│  │ - GeoJsonParser                  │ │
│  │ - GeoJsonGenerator               │ │
│  └──────────────────────────────────┘ │
│  ┌──────────────────────────────────┐ │
│  │ Repositories                     │ │
│  │ - TrackRepository                │ │
│  │ - SettingsRepository             │ │
│  │ - MapNoteRepository              │ │
│  └──────────────────────────────────┘ │
└────────────────┬───────────────────────┘
                 │
    ┌────────────┴────────────┐
    │                         │
    ▼                         ▼
┌─────────────────┐   ┌──────────────────┐
│ ViewModelComp.  │   │ ServiceComponent │
│  (ViewModel)    │   │  (Service)       │
│  - ViewModels   │   │  - Services      │
└─────────────────┘   └──────────────────┘
```

### Injection Graph

```
@HiltAndroidApp
XTrackApplication
    │
    ├─► @AndroidEntryPoint
    │   MainActivity
    │       │
    │       └─► @HiltViewModel
    │           ViewModels (injected)
    │               │
    │               └─► Repositories (injected)
    │                       │
    │                       └─► DAOs (injected)
    │
    └─► @AndroidEntryPoint
        LocationTrackingService
            │
            └─► Repositories (injected)
                    │
                    └─► DAOs (injected)
```

---

## 🎭 State Management

### ViewModel State Flow

```
┌───────────────────────────────────────────────────────────────┐
│                         ViewModel                             │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐     │
│  │              Private State                          │     │
│  │  _uiState: MutableStateFlow<UiState>               │     │
│  │  _events: MutableSharedFlow<Event>                 │     │
│  └────────────────────┬────────────────────────────────┘     │
│                       │ expose as                             │
│                       ▼                                       │
│  ┌─────────────────────────────────────────────────────┐     │
│  │              Public State                           │     │
│  │  uiState: StateFlow<UiState>                       │     │
│  │  events: SharedFlow<Event>                         │     │
│  └────────────────────┬────────────────────────────────┘     │
└───────────────────────┼───────────────────────────────────────┘
                        │
                        │ collect in Compose
                        ▼
┌───────────────────────────────────────────────────────────────┐
│                        Composable                             │
│                                                               │
│  val uiState by viewModel.uiState.collectAsState()           │
│                                                               │
│  LaunchedEffect(Unit) {                                       │
│      viewModel.events.collect { event ->                      │
│          when (event) {                                       │
│              is Event.ShowSnackbar -> ...                     │
│              is Event.Navigate -> ...                         │
│          }                                                    │
│      }                                                        │
│  }                                                            │
│                                                               │
│  // UI based on uiState                                       │
│  if (uiState.isLoading) { ... }                              │
└───────────────────────────────────────────────────────────────┘
```

### State Hoisting Pattern

```
┌─────────────────────────────────────────────────────────────┐
│                      Screen (Stateful)                       │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ ViewModel                                             │  │
│  │ - State management                                    │  │
│  │ - Business logic                                      │  │
│  └─────────────┬─────────────────────────────────────────┘  │
│                │ provides                                    │
│                ▼                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Screen Composable                                     │  │
│  │ - Observes state                                      │  │
│  │ - Delegates events to ViewModel                       │  │
│  └─────────────┬─────────────────────────────────────────┘  │
└────────────────┼─────────────────────────────────────────────┘
                 │ delegates to
                 ▼
┌─────────────────────────────────────────────────────────────┐
│              Content Composable (Stateless)                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ @Composable                                           │  │
│  │ fun Content(                                          │  │
│  │     state: UiState,                                   │  │
│  │     onEvent: (Event) -> Unit                          │  │
│  │ ) {                                                   │  │
│  │     // Pure UI based on state                         │  │
│  │     // Calls onEvent for user actions                 │  │
│  │ }                                                     │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  Benefits:                                                   │
│  ✓ Testable (pure function)                                 │
│  ✓ Reusable                                                 │
│  ✓ Preview-able                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗺️ Navigation Architecture

### Navigation Graph Structure

```
NavHost (startDestination = "main")
    │
    ├─► main (MainScreen)
    │   ├─► Navigate to: tracks, settings, add_note
    │   └─► Features: Map, Recording, Metrics
    │
    ├─► tracks (TracksListScreen)
    │   ├─► Navigate to: track_detail, import_track
    │   └─► Features: List, Search, Delete, Export
    │
    ├─► track_detail/{trackId} (TrackDetailScreen)
    │   ├─► Navigate to: [popBackStack]
    │   └─► Features: Map, Statistics, Export, Delete
    │
    ├─► import_track (ImportTrackScreen)
    │   ├─► Navigate to: [popBackStack]
    │   └─► Features: File picker, Preview, Import
    │
    ├─► settings (SettingsScreen)
    │   ├─► Navigate to: error_logs, notes_list, notes_map
    │   └─► Features: GPS settings, Export, Diagnostics
    │
    ├─► error_logs (ErrorLogScreen)
    │   ├─► Navigate to: [popBackStack]
    │   └─► Features: View logs, Export, Clear
    │
    ├─► notes_list (NotesListScreen)
    │   ├─► Navigate to: note_detail, notes_map_note
    │   └─► Features: List, Filter, Delete
    │
    ├─► notes_map (NotesMapScreen)
    │   ├─► Navigate to: [popBackStack]
    │   └─► Features: Map with notes, Cluster
    │
    ├─► notes_map_note/{noteId} (NotesMapScreen)
    │   ├─► Navigate to: [popBackStack]
    │   └─► Features: Map centered on note
    │
    ├─► note_detail/{noteId} (NoteDetailScreen)
    │   ├─► Navigate to: notes_map_note, [popBackStack]
    │   └─► Features: View, Edit, Delete, Map
    │
    └─► add_note/{lat}/{lng} (AddNoteScreen)
        ├─► Navigate to: [popBackStack]
        └─► Features: Text, Photo, Video
```

### Navigation Flow Example: View Track Details

```
User Flow:
┌─────────────┐
│ MainScreen  │
│  (main)     │
└──────┬──────┘
       │ Tap "View Tracks"
       ▼
┌─────────────────┐
│ TracksListScreen│
│   (tracks)      │
└──────┬──────────┘
       │ Tap Track Item
       │ navController.navigate("track_detail/$trackId")
       ▼
┌─────────────────────┐
│ TrackDetailScreen   │
│ (track_detail/{id}) │
│                     │
│ - Extracts trackId  │
│   from arguments    │
│ - ViewModel loads   │
│   track data        │
│ - Shows map + stats │
└──────┬──────────────┘
       │ Tap Back
       │ navController.popBackStack()
       ▼
┌─────────────────┐
│ TracksListScreen│
│   (restored)    │
└─────────────────┘
```

---

## 🔔 Service Architecture

### LocationTrackingService Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│                    Service Lifecycle                         │
└─────────────────────────────────────────────────────────────┘

onCreate()
    │
    ├─► createNotificationChannel()
    ├─► initialize FusedLocationProviderClient
    ├─► setup LocationCallback
    └─► startNetworkMonitoring()

onStartCommand(intent, flags, startId)
    │
    ├─► startForeground(NOTIFICATION_ID, notification)
    │   └─► (MUST be called within 5 seconds)
    │
    └─► when (intent.action)
        │
        ├─► ACTION_START_RECORDING
        │   ├─► load settings
        │   ├─► requestLocationUpdates()
        │   └─► update notification
        │
        ├─► ACTION_STOP_RECORDING
        │   ├─► removeLocationUpdates()
        │   ├─► finalize track
        │   ├─► generate GPX/GeoJSON (optional)
        │   ├─► stopForeground()
        │   └─► stopSelf()
        │
        ├─► ACTION_PAUSE_RECORDING
        │   ├─► removeLocationUpdates()
        │   └─► update notification
        │
        ├─► ACTION_RESUME_RECORDING
        │   ├─► requestLocationUpdates()
        │   └─► update notification
        │
        └─► ACTION_EMERGENCY_STOP
            ├─► removeLocationUpdates()
            ├─► log error
            └─► stopSelf()

onDestroy()
    │
    ├─► removeLocationUpdates()
    ├─► stopNetworkMonitoring()
    └─► cleanup resources

return START_STICKY
```

### Location Update Flow

```
GPS Sensor
    │
    ▼
FusedLocationProviderClient
    │ location updates every N ms
    ▼
LocationCallback.onLocationResult()
    │
    ├─► for each location:
    │   │
    │   ├─► [Filter 1] accuracy < threshold?
    │   │   │  No → discard
    │   │   │  Yes ↓
    │   │
    │   ├─► [Filter 2] distance >= minDistance?
    │   │   │  No → discard
    │   │   │  Yes ↓
    │   │
    │   ├─► [Filter 3] time >= interval?
    │   │   │  No → discard
    │   │   │  Yes ↓
    │   │
    │   ├─► Create TrackPoint
    │   │
    │   ├─► Check network
    │   │   ├─► Online → save to track_points
    │   │   └─► Offline → save to buffered_track_points
    │   │
    │   ├─► Update track metrics
    │   │   ├─► totalDistance += calculateDistance()
    │   │   ├─► elevationGain += calculateElevation()
    │   │   └─► duration = now - startTime
    │   │
    │   └─► Update notification
    │       └─► show current metrics
    │
    └─► [Auto-Pause Check]
        ├─► speed < autoPauseThreshold?
        │   └─► wait autoPauseDuration
        │       └─► still slow? → pause()
        └─► speed >= autoPauseThreshold?
            └─► was paused? → resume()
```

### Foreground Service Notification

```
┌───────────────────────────────────────────────────────────┐
│                 🗺️ Запись трека                           │
│                                                           │
│  📍 Расстояние: 5.2 км                                    │
│  ⏱️  Время: 01:15:30                                      │
│  🚀 Скорость: 12.5 км/ч                                   │
│                                                           │
│  [⏸️ Пауза]  [⏹️ Стоп]                                     │
└───────────────────────────────────────────────────────────┘

States:
├─► Recording (ongoing = true, priority = LOW)
├─► Paused (ongoing = true, priority = MIN)
└─► Stopped (notification removed)

Updates:
├─► Every 5 seconds (while recording)
├─► Every 1 second (during auto-pause countdown)
└─► On distance milestones (if enabled)
```

---

## 📱 UI Component Hierarchy

### MainScreen Component Tree

```
MainScreen (@Composable)
│
├─► Scaffold
│   ├─► TopAppBar
│   │   ├─► Title: "XTrack"
│   │   └─► Actions
│   │       ├─► IconButton (Settings)
│   │       └─► IconButton (Tracks)
│   │
│   └─► Content
│       │
│       ├─► MapView (Full screen)
│       │   ├─► Yandex MapKit
│       │   ├─► Polyline (current track)
│       │   ├─► Markers (notes)
│       │   └─► User location marker
│       │
│       ├─► BottomSheet (Metrics)
│       │   ├─► StatisticsCard (Distance)
│       │   ├─► StatisticsCard (Time)
│       │   ├─► StatisticsCard (Speed)
│       │   └─► StatisticsCard (Elevation)
│       │
│       └─► FloatingActionButton
│           ├─► State: Not Recording → Green "Start"
│           ├─► State: Recording → Red "Stop"
│           └─► State: Paused → Yellow "Resume"
│
├─► if (showPermissionDialog)
│   └─► PermissionDialog
│
└─► if (showAddNoteDialog)
    └─► AddNoteDialog
```

### Recomposition Optimization

```
Remember & Derivation:

┌─────────────────────────────────────────────────────────┐
│ @Composable                                             │
│ fun MainScreen(viewModel: MainViewModel) {              │
│                                                         │
│   // ✓ Observe state                                   │
│   val uiState by viewModel.uiState.collectAsState()    │
│   val track by viewModel.currentTrack.collectAsState() │
│                                                         │
│   // ✓ Derived state (doesn't cause extra recomp)      │
│   val formattedDistance = remember(track) {            │
│       LocationUtils.formatDistance(track?.distance)    │
│   }                                                     │
│                                                         │
│   // ✓ Stable callbacks                                │
│   val onStartClick = remember {                        │
│       { viewModel.startRecording() }                   │
│   }                                                     │
│                                                         │
│   // UI...                                             │
│ }                                                       │
└─────────────────────────────────────────────────────────┘

Recomposition Scopes:

┌─────────────────────────────────────────────────────────┐
│ Column {                                                │
│   // Scope 1: Only recomposes when title changes       │
│   Text(title)                                           │
│                                                         │
│   // Scope 2: Only recomposes when distance changes    │
│   StatisticsCard(                                       │
│       title = "Distance",                              │
│       value = formattedDistance                        │
│   )                                                     │
│                                                         │
│   // Scope 3: Only recomposes when isRecording changes │
│   Button(                                              │
│       onClick = onStartClick,                          │
│       enabled = !isRecording                           │
│   ) {                                                   │
│       Text(if (isRecording) "Stop" else "Start")       │
│   }                                                     │
│ }                                                       │
└─────────────────────────────────────────────────────────┘
```

---

## 🔐 Security & Privacy

### Data Protection

```
┌─────────────────────────────────────────────────────────┐
│                    Data Storage                          │
└─────────────────────────────────────────────────────────┘

Internal Storage (app-private, encrypted on device)
├─► /data/data/com.xtrack/
    │
    ├─► databases/
    │   └─► tracking_database
    │       ├─► All track data
    │       ├─► Settings
    │       └─► Notes
    │
    ├─► files/
    │   ├─► tracks/ (GPX/GeoJSON)
    │   ├─► media/ (photos/videos)
    │   └─► logs/ (error logs)
    │
    └─► shared_prefs/
        └─► (none used - all in Room DB)

External Storage (user-accessible)
└─► /Android/data/com.xtrack/
    └─► files/export/ (exported files only)
```

### Permission Model

```
Runtime Permissions Flow:

┌─────────────────────────────────────────────────────────┐
│ App Launch                                              │
└────┬────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────┐
│ Check Location Permission                               │
│ ├─► GRANTED → Continue                                  │
│ └─► DENIED → Show rationale                             │
└────┬────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────┐
│ User Taps "Start Recording"                             │
└────┬────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────┐
│ Request Permissions Sequentially                        │
│ 1. ACCESS_FINE_LOCATION                                 │
│    ├─► GRANTED → Continue                               │
│    └─► DENIED → Show error                              │
│                                                         │
│ 2. ACCESS_BACKGROUND_LOCATION (Android 10+)             │
│    ├─► GRANTED → Full functionality                     │
│    └─► DENIED → Limited (foreground only)               │
│                                                         │
│ 3. POST_NOTIFICATIONS (Android 13+)                     │
│    ├─► GRANTED → Show notifications                     │
│    └─► DENIED → Silent mode                             │
└─────────────────────────────────────────────────────────┘
```

---

## ⚡ Performance Optimizations

### Database Optimizations

```
Indexes:
├─► tracks.startedAt (for sorting by date)
├─► tracks.isRecording (for finding active track)
├─► track_points.trackId (for JOIN queries)
├─► track_points.timestamp (for time-based queries)
├─► map_notes.trackId (for filtering by track)
├─► map_notes.noteType (for filtering by type)
└─► buffered_track_points.isSynced (for sync queries)

Query Optimization:
├─► Use Flow for reactive updates (no polling)
├─► Pagination for large lists (LazyColumn)
├─► Limit queries with LIMIT clause
└─► Batch inserts for multiple points

Transaction Batching:
┌───────────────────────────────────────────────┐
│ @Transaction                                  │
│ suspend fun insertTrackWithPoints(            │
│     track: Track,                             │
│     points: List<TrackPoint>                  │
│ ) {                                           │
│     trackDao.insert(track)                    │
│     points.chunked(100).forEach { chunk ->    │
│         trackPointDao.insertAll(chunk)        │
│     }                                         │
│ }                                             │
└───────────────────────────────────────────────┘
```

### Memory Management

```
Lifecycle-Aware Collection:

viewModelScope.launch {
    trackRepository.getActiveTrack()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            //       ↑ Stop collecting 5s after last subscriber
            initialValue = null
        )
}

Benefits:
✓ Stops DB queries when UI not visible
✓ Cancels coroutines automatically
✓ Prevents memory leaks
```

### Location Update Optimization

```
Adaptive Interval:

Moving Fast (> 5 m/s):
├─► Interval: 1 second
└─► High accuracy GPS

Moving Slow (1-5 m/s):
├─► Interval: 5 seconds
└─► Balanced accuracy

Stationary (< 1 m/s):
├─► Interval: 10 seconds (or auto-pause)
└─► Low power mode
```

---

## 🧪 Testing Strategy

### Testing Pyramid

```
                    ┌─────────┐
                    │   E2E   │  (5%)
                    │  Tests  │
                ┌───┴─────────┴───┐
                │  Integration    │  (15%)
                │     Tests       │
            ┌───┴─────────────────┴───┐
            │      Unit Tests         │  (80%)
            └─────────────────────────┘

Unit Tests (Fast, Isolated):
├─► ViewModels
├─► Repositories
├─► Utils (LocationUtils, etc.)
└─► Parsers (GPX, GeoJSON)

Integration Tests (Medium):
├─► Database (DAO tests)
├─► Repository + Database
└─► Service + Repository

E2E Tests (Slow, Full app):
├─► User flows
└─► UI tests (Compose)
```

---

**Версия:** 1.0  
**Последнее обновление:** 10 октября 2024


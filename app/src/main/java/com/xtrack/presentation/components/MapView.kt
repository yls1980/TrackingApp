package com.xtrack.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.xtrack.R
import com.xtrack.data.model.MapNote
import com.xtrack.data.model.Point
import com.xtrack.data.model.TrackPoint
import com.xtrack.utils.ErrorLogger
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point as YandexPoint
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView as YandexMapView
import com.yandex.runtime.image.ImageProvider
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.geometry.Polyline

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    trackPoints: List<TrackPoint>,
    currentLocation: Point?,
    onLocationUpdate: (Point) -> Unit,
    centerOnLocation: Boolean = false,
    centerOnTrack: Boolean = false,
    centerOnNote: MapNote? = null,
    onLongPress: ((Double, Double) -> Unit)? = null,
    notes: List<MapNote> = emptyList(),
    onNoteClick: ((MapNote) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Состояние для отслеживания ошибок MapKit
    var mapKitError by remember { mutableStateOf<String?>(null) }
    
    // Кэш для иконок, чтобы не создавать их каждый раз
    var locationIcon by remember {
        mutableStateOf<ImageProvider?>(null)
    }
    
    var noteIcon by remember {
        mutableStateOf<ImageProvider?>(null)
    }
    
    // Функция для создания иконки местоположения в фоновом потоке
    suspend fun createLocationIcon(): ImageProvider = withContext(Dispatchers.Default) {
        android.util.Log.d("MapView", "Creating location icon in background thread")
        val bitmap = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Рисуем золотой круг
        val backgroundPaint = android.graphics.Paint().apply {
            color = 0xFFFFD700.toInt() // Золотой цвет
            isAntiAlias = true
        }
        canvas.drawCircle(24f, 24f, 20f, backgroundPaint)
        
        // Рисуем белый круг внутри
        val innerPaint = android.graphics.Paint().apply {
            color = 0xFFFFFFFF.toInt() // Белый цвет
            isAntiAlias = true
        }
        canvas.drawCircle(24f, 24f, 16f, innerPaint)
        
        // Рисуем глаза
        val eyePaint = android.graphics.Paint().apply {
            color = 0xFF000000.toInt() // Черный цвет
            isAntiAlias = true
        }
        canvas.drawCircle(18f, 18f, 2f, eyePaint)
        canvas.drawCircle(30f, 18f, 2f, eyePaint)
        
        // Рисуем улыбку
        val smilePaint = android.graphics.Paint().apply {
            color = 0xFF000000.toInt() // Черный цвет
            strokeWidth = 2f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = android.graphics.Paint.Cap.ROUND
        }
        val smilePath = android.graphics.Path()
        smilePath.moveTo(18f, 28f)
        smilePath.quadTo(24f, 34f, 30f, 28f)
        canvas.drawPath(smilePath, smilePaint)
        
        ImageProvider.fromBitmap(bitmap)
    }
    
    // Функция для создания иконки заметки в фоновом потоке
    suspend fun createNoteIcon(): ImageProvider = withContext(Dispatchers.Default) {
        android.util.Log.d("MapView", "Creating note icon in background thread")
        val bitmap = android.graphics.Bitmap.createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Создаем синий круг для заметки
        val backgroundPaint = android.graphics.Paint().apply {
            color = 0xFF2196F3.toInt() // Синий цвет
            isAntiAlias = true
        }
        canvas.drawCircle(16f, 16f, 14f, backgroundPaint)
        
        // Добавляем белую точку в центре
        val centerPaint = android.graphics.Paint().apply {
            color = 0xFFFFFFFF.toInt() // Белый цвет
            isAntiAlias = true
        }
        canvas.drawCircle(16f, 16f, 6f, centerPaint)
        
        ImageProvider.fromBitmap(bitmap)
    }
    
    // Сохраняем состояние камеры
    var cameraPosition by remember { 
        mutableStateOf(
            CameraPosition(
                YandexPoint(55.751244, 37.618423), // Москва по умолчанию
                15.0f, 0.0f, 0.0f
            )
        ) 
    }
    
    // Предварительно создаем иконки при инициализации
    LaunchedEffect(Unit) {
        try {
            if (locationIcon == null) {
                locationIcon = createLocationIcon()
            }
        } catch (e: Exception) {
            android.util.Log.e("MapView", "Failed to pre-create location icon", e)
        }
        
        try {
            if (noteIcon == null) {
                noteIcon = createNoteIcon()
            }
        } catch (e: Exception) {
            android.util.Log.e("MapView", "Failed to pre-create note icon", e)
        }
    }
    
    // Флаг для отслеживания первого запуска
    var isFirstLaunch by remember { mutableStateOf(true) }
    
    // Если есть ошибка MapKit, показываем fallback UI
    if (mapKitError != null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Карта недоступна",
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "Проверьте подключение к интернету",
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "Ошибка: ${mapKitError}",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        return
    }
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            try {
                // Проверяем, инициализирован ли MapKit
                try {
                    MapKitFactory.getInstance()
                } catch (e: IllegalStateException) {
                    ErrorLogger.logError(
                        ctx,
                        e,
                        "MapKit not initialized, using fallback"
                    )
                    // Возвращаем простое представление в случае ошибки
                    return@AndroidView android.view.View(ctx).apply {
                        setBackgroundColor(android.graphics.Color.GRAY)
                    }
                }
                
                YandexMapView(ctx).apply {
                    // Настройка карты
                    val moscowPoint = YandexPoint(55.751244, 37.618423) // Москва по умолчанию
                    val startPoint = currentLocation?.let { 
                        android.util.Log.d("MapView", "Using current location for initial position: ${it.latitude}, ${it.longitude}")
                        YandexPoint(it.latitude, it.longitude) 
                    } ?: run {
                        android.util.Log.d("MapView", "No current location, using Moscow default: ${moscowPoint.latitude}, ${moscowPoint.longitude}")
                        moscowPoint
                    }
                    
                    android.util.Log.d("MapView", "Setting initial camera position to: ${startPoint.latitude}, ${startPoint.longitude}")
                    
                    // Устанавливаем начальную позицию камеры
                    map.move(
                        CameraPosition(startPoint, 15.0f, 0.0f, 0.0f),
                        Animation(Animation.Type.SMOOTH, 0.5f),
                        null
                    )
                    
                    // Сохраняем начальную позицию
                    cameraPosition = CameraPosition(startPoint, 15.0f, 0.0f, 0.0f)
                    
                    // Включаем жесты
                    map.isRotateGesturesEnabled = true
                    map.isZoomGesturesEnabled = true
                    map.isTiltGesturesEnabled = true
                    
                    // Слушаем изменения камеры для сохранения состояния
                    map.addCameraListener(object : com.yandex.mapkit.map.CameraListener {
                        override fun onCameraPositionChanged(
                            map: com.yandex.mapkit.map.Map,
                            position: CameraPosition,
                            reason: com.yandex.mapkit.map.CameraUpdateReason,
                            finished: Boolean
                        ) {
                            if (finished) {
                                cameraPosition = position
                            }
                        }
                    })

                    // Добавляем обработчик долгого нажатия
                    onLongPress?.let { longPressHandler ->
                        map.addInputListener(object : com.yandex.mapkit.map.InputListener {
                            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: com.yandex.mapkit.geometry.Point) {
                                // Обычное нажатие - ничего не делаем
                            }

                            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: com.yandex.mapkit.geometry.Point) {
                                // Долгое нажатие - вызываем обработчик
                                android.util.Log.d("MapView", "Long tap detected at: ${point.latitude}, ${point.longitude}")
                                longPressHandler(point.latitude, point.longitude)
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MapView", "Failed to create map view", e)
                mapKitError = e.message ?: "Unknown error"
                // Возвращаем пустой View как fallback
                android.view.View(ctx)
            }
        },
        update = { mapView ->
            try {
                val map = (mapView as YandexMapView).map
                
                // Очищаем предыдущие объекты
                map.mapObjects.clear()
                
                // Добавляем маркер текущего местоположения
                currentLocation?.let { location ->
                    val point = YandexPoint(location.latitude, location.longitude)
                    val placemark = map.mapObjects.addPlacemark(point)
                    placemark.setText("😊 Текущее местоположение")
                    
                    // Устанавливаем простую иконку для текущего местоположения
                    try {
                        val iconToUse = if (locationIcon == null) {
                            // Если иконка еще не создана, используем стандартную
                            android.util.Log.w("MapView", "Location icon not pre-created, using standard icon")
                            null
                        } else {
                            locationIcon!!
                        }
                        
                        if (iconToUse != null) {
                            placemark.setIcon(iconToUse)
                            // android.util.Log.d("MapView", "Programmatic emoji icon set successfully")
                        } else {
                            // Используем стандартную иконку если кастомная не готова
                            try {
                                placemark.setIcon(ImageProvider.fromResource(context, android.R.drawable.ic_menu_mylocation))
                                android.util.Log.d("MapView", "Using standard location icon")
                            } catch (e2: Exception) {
                                android.util.Log.e("MapView", "Failed to set standard location icon", e2)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MapView", "Failed to set programmatic emoji icon", e)
                        // Если и это не работает, используем стандартную иконку
                        try {
                            placemark.setIcon(ImageProvider.fromResource(context, android.R.drawable.ic_menu_mylocation))
                            android.util.Log.d("MapView", "Fallback to standard location icon")
                        } catch (e2: Exception) {
                            android.util.Log.e("MapView", "All icon setting methods failed", e2)
                        }
                    }
                    
                    // Центрируем карту на текущем местоположении при первом запуске или по запросу
                    if (isFirstLaunch || centerOnLocation) {
                        map.move(
                            CameraPosition(point, 15.0f, 0.0f, 0.0f),
                            Animation(Animation.Type.SMOOTH, 0.5f),
                            null
                        )
                        cameraPosition = CameraPosition(point, 15.0f, 0.0f, 0.0f)
                        isFirstLaunch = false
                    }
                }
                
                // Добавляем линию маршрута
                android.util.Log.d("MapView", "Received ${trackPoints.size} track points for route display")
                trackPoints.forEachIndexed { index, point ->
                    android.util.Log.d("MapView", "Point $index: ${point.latitude}, ${point.longitude}")
                }
                
                if (trackPoints.isNotEmpty()) {
                    val points = trackPoints.map { 
                        YandexPoint(it.latitude, it.longitude) 
                    }
                    
                    android.util.Log.d("MapView", "Creating polyline with ${points.size} points")
                    
                    if (points.size >= 2) {
                        val polyline = Polyline(points)
                        val polylineObject = map.mapObjects.addPolyline(polyline)
                        polylineObject.setStrokeColor(0xFF2196F3.toInt()) // Синий цвет
                        polylineObject.setStrokeWidth(5.0f)
                        
                        // Центрируем карту на маршруте если запрошено
                        if (centerOnTrack) {
                            // Вычисляем центр маршрута
                            val centerLat = points.map { it.latitude }.average()
                            val centerLon = points.map { it.longitude }.average()
                            val centerPoint = YandexPoint(centerLat, centerLon)
                            
                            android.util.Log.d("MapView", "Centering on multi-point track: $centerLat, $centerLon")
                            
                            map.move(
                                CameraPosition(centerPoint, 15.0f, 0.0f, 0.0f),
                                Animation(Animation.Type.SMOOTH, 0.5f),
                                null
                            )
                            cameraPosition = CameraPosition(centerPoint, 15.0f, 0.0f, 0.0f)
                        }
                    } else if (points.size == 1 && centerOnTrack) {
                        // Для маршрута с одной точкой просто центрируем на ней
                        val singlePoint = points.first()
                        android.util.Log.d("MapView", "Centering on single-point track: ${singlePoint.latitude}, ${singlePoint.longitude}")
                        
                        map.move(
                            CameraPosition(singlePoint, 15.0f, 0.0f, 0.0f),
                            Animation(Animation.Type.SMOOTH, 0.5f),
                            null
                        )
                        cameraPosition = CameraPosition(singlePoint, 15.0f, 0.0f, 0.0f)
                    }
                }
                
                // Добавляем заметки на карту
                android.util.Log.d("MapView", "Adding ${notes.size} notes to map")
                notes.forEach { note ->
                    // Уменьшаем логирование для производительности
                    // android.util.Log.d("MapView", "Adding note: ${note.title} at ${note.latitude}, ${note.longitude}")
                    val notePoint = YandexPoint(note.latitude, note.longitude)
                    val placemark = map.mapObjects.addPlacemark(notePoint)
                    
                    // Устанавливаем иконку для заметки программно
                    try {
                        val iconToUse = if (noteIcon == null) {
                            // Если иконка еще не создана, используем стандартную
                            android.util.Log.w("MapView", "Note icon not pre-created, using standard icon")
                            null
                        } else {
                            noteIcon!!
                        }
                        
                        if (iconToUse != null) {
                            placemark.setIcon(iconToUse)
                            // android.util.Log.d("MapView", "Programmatic icon set successfully for note: ${note.title}")
                        } else {
                            // Используем стандартную иконку если кастомная не готова
                            try {
                                placemark.setIcon(ImageProvider.fromResource(context, android.R.drawable.ic_menu_mylocation))
                                android.util.Log.d("MapView", "Using standard icon for note: ${note.title}")
                            } catch (e2: Exception) {
                                android.util.Log.e("MapView", "Failed to set standard icon for note: ${note.title}", e2)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MapView", "Failed to set programmatic icon for note: ${note.title}", e)
                        // Fallback к стандартной иконке Android
                        try {
                            placemark.setIcon(ImageProvider.fromResource(context, android.R.drawable.ic_menu_mylocation))
                            android.util.Log.d("MapView", "Fallback to standard Android icon for note: ${note.title}")
                        } catch (e2: Exception) {
                            android.util.Log.e("MapView", "All icon setting methods failed for note: ${note.title}", e2)
                        }
                    }
                    
                    // Устанавливаем текст заметки
                    try {
                        placemark.setText(note.title)
                        android.util.Log.d("MapView", "Text set for note: ${note.title}")
                    } catch (e: Exception) {
                        android.util.Log.e("MapView", "Failed to set text for note: ${note.title}", e)
                    }
                    
                    android.util.Log.d("MapView", "Successfully added placemark for note: ${note.title}")
                    
                    // Добавляем обработчик нажатия на заметку
                    onNoteClick?.let { clickHandler ->
                        placemark.addTapListener { _: com.yandex.mapkit.map.MapObject, _: com.yandex.mapkit.geometry.Point ->
                            android.util.Log.d("MapView", "Note clicked: ${note.title}")
                            clickHandler(note)
                            true
                        }
                    }
                }
                
                // Центрируем карту на заметке если запрошено
                centerOnNote?.let { note ->
                    val notePoint = YandexPoint(note.latitude, note.longitude)
                    map.move(
                        CameraPosition(notePoint, 16.0f, 0.0f, 0.0f),
                        Animation(Animation.Type.SMOOTH, 1.0f),
                        null
                    )
                    cameraPosition = CameraPosition(notePoint, 16.0f, 0.0f, 0.0f)
                }
            } catch (e: Exception) {
                android.util.Log.e("MapView", "Failed to update map", e)
                mapKitError = e.message ?: "Unknown error"
            }
        }
    )
}

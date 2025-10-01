package com.trackingapp.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.trackingapp.data.model.Point
import com.trackingapp.data.model.TrackPoint
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
    onLocationUpdate: (Point) -> Unit
) {
    val context = LocalContext.current
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            YandexMapView(ctx).apply {
                // Настройка карты
                val moscowPoint = YandexPoint(55.751244, 37.618423) // Москва по умолчанию
                val startPoint = currentLocation?.let { 
                    YandexPoint(it.latitude, it.longitude) 
                } ?: moscowPoint
                
                map.move(
                    CameraPosition(startPoint, 15.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 0.5f),
                    null
                )
                
                // Включаем кнопку местоположения
                map.isRotateGesturesEnabled = true
                map.isZoomGesturesEnabled = true
                map.isTiltGesturesEnabled = true
            }
        },
        update = { mapView ->
            val map = mapView.map
            
            // Очищаем предыдущие объекты
            map.mapObjects.clear()
            
            // Добавляем маркер текущего местоположения
            currentLocation?.let { location ->
                val point = YandexPoint(location.latitude, location.longitude)
                map.mapObjects.addPlacemark(point).apply {
                    setText("Текущее местоположение")
                }
                
                // Центрируем карту на текущем местоположении
                map.move(
                    CameraPosition(point, map.cameraPosition.zoom, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 0.5f),
                    null
                )
            }
            
            // Добавляем линию маршрута
            if (trackPoints.isNotEmpty()) {
                val points = trackPoints.map { 
                    YandexPoint(it.latitude, it.longitude) 
                }
                
                if (points.size >= 2) {
                    val polyline = Polyline(points)
                    map.mapObjects.addPolyline(polyline).apply {
                        setStrokeColor(0xFF2196F3.toInt()) // Синий цвет
                        strokeWidth = 5.0f
                    }
                }
            }
        }
    )
}

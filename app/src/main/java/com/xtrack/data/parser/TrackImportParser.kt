package com.xtrack.data.parser

import com.xtrack.data.model.GeoJsonTrack
import com.xtrack.data.model.GpxTrack
import com.xtrack.data.model.Track
import com.xtrack.data.model.TrackPoint
import com.xtrack.utils.ErrorLogger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackImportParser @Inject constructor() {

    /**
     * Импортирует трек из GPX файла
     */
    fun importFromGpx(inputStream: InputStream, fileName: String): ImportResult {
        return try {
            android.util.Log.i("TrackImportParser", "Starting GPX parsing for file: $fileName")
            
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var trackName = ""
            var trackDescription = ""
            val trackPoints = mutableListOf<TrackPoint>()
            var trackStartTime: Instant? = null
            var pointCount = 0
            
            android.util.Log.i("TrackImportParser", "Starting XML parsing loop")

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "name" -> {
                                if (parser.depth == 3) { // Track name
                                    trackName = parser.nextText() ?: ""
                                    android.util.Log.i("TrackImportParser", "Found track name: $trackName")
                                }
                            }
                            "desc" -> {
                                if (parser.depth == 3) { // Track description
                                    trackDescription = parser.nextText() ?: ""
                                }
                            }
                            "trkpt" -> {
                                val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                                val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                                
                                if (lat != null && lon != null) {
                                    pointCount++
                                    if (pointCount % 100 == 0) {
                                        android.util.Log.i("TrackImportParser", "Processed $pointCount track points")
                                    }
                                    var elevation: Double? = null
                                    var time: Instant? = null
                                    
                                    // Читаем дочерние элементы
                                    while (parser.next() != XmlPullParser.END_TAG || parser.name != "trkpt") {
                                        if (parser.eventType == XmlPullParser.START_TAG) {
                                            when (parser.name) {
                                                "ele" -> {
                                                    elevation = parser.nextText()?.toDoubleOrNull()
                                                }
                                                "time" -> {
                                                    val timeStr = parser.nextText()
                                                    time = try {
                                                        // Парсим ISO 8601 формат времени
                                                        Instant.parse(timeStr)
                                                    } catch (e: Exception) {
                                                        android.util.Log.w("TrackImportParser", "Failed to parse time: $timeStr", e)
                                                        null
                                                    }
                                                }
                                            }
                                        }
                                        // Убираем лишний parser.next() - он может вызывать пропуск событий
                                    }
                                    
                                    // Если время не указано, используем текущее время
                                    val finalTime = time ?: Clock.System.now()
                                    
                                    // Устанавливаем время начала трека
                                    if (trackStartTime == null) {
                                        trackStartTime = finalTime
                                    }
                                    
                                    val trackPoint = TrackPoint(
                                        id = UUID.randomUUID().toString(),
                                        trackId = "", // Будет установлен позже
                                        timestamp = finalTime,
                                        latitude = lat,
                                        longitude = lon,
                                        altitude = elevation,
                                        speed = null,
                                        bearing = null,
                                        accuracy = 0f
                                    )
                                    trackPoints.add(trackPoint)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            android.util.Log.i("TrackImportParser", "XML parsing completed. Found $pointCount track points")

            if (trackPoints.isEmpty()) {
                android.util.Log.w("TrackImportParser", "No track points found in file")
                ImportResult.Error("Файл не содержит точек трека")
            } else {
                // Создаем трек
                android.util.Log.i("TrackImportParser", "Calculating track distance and duration")
                val distance = calculateTrackDistance(trackPoints)
                val duration = calculateTrackDuration(trackPoints)
                android.util.Log.i("TrackImportParser", "Track calculated: distance=${distance}m, duration=${duration}s")
                
                val track = Track(
                    id = UUID.randomUUID().toString(),
                    name = trackName.ifEmpty { "Импортированный маршрут" },
                    startedAt = trackStartTime ?: Clock.System.now(),
                    endedAt = trackPoints.lastOrNull()?.timestamp,
                    distanceMeters = distance,
                    durationSec = duration,
                    gpxPath = null,
                    geojsonPath = null,
                    isRecording = false
                )
                
                // Обновляем trackId во всех точках
                val updatedTrackPoints = trackPoints.map { it.copy(trackId = track.id) }
                
                ImportResult.Success(track, updatedTrackPoints)
            }
        } catch (e: Exception) {
            android.util.Log.e("TrackImportParser", "Failed to parse GPX file: $fileName", e)
            ImportResult.Error("Ошибка парсинга GPX файла: ${e.message}")
        }
    }

    /**
     * Импортирует трек из GeoJSON файла
     */
    fun importFromGeoJson(inputStream: InputStream, fileName: String): ImportResult {
        return try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val geoJsonTrack = com.google.gson.Gson().fromJson(jsonString, GeoJsonTrack::class.java)
            
            if (geoJsonTrack.geometry.type != "LineString" || geoJsonTrack.geometry.coordinates.isEmpty()) {
                ImportResult.Error("Файл не содержит данных трека")
            } else {
                val trackPoints = mutableListOf<TrackPoint>()
                var trackStartTime: Instant? = null
                
                geoJsonTrack.geometry.coordinates.forEach { coordinate ->
                    if (coordinate.size >= 2) {
                        val lon = coordinate[0]
                        val lat = coordinate[1]
                        val elevation = if (coordinate.size >= 3) coordinate[2] else null
                        
                        // Используем текущее время, так как GeoJSON может не содержать временные метки
                        val timestamp = Clock.System.now()
                        
                        if (trackStartTime == null) {
                            trackStartTime = timestamp
                        }
                        
                        trackPoints.add(
                            TrackPoint(
                                id = UUID.randomUUID().toString(),
                                trackId = "", // Будет установлен позже
                                timestamp = timestamp,
                                latitude = lat,
                                longitude = lon,
                                altitude = elevation,
                                speed = null,
                                bearing = null,
                                accuracy = 0f
                            )
                        )
                    }
                }
                
                if (trackPoints.isEmpty()) {
                    ImportResult.Error("Файл не содержит точек трека")
                } else {
                    val track = Track(
                        id = UUID.randomUUID().toString(),
                        name = geoJsonTrack.properties?.name ?: "Импортированный маршрут",
                        startedAt = trackStartTime ?: Clock.System.now(),
                        endedAt = trackPoints.lastOrNull()?.timestamp,
                        distanceMeters = calculateTrackDistance(trackPoints),
                        durationSec = calculateTrackDuration(trackPoints),
                        gpxPath = null,
                        geojsonPath = null,
                        isRecording = false
                    )
                    
                    // Обновляем trackId во всех точках
                    val updatedTrackPoints = trackPoints.map { it.copy(trackId = track.id) }
                    
                    ImportResult.Success(track, updatedTrackPoints)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TrackImportParser", "Failed to parse GeoJSON file: $fileName", e)
            ImportResult.Error("Ошибка парсинга GeoJSON файла: ${e.message}")
        }
    }

    private fun calculateTrackDistance(trackPoints: List<TrackPoint>): Double {
        if (trackPoints.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 1 until trackPoints.size) {
            val prevPoint = trackPoints[i - 1]
            val currentPoint = trackPoints[i]
            totalDistance += calculateDistance(
                prevPoint.latitude, prevPoint.longitude,
                currentPoint.latitude, currentPoint.longitude
            )
        }
        return totalDistance
    }

    private fun calculateTrackDuration(trackPoints: List<TrackPoint>): Long {
        if (trackPoints.size < 2) return 0L
        
        val startTime = trackPoints.first().timestamp
        val endTime = trackPoints.last().timestamp
        return endTime.epochSeconds - startTime.epochSeconds
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Радиус Земли в метрах
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }

    sealed class ImportResult {
        data class Success(val track: Track, val trackPoints: List<TrackPoint>) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}

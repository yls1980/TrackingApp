package com.xtrack.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Заметка на карте с привязкой к координатам
 */
@Entity(tableName = "map_notes")
data class MapNote(
    @PrimaryKey
    val id: String,
    val trackId: String, // ID маршрута, к которому привязана заметка
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String? = null,
    val timestamp: Instant,
    val noteType: NoteType,
    val mediaPath: String? = null, // Путь к фото/видео
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

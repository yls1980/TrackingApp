package com.xtrack.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Последняя сохраненная геопозиция пользователя
 */
@Entity(tableName = "last_location")
data class LastLocation(
    @PrimaryKey
    val id: Int = 1, // Всегда один элемент
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant,
    val accuracy: Float? = null
)

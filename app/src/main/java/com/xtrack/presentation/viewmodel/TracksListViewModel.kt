package com.xtrack.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xtrack.data.model.Track
import com.xtrack.data.repository.MapNoteRepository
import com.xtrack.data.repository.TrackRepository
import com.xtrack.utils.ErrorLogger
import com.xtrack.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class TracksListViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val noteRepository: MapNoteRepository
) : ViewModel() {

    init {
        // Инициализируем кэш заметок при создании ViewModel
        updateNotesCache()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val allTracks: StateFlow<List<Track>> = trackRepository.getAllTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredTracks: StateFlow<List<Track>> = combine(
        allTracks,
        searchQuery,
        sortOrder
    ) { tracks, query, sort ->
        val filtered = if (query.isBlank()) {
            tracks
        } else {
            tracks.filter { track ->
                track.name.contains(query, ignoreCase = true)
            }
        }

        when (sort) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.startedAt }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.startedAt }
            SortOrder.DISTANCE_DESC -> filtered.sortedByDescending { it.distanceMeters }
            SortOrder.DISTANCE_ASC -> filtered.sortedBy { it.distanceMeters }
            SortOrder.DURATION_DESC -> filtered.sortedByDescending { it.durationSec }
            SortOrder.DURATION_ASC -> filtered.sortedBy { it.durationSec }
            SortOrder.NAME_ASC -> filtered.sortedBy { it.name }
            SortOrder.NAME_DESC -> filtered.sortedByDescending { it.name }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            trackRepository.deleteTrack(track)
            // Обновляем кэш после удаления трека
            updateNotesCache()
        }
    }

    fun formatDistance(distanceMeters: Double): String {
        return LocationUtils.formatDistance(distanceMeters)
    }

    fun formatDuration(durationSeconds: Long): String {
        return LocationUtils.formatDuration(durationSeconds)
    }

    // Кэш для проверки наличия заметок, чтобы избежать блокировки UI
    private val notesCache = mutableMapOf<String, Boolean>()
    
    fun hasNotes(trackId: String): Boolean {
        // Возвращаем кэшированное значение или false по умолчанию
        return notesCache[trackId] ?: false
    }
    
    // Функция для обновления кэша заметок
    private fun updateNotesCache() {
        viewModelScope.launch {
            try {
                // Получаем все заметки и группируем по trackId
                val allNotes = noteRepository.getAllMapNotes().first()
                val notesByTrack = allNotes.groupBy { it.trackId }
                
                // Обновляем кэш
                notesCache.clear()
                notesByTrack.forEach { (trackId, notes) ->
                    notesCache[trackId] = notes.isNotEmpty()
                }
                
                android.util.Log.d("TracksListViewModel", "Notes cache updated: ${notesCache.size} tracks")
            } catch (e: Exception) {
                android.util.Log.e("TracksListViewModel", "Failed to update notes cache", e)
            }
        }
    }
    
    // Публичная функция для обновления кэша извне (например, после создания заметки)
    fun refreshNotesCache() {
        updateNotesCache()
    }

    enum class SortOrder {
        DATE_DESC,
        DATE_ASC,
        DISTANCE_DESC,
        DISTANCE_ASC,
        DURATION_DESC,
        DURATION_ASC,
        NAME_ASC,
        NAME_DESC
    }
}






package com.trackingapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackingapp.data.model.Track
import com.trackingapp.data.repository.TrackRepository
import com.trackingapp.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TracksListViewModel @Inject constructor(
    private val trackRepository: TrackRepository
) : ViewModel() {

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
        }
    }

    fun formatDistance(distanceMeters: Double): String {
        return LocationUtils.formatDistance(distanceMeters)
    }

    fun formatDuration(durationSeconds: Long): String {
        return LocationUtils.formatDuration(durationSeconds)
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




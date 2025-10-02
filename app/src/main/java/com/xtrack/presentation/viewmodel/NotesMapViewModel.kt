package com.xtrack.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xtrack.data.model.MapNote
import com.xtrack.data.repository.MapNoteRepository
import com.xtrack.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesMapViewModel @Inject constructor(
    private val mapNoteRepository: MapNoteRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<MapNote>>(emptyList())
    val notes: StateFlow<List<MapNote>> = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedNote = MutableStateFlow<MapNote?>(null)
    val selectedNote: StateFlow<MapNote?> = _selectedNote.asStateFlow()

    private val _showNotes = MutableStateFlow(true)
    val showNotes: StateFlow<Boolean> = _showNotes.asStateFlow()
    
    private val _lastNote = MutableStateFlow<MapNote?>(null)
    val lastNote: StateFlow<MapNote?> = _lastNote.asStateFlow()
    
    private val _currentNoteIndex = MutableStateFlow(0)
    val currentNoteIndex: StateFlow<Int> = _currentNoteIndex.asStateFlow()
    
    private var context: Context? = null
    
    fun setContext(context: Context) {
        this.context = context
    }

    fun loadNotes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val notesList = mapNoteRepository.getAllMapNotes().first()
                val sortedNotes = notesList.sortedByDescending { it.timestamp }
                _notes.value = sortedNotes
                
                // Устанавливаем последнюю заметку (самую новую по времени)
                _lastNote.value = sortedNotes.firstOrNull()
                
                android.util.Log.d("NotesMapViewModel", "Loaded ${notesList.size} notes for map")
                context?.let { ctx ->
                    ErrorLogger.logMessage(
                        ctx,
                        "Loaded ${notesList.size} notes for map, last note: ${_lastNote.value?.title}",
                        ErrorLogger.LogLevel.INFO
                    )
                }
            } catch (e: Exception) {
                context?.let { ctx ->
                    ErrorLogger.logError(
                        ctx,
                        e,
                        "Failed to load notes for map"
                    )
                }
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectNote(note: MapNote) {
        _selectedNote.value = note
    }

    fun clearSelectedNote() {
        _selectedNote.value = null
    }

    fun toggleShowNotes() {
        _showNotes.value = !_showNotes.value
    }
    
    fun cycleToNextNote() {
        val notesList = _notes.value
        if (notesList.isNotEmpty()) {
            val currentIndex = _currentNoteIndex.value
            val nextIndex = (currentIndex + 1) % notesList.size
            _currentNoteIndex.value = nextIndex
            _selectedNote.value = notesList[nextIndex]
            
            android.util.Log.d("NotesMapViewModel", "Cycled to note ${nextIndex + 1}/${notesList.size}: ${notesList[nextIndex].title}")
            context?.let { ctx ->
                ErrorLogger.logMessage(
                    ctx,
                    "Cycled to note ${nextIndex + 1}/${notesList.size}: ${notesList[nextIndex].title}",
                    ErrorLogger.LogLevel.INFO
                )
            }
        }
    }
    
    fun resetNoteIndex() {
        _currentNoteIndex.value = 0
        _selectedNote.value = null
    }
}

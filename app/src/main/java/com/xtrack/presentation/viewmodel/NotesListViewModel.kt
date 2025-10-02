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
class NotesListViewModel @Inject constructor(
    private val mapNoteRepository: MapNoteRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<MapNote>>(emptyList())
    val notes: StateFlow<List<MapNote>> = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
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
                _notes.value = notesList.sortedByDescending { it.timestamp }
                context?.let { ctx ->
                    ErrorLogger.logMessage(
                        ctx,
                        "Loaded ${notesList.size} notes for list",
                        ErrorLogger.LogLevel.INFO
                    )
                }
            } catch (e: Exception) {
                context?.let { ctx ->
                    ErrorLogger.logError(
                        ctx,
                        e,
                        "Failed to load notes"
                    )
                }
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteNote(note: MapNote) {
        viewModelScope.launch {
            try {
                mapNoteRepository.deleteMapNote(note)
                
                // Обновляем список заметок после удаления
                val currentNotes = _notes.value.toMutableList()
                currentNotes.removeAll { it.id == note.id }
                _notes.value = currentNotes
                
                context?.let { ctx ->
                    ErrorLogger.logMessage(
                        ctx,
                        "Note deleted: ${note.title}",
                        ErrorLogger.LogLevel.INFO
                    )
                }
            } catch (e: Exception) {
                context?.let { ctx ->
                    ErrorLogger.logError(
                        ctx,
                        e,
                        "Failed to delete note: ${note.title}"
                    )
                }
                _error.value = "Ошибка удаления заметки: ${e.message}"
            }
        }
    }
}

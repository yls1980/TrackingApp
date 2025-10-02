package com.xtrack.presentation.viewmodel

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
class NoteDetailViewModel @Inject constructor(
    private val mapNoteRepository: MapNoteRepository
) : ViewModel() {

    private val _note = MutableStateFlow<MapNote?>(null)
    val note: StateFlow<MapNote?> = _note.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadNote(noteId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val note = mapNoteRepository.getMapNoteById(noteId).first()
                _note.value = note
            } catch (e: Exception) {
                android.util.Log.e("NoteDetailViewModel", "Failed to load note detail for ID: $noteId", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try {
                mapNoteRepository.deleteMapNote(noteId)
                android.util.Log.i("NoteDetailViewModel", "Note deleted successfully: $noteId")
            } catch (e: Exception) {
                android.util.Log.e("NoteDetailViewModel", "Failed to delete note: $noteId", e)
                _error.value = e.message
            }
        }
    }
}

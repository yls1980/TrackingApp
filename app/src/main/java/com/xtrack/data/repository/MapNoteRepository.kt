package com.xtrack.data.repository

import com.xtrack.data.database.MapNoteDao
import com.xtrack.data.model.MapNote
import com.xtrack.utils.ErrorLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapNoteRepository @Inject constructor(
    private val mapNoteDao: MapNoteDao
) {
    fun getNotesByTrackId(trackId: String): Flow<List<MapNote>> = 
        mapNoteDao.getNotesByTrackId(trackId)

    suspend fun getNoteById(noteId: String): MapNote? = 
        mapNoteDao.getNoteById(noteId)

    suspend fun insertNote(note: MapNote) = 
        mapNoteDao.insertNote(note)

    suspend fun updateNote(note: MapNote) = 
        mapNoteDao.updateNote(note)

    suspend fun deleteNote(note: MapNote) = 
        mapNoteDao.deleteNote(note)

    suspend fun deleteNotesByTrackId(trackId: String) = 
        mapNoteDao.deleteNotesByTrackId(trackId)
    
    fun getAllMapNotes(): Flow<List<MapNote>> = 
        mapNoteDao.getAllMapNotes()
    
    suspend fun insertMapNote(mapNote: MapNote) = 
        mapNoteDao.insertMapNote(mapNote)
    
    suspend fun updateMapNote(mapNote: MapNote) = 
        mapNoteDao.updateMapNote(mapNote)
    
    suspend fun deleteMapNote(mapNote: MapNote) = 
        mapNoteDao.deleteMapNote(mapNote)
    
    fun getMapNoteById(noteId: String): Flow<MapNote?> = 
        mapNoteDao.getMapNoteById(noteId)
    
    fun getMapNotesForTrack(trackId: String): Flow<List<MapNote>> = 
        mapNoteDao.getMapNotesForTrack(trackId)
    
    suspend fun deleteMapNote(noteId: String) = 
        mapNoteDao.deleteMapNoteById(noteId)

    /**
     * Безопасно удаляет заметку и связанные с ней медиа файлы
     */
    suspend fun deleteNoteWithMedia(note: MapNote) {
        try {
            // Сначала удаляем медиа файлы
            deleteMediaFiles(note)
            
            // Затем удаляем заметку из базы данных
            mapNoteDao.deleteMapNote(note)
            
            android.util.Log.i("MapNoteRepository", "Note and media files deleted successfully: ${note.title}")
        } catch (e: Exception) {
            android.util.Log.e("MapNoteRepository", "Failed to delete note with media: ${note.title}", e)
            throw e
        }
    }

    /**
     * Удаляет медиа файлы, связанные с заметкой
     */
    private suspend fun deleteMediaFiles(note: MapNote) {
        note.mediaPath?.let { mediaPath ->
            try {
                val mediaFile = File(mediaPath)
                if (mediaFile.exists()) {
                    val deleted = mediaFile.delete()
                    if (deleted) {
                        android.util.Log.i("MapNoteRepository", "Media file deleted: $mediaPath")
                    } else {
                        android.util.Log.w("MapNoteRepository", "Failed to delete media file: $mediaPath")
                    }
                } else {
                    android.util.Log.w("MapNoteRepository", "Media file not found: $mediaPath")
                }
            } catch (e: Exception) {
                android.util.Log.e("MapNoteRepository", "Error deleting media file: $mediaPath", e)
                // Не прерываем удаление заметки из-за ошибки с медиа файлом
            }
        }
    }

    /**
     * Безопасно удаляет заметку по ID и связанные с ней медиа файлы
     */
    suspend fun deleteNoteWithMediaById(noteId: String) {
        try {
            // Получаем заметку из базы данных
            val note = mapNoteDao.getNoteById(noteId)
            if (note != null) {
                deleteNoteWithMedia(note)
            } else {
                android.util.Log.w("MapNoteRepository", "Note not found for deletion: $noteId")
            }
        } catch (e: Exception) {
            android.util.Log.e("MapNoteRepository", "Failed to delete note by ID: $noteId", e)
            throw e
        }
    }

    /**
     * Безопасно удаляет все заметки трека и связанные с ними медиа файлы
     */
    suspend fun deleteAllNotesWithMediaByTrackId(trackId: String) {
        try {
            // Получаем все заметки трека
            val notes = mapNoteDao.getNotesByTrackId(trackId).first()
            
            if (notes.isNotEmpty()) {
                android.util.Log.i("MapNoteRepository", "Deleting ${notes.size} notes with media files for track: $trackId")
                
                // Удаляем каждую заметку с её медиа файлами
                notes.forEach { note ->
                    try {
                        deleteNoteWithMedia(note)
                    } catch (e: Exception) {
                        android.util.Log.e("MapNoteRepository", "Failed to delete note ${note.id} for track $trackId", e)
                        // Продолжаем удаление других заметок
                    }
                }
                
                android.util.Log.i("MapNoteRepository", "All notes and media files deleted for track: $trackId")
            } else {
                android.util.Log.i("MapNoteRepository", "No notes found for track: $trackId")
            }
        } catch (e: Exception) {
            android.util.Log.e("MapNoteRepository", "Failed to delete notes for track: $trackId", e)
            throw e
        }
    }
}

package com.xtrack.data.repository

import com.xtrack.data.database.MapNoteDao
import com.xtrack.data.model.MapNote
import kotlinx.coroutines.flow.Flow
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
}

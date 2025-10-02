package com.xtrack.data.database

import androidx.room.*
import com.xtrack.data.model.MapNote
import kotlinx.coroutines.flow.Flow

@Dao
interface MapNoteDao {
    @Query("SELECT * FROM map_notes WHERE trackId = :trackId ORDER BY timestamp DESC")
    fun getNotesByTrackId(trackId: String): Flow<List<MapNote>>

    @Query("SELECT * FROM map_notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): MapNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: MapNote)

    @Update
    suspend fun updateNote(note: MapNote)

    @Delete
    suspend fun deleteNote(note: MapNote)

    @Query("DELETE FROM map_notes WHERE trackId = :trackId")
    suspend fun deleteNotesByTrackId(trackId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapNote(mapNote: MapNote)

    @Update
    suspend fun updateMapNote(mapNote: MapNote)

    @Delete
    suspend fun deleteMapNote(mapNote: MapNote)

    @Query("SELECT * FROM map_notes WHERE id = :noteId")
    fun getMapNoteById(noteId: String): Flow<MapNote?>

    @Query("SELECT * FROM map_notes WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getMapNotesForTrack(trackId: String): Flow<List<MapNote>>

    @Query("SELECT * FROM map_notes ORDER BY timestamp DESC")
    fun getAllMapNotes(): Flow<List<MapNote>>
    
    @Query("DELETE FROM map_notes WHERE id = :noteId")
    suspend fun deleteMapNoteById(noteId: String)
}

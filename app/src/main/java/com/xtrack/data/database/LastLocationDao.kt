package com.xtrack.data.database

import androidx.room.*
import com.xtrack.data.model.LastLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface LastLocationDao {
    @Query("SELECT * FROM last_location WHERE id = 1")
    fun getLastLocation(): Flow<LastLocation?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLastLocation(location: LastLocation)

    @Update
    suspend fun updateLastLocation(location: LastLocation)

    @Delete
    suspend fun deleteLastLocation(location: LastLocation)
}

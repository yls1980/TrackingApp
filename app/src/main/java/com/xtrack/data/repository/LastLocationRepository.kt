package com.xtrack.data.repository

import com.xtrack.data.database.LastLocationDao
import com.xtrack.data.model.LastLocation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastLocationRepository @Inject constructor(
    private val lastLocationDao: LastLocationDao
) {
    fun getLastLocation(): Flow<LastLocation?> = 
        lastLocationDao.getLastLocation()

    suspend fun saveLastLocation(location: LastLocation) = 
        lastLocationDao.insertLastLocation(location)

    suspend fun updateLastLocation(location: LastLocation) = 
        lastLocationDao.updateLastLocation(location)

    suspend fun deleteLastLocation(location: LastLocation) = 
        lastLocationDao.deleteLastLocation(location)
}

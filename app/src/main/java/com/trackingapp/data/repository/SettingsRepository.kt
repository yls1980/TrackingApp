package com.trackingapp.data.repository

import com.trackingapp.data.database.AppSettingsDao
import com.trackingapp.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val appSettingsDao: AppSettingsDao
) {
    fun getSettings(): Flow<AppSettings> = appSettingsDao.getSettings()

    suspend fun getSettingsSync(): AppSettings? = appSettingsDao.getSettingsSync()

    suspend fun updateSettings(settings: AppSettings) = appSettingsDao.updateSettings(settings)

    suspend fun insertDefaultSettings() {
        val existingSettings = appSettingsDao.getSettingsSync()
        if (existingSettings == null) {
            appSettingsDao.insertSettings(AppSettings())
        }
    }
}




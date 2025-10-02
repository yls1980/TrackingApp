package com.xtrack.data.repository

import com.xtrack.data.database.AppSettingsDao
import com.xtrack.data.model.AppSettings
import com.xtrack.utils.ErrorLogger
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
        try {
            val existingSettings = appSettingsDao.getSettingsSync()
            if (existingSettings == null) {
                val defaultSettings = AppSettings()
                appSettingsDao.insertSettings(defaultSettings)
                android.util.Log.i("SettingsRepository", "Default settings inserted successfully")
            } else {
                android.util.Log.i("SettingsRepository", "Settings already exist, skipping initialization")
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Failed to insert default settings", e)
            throw e
        }
    }
}





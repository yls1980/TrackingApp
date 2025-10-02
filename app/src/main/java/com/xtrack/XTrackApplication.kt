package com.xtrack

import android.app.Application
import com.xtrack.utils.ErrorLogger
import com.xtrack.utils.GlobalExceptionHandler
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class XTrackApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Устанавливаем глобальный обработчик исключений
        GlobalExceptionHandler.install(this)
        
        // Логируем запуск приложения
        ErrorLogger.logMessage(
            this,
            "Application started",
            ErrorLogger.LogLevel.INFO
        )
        
        // Инициализация Yandex MapKit
        try {
            val apiKey = getString(R.string.yandex_maps_key)
            ErrorLogger.logMessage(
                this,
                "Initializing Yandex MapKit with API key: ${apiKey.take(10)}...",
                ErrorLogger.LogLevel.INFO
            )
            
            // Пропускаем загрузку magtsync - эта библиотека не нужна для базовой функциональности MapKit
            ErrorLogger.logMessage(
                this,
                "Skipping magtsync library loading - not required for basic MapKit functionality",
                ErrorLogger.LogLevel.INFO
            )
            
            MapKitFactory.setApiKey(apiKey)
            MapKitFactory.initialize(this)
            
            ErrorLogger.logMessage(
                this,
                "Yandex MapKit initialized successfully",
                ErrorLogger.LogLevel.INFO
            )
        } catch (e: Exception) {
            ErrorLogger.logError(
                this,
                e,
                "Failed to initialize Yandex MapKit"
            )
        }
    }
}


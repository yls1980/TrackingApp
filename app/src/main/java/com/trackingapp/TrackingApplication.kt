package com.trackingapp

import android.app.Application
import com.trackingapp.utils.ErrorLogger
import com.trackingapp.utils.GlobalExceptionHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TrackingApplication : Application() {
    
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
    }
}


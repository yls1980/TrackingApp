package com.trackingapp.utils

import android.content.Context

/**
 * Глобальный обработчик необработанных исключений
 * Перехватывает все краши приложения и записывает их в лог
 */
class GlobalExceptionHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Логируем необработанное исключение
            ErrorLogger.logError(
                context,
                throwable,
                "UNCAUGHT EXCEPTION in thread: ${thread.name}"
            )
            
            // Добавляем информацию о крашe
            ErrorLogger.logMessage(
                context,
                "Application crashed! Thread: ${thread.name}",
                ErrorLogger.LogLevel.ERROR
            )
            
        } catch (e: Exception) {
            // Если не удалось залогировать, ничего не делаем
        } finally {
            // Передаем управление стандартному обработчику
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    companion object {
        /**
         * Установить глобальный обработчик исключений
         */
        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            val customHandler = GlobalExceptionHandler(context, defaultHandler)
            Thread.setDefaultUncaughtExceptionHandler(customHandler)
            
            ErrorLogger.logMessage(
                context,
                "Global exception handler installed",
                ErrorLogger.LogLevel.INFO
            )
        }
    }
}


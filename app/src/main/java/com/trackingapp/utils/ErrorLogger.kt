package com.trackingapp.utils

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Класс для записи всех ошибок приложения в файл
 * Использование: ErrorLogger.logError(context, exception, "Описание где произошла ошибка")
 */
object ErrorLogger {
    private const val TAG = "ErrorLogger"
    private const val LOG_FILE_NAME = "app_errors.log"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
    
    /**
     * Логирование ошибки в файл
     */
    fun logError(context: Context, throwable: Throwable, additionalInfo: String = "") {
        try {
            val logFile = getLogFile(context)
            
            // Проверяем размер файла и очищаем если слишком большой
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                archiveOldLog(context, logFile)
            }
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val errorMessage = buildErrorMessage(timestamp, throwable, additionalInfo)
            
            // Записываем в файл
            FileOutputStream(logFile, true).use { fos ->
                fos.write(errorMessage.toByteArray())
            }
            
            // Также выводим в logcat
            Log.e(TAG, "Error logged: $additionalInfo", throwable)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log error", e)
        }
    }
    
    /**
     * Логирование простого сообщения
     */
    fun logMessage(context: Context, message: String, level: LogLevel = LogLevel.INFO) {
        try {
            val logFile = getLogFile(context)
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            
            val logMessage = """
                ========================================
                [$level] $timestamp
                Message: $message
                ========================================
                
            """.trimIndent()
            
            FileOutputStream(logFile, true).use { fos ->
                fos.write(logMessage.toByteArray())
            }
            
            when (level) {
                LogLevel.ERROR -> Log.e(TAG, message)
                LogLevel.WARNING -> Log.w(TAG, message)
                LogLevel.INFO -> Log.i(TAG, message)
                LogLevel.DEBUG -> Log.d(TAG, message)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log message", e)
        }
    }
    
    /**
     * Получить файл логов
     */
    fun getLogFile(context: Context): File {
        val logsDir = File(context.getExternalFilesDir(null), "logs")
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        return File(logsDir, LOG_FILE_NAME)
    }
    
    /**
     * Получить содержимое лог-файла
     */
    fun getLogContent(context: Context): String {
        return try {
            val logFile = getLogFile(context)
            if (logFile.exists()) {
                logFile.readText()
            } else {
                "Лог-файл пуст"
            }
        } catch (e: Exception) {
            "Ошибка чтения лог-файла: ${e.message}"
        }
    }
    
    /**
     * Очистить логи
     */
    fun clearLogs(context: Context) {
        try {
            val logFile = getLogFile(context)
            if (logFile.exists()) {
                logFile.delete()
            }
            Log.i(TAG, "Logs cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
    
    /**
     * Построить детальное сообщение об ошибке
     */
    private fun buildErrorMessage(timestamp: String, throwable: Throwable, additionalInfo: String): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()
        
        return """
            ========================================
            ERROR: $timestamp
            ========================================
            Additional Info: $additionalInfo
            
            Device Info:
            - Manufacturer: ${Build.MANUFACTURER}
            - Model: ${Build.MODEL}
            - Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            
            Exception:
            - Type: ${throwable.javaClass.simpleName}
            - Message: ${throwable.message ?: "No message"}
            
            Stack Trace:
            $stackTrace
            ========================================
            
        """.trimIndent()
    }
    
    /**
     * Архивировать старый лог
     */
    private fun archiveOldLog(context: Context, logFile: File) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val archiveFile = File(logFile.parent, "app_errors_$timestamp.log")
            logFile.copyTo(archiveFile, overwrite = true)
            logFile.delete()
            Log.i(TAG, "Old log archived to: ${archiveFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to archive old log", e)
            logFile.delete() // Просто удаляем если не удалось архивировать
        }
    }
    
    enum class LogLevel {
        ERROR, WARNING, INFO, DEBUG
    }
}


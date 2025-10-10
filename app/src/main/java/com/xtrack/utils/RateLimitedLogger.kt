package com.xtrack.utils

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Логгер с ограничением частоты сообщений для предотвращения спама в логах
 */
object RateLimitedLogger {
    private val messageCounts = ConcurrentHashMap<String, AtomicLong>()
    private val lastResetTime = AtomicLong(System.currentTimeMillis())
    private const val RESET_INTERVAL_MS = 60_000L // 1 минута
    private const val MAX_MESSAGES_PER_MINUTE = 60 // Максимум 60 сообщений в минуту
    
    /**
     * Логирует сообщение с ограничением частоты
     * @param context Контекст приложения
     * @param message Сообщение для логирования
     * @param level Уровень логирования
     * @param key Уникальный ключ для группировки сообщений (по умолчанию используется message)
     */
    fun logMessage(
        context: Context,
        message: String,
        level: ErrorLogger.LogLevel = ErrorLogger.LogLevel.INFO,
        key: String = message
    ) {
        val currentTime = System.currentTimeMillis()
        
        // Сбрасываем счетчики каждую минуту
        if (currentTime - lastResetTime.get() > RESET_INTERVAL_MS) {
            messageCounts.clear()
            lastResetTime.set(currentTime)
        }
        
        // Получаем или создаем счетчик для данного ключа
        val counter = messageCounts.computeIfAbsent(key) { AtomicLong(0) }
        val currentCount = counter.incrementAndGet()
        
        // Логируем только если не превышен лимит
        if (currentCount <= MAX_MESSAGES_PER_MINUTE) {
            ErrorLogger.logMessage(context, message, level)
        } else if (currentCount == MAX_MESSAGES_PER_MINUTE.toLong() + 1) {
            // Логируем сообщение о превышении лимита только один раз
            ErrorLogger.logMessage(
                context,
                "Rate limit exceeded for key '$key', suppressing further messages for 1 minute",
                ErrorLogger.LogLevel.WARNING
            )
        }
    }
    
    /**
     * Логирует ошибку без ограничений (ошибки всегда важны)
     */
    fun logError(
        context: Context,
        throwable: Throwable,
        additionalInfo: String = ""
    ) {
        ErrorLogger.logError(context, throwable, additionalInfo)
    }
    
    /**
     * Проверяет, можно ли логировать сообщение с данным ключом
     */
    fun canLog(key: String): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Сбрасываем счетчики каждую минуту
        if (currentTime - lastResetTime.get() > RESET_INTERVAL_MS) {
            messageCounts.clear()
            lastResetTime.set(currentTime)
        }
        
        val counter = messageCounts.computeIfAbsent(key) { AtomicLong(0) }
        return counter.get() < MAX_MESSAGES_PER_MINUTE.toLong()
    }
}

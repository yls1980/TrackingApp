package com.xtrack.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import com.xtrack.service.LocationTrackingService

object ServiceUtils {
    
    /**
     * Проверяет, запущен ли указанный сервис
     */
    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        
        return runningServices.any { serviceInfo ->
            serviceInfo.service.className == serviceClass.name
        }
    }
    
    /**
     * Получает список всех запущенных сервисов (для отладки)
     */
    fun getRunningServices(context: Context): List<String> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        
        return runningServices.map { serviceInfo ->
            "${serviceInfo.service.className} (PID: ${serviceInfo.pid})"
        }
    }
    
    /**
     * Проверяет, запущен ли LocationTrackingService
     */
    fun isLocationTrackingServiceRunning(context: Context): Boolean {
        return isServiceRunning(context, LocationTrackingService::class.java)
    }
}

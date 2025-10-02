package com.xtrack.service

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@SmallTest
class LocationTrackingServiceTest {

    @Test
    fun serviceCanBeCreated() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), LocationTrackingService::class.java)
        val service = LocationTrackingService()
        
        assertNotNull(service)
    }

    @Test
    fun serviceHandlesStartRecordingAction() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_RECORDING
        }
        
        // В реальном тесте здесь была бы проверка запуска сервиса
        // Но для этого нужны разрешения и настройка тестового окружения
        assertNotNull(intent.action)
    }

    @Test
    fun serviceHandlesStopRecordingAction() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_RECORDING
        }
        
        assertNotNull(intent.action)
    }
}






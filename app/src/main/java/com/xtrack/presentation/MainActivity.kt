package com.xtrack.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.xtrack.R
import com.xtrack.presentation.navigation.TrackingNavigation
import com.xtrack.presentation.theme.XTrackTheme
import com.xtrack.presentation.viewmodel.MainViewModel
import com.xtrack.utils.ErrorLogger
import com.xtrack.utils.ServiceUtils
import com.xtrack.service.LocationTrackingService
import android.content.Intent
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    
    // Запрос разрешения на уведомления для Android 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ErrorLogger.logMessage(
                this,
                "POST_NOTIFICATIONS permission granted",
                ErrorLogger.LogLevel.INFO
            )
        } else {
            ErrorLogger.logMessage(
                this,
                "POST_NOTIFICATIONS permission denied - notifications will not work",
                ErrorLogger.LogLevel.WARNING
            )
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Логируем запуск MainActivity
            ErrorLogger.logMessage(
                this,
                "MainActivity onCreate started",
                ErrorLogger.LogLevel.INFO
            )
            
            // Обрабатываем аварийное завершение записи при предыдущем запуске
            handleEmergencyStopOnAppStart()
            
            // Запрашиваем разрешение на уведомления для Android 13+
            requestNotificationPermission()
            
        } catch (e: Exception) {
            ErrorLogger.logError(
                this,
                e,
                "Failed to initialize MainActivity"
            )
            // Не крашим приложение, продолжаем работу
        }
        
        setContent {
            XTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val locationPermissionState = rememberPermissionState(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )

                    if (locationPermissionState.status.isGranted) {
                        TrackingNavigation(
                            mainViewModel = mainViewModel,
                            navController = navController,
                            onExitApp = {
                                // Свернуть приложение (минимизировать)
                                moveTaskToBack(true)
                            },
                            onKillApp = {
                                // Полностью закрыть приложение и освободить память
                                try {
                                    // Останавливаем все сервисы
                                    val stopServiceIntent = Intent(this, com.xtrack.service.LocationTrackingService::class.java)
                                    stopService(stopServiceIntent)
                                    
                                    // Очищаем задачи приложения из системного кэша
                                    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                                    activityManager.appTasks?.forEach { task ->
                                        task.finishAndRemoveTask()
                                    }
                                    
                                    // Завершаем все активности
                                    finishAffinity()
                                    
                                    // Убиваем процесс
                                    android.os.Process.killProcess(android.os.Process.myPid())
                                    System.exit(0)
                                } catch (e: Exception) {
                                    try {
                                        // Fallback 1 - пытаемся удалить из задач
                                        finishAndRemoveTask()
                                        android.os.Process.killProcess(android.os.Process.myPid())
                                        System.exit(0)
                                    } catch (e2: Exception) {
                                        // Fallback 2 - базовое завершение
                                        finishAffinity()
                                        System.exit(0)
                                    }
                                }
                            }
                        )
                    } else {
                        PermissionScreen(
                            onRequestPermission = {
                                locationPermissionState.launchPermissionRequest()
                            }
                        )
                    }
                }
            }
        }
    }
    
    private fun requestNotificationPermission() {
        // Запрашиваем разрешение только для Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        try {
            MapKitFactory.getInstance().onStart()
        } catch (e: Exception) {
            ErrorLogger.logMessage(
                this,
                "MapKit onStart failed: ${e.message}",
                ErrorLogger.LogLevel.WARNING
            )
        }
    }
    
    override fun onStop() {
        try {
            MapKitFactory.getInstance().onStop()
        } catch (e: Exception) {
            ErrorLogger.logMessage(
                this,
                "MapKit onStop failed: ${e.message}",
                ErrorLogger.LogLevel.WARNING
            )
        }
        super.onStop()
    }
    
    /**
     * Обрабатывает аварийное завершение записи при запуске приложения
     */
    private fun handleEmergencyStopOnAppStart() {
        try {
            // Проверяем, был ли сервис записи активен при предыдущем запуске
            val isServiceRunning = com.xtrack.utils.ServiceUtils.isLocationTrackingServiceRunning(this)
            
            if (!isServiceRunning) {
                // Сервис не работает - проверяем, нужна ли аварийная остановка
                // Аварийная остановка нужна только если приложение было закрыто во время записи
                ErrorLogger.logMessage(
                    this,
                    "Service is not running on app start - emergency stop will be handled by MainViewModel if needed",
                    ErrorLogger.LogLevel.INFO
                )
                
                // Аварийную остановку теперь обрабатывает MainViewModel на основе appExitState
                // Здесь больше не нужно отправлять ACTION_EMERGENCY_STOP
            } else {
                ErrorLogger.logMessage(
                    this,
                    "Service is running on app start - no emergency stop needed",
                    ErrorLogger.LogLevel.INFO
                )
            }
        } catch (e: Exception) {
            ErrorLogger.logError(
                this,
                e,
                "Failed to handle emergency stop on app start"
            )
        }
    }
}

@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Требуется разрешение на местоположение",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Приложению необходимо разрешение на доступ к местоположению для записи GPS-треков",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Предоставить разрешение")
        }
    }
}


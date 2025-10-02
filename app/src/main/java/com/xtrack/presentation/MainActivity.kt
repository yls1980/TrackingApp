package com.xtrack.presentation

import android.Manifest
import android.content.pm.PackageManager
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
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

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
                                finishAffinity() // Закрывает приложение
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


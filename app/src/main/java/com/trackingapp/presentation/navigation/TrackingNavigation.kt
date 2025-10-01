package com.trackingapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.trackingapp.presentation.screen.ErrorLogScreen
import com.trackingapp.presentation.screen.MainScreen
import com.trackingapp.presentation.screen.SettingsScreen
import com.trackingapp.presentation.screen.TrackDetailScreen
import com.trackingapp.presentation.screen.TracksListScreen
import com.trackingapp.presentation.viewmodel.MainViewModel

@Composable
fun TrackingNavigation(
    mainViewModel: MainViewModel,
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                viewModel = mainViewModel,
                onNavigateToTracks = {
                    navController.navigate("tracks")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        
        composable("tracks") {
            TracksListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTrackDetail = { trackId ->
                    navController.navigate("track_detail/$trackId")
                }
            )
        }
        
        composable("track_detail/{trackId}") { backStackEntry ->
            val trackId = backStackEntry.arguments?.getString("trackId") ?: ""
            TrackDetailScreen(
                trackId = trackId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToErrorLogs = {
                    navController.navigate("error_logs")
                }
            )
        }
        
        composable("error_logs") {
            ErrorLogScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}




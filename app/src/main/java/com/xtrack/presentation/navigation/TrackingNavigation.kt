package com.xtrack.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xtrack.presentation.screen.AddNoteScreen
import com.xtrack.presentation.screen.ErrorLogScreen
import com.xtrack.presentation.screen.ImportTrackScreen
import com.xtrack.presentation.screen.MainScreen
import com.xtrack.presentation.screen.NoteDetailScreen
import com.xtrack.presentation.screen.NotesListScreen
import com.xtrack.presentation.screen.NotesMapScreen
import com.xtrack.presentation.screen.SettingsScreen
import com.xtrack.presentation.screen.TrackDetailScreen
import com.xtrack.presentation.screen.TracksListScreen
import com.xtrack.presentation.viewmodel.MainViewModel

@Composable
fun TrackingNavigation(
    mainViewModel: MainViewModel,
    navController: NavHostController,
    onExitApp: () -> Unit = {},
    onKillApp: () -> Unit = {}
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
                },
                onNavigateToAddNote = { latitude, longitude ->
                    navController.navigate("add_note/$latitude/$longitude")
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
                },
                onNavigateToImport = {
                    navController.navigate("import_track")
                }
            )
        }
        
        composable("import_track") {
            ImportTrackScreen(
                onNavigateBack = {
                    navController.popBackStack()
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
                },
                onNavigateToNotesList = {
                    navController.navigate("notes_list")
                },
                onNavigateToNotesMap = {
                    navController.navigate("notes_map_center")
                },
                onExitApp = onExitApp,
                onKillApp = onKillApp
            )
        }
        
        composable("error_logs") {
            ErrorLogScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("notes_list") {
            NotesListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMap = {
                    navController.navigate("notes_map_center")
                },
                onNavigateToMapWithNote = { noteId ->
                    navController.navigate("notes_map_note/$noteId")
                },
                onNavigateToNoteDetail = { noteId ->
                    navController.navigate("note_detail/$noteId")
                }
            )
        }
        
        composable("notes_map") {
            NotesMapScreen(
                centerOnLastNote = false,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("notes_map_center") {
            NotesMapScreen(
                centerOnLastNote = true,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("notes_map_note/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            NotesMapScreen(
                centerOnNoteId = noteId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("note_detail/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            NoteDetailScreen(
                noteId = noteId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMap = {
                    navController.navigate("notes_map_note/$noteId")
                }
            )
        }
        
        composable("add_note/{latitude}/{longitude}") { backStackEntry ->
            val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull() ?: 0.0
            val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull() ?: 0.0
            val currentTrackId = mainViewModel.currentTrack.value?.id ?: ""
            
            AddNoteScreen(
                latitude = latitude,
                longitude = longitude,
                trackId = currentTrackId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}




package com.trackingapp.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackingapp.presentation.components.TrackItem
import com.trackingapp.presentation.viewmodel.TracksListViewModel
import com.trackingapp.utils.LocationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTrackDetail: (String) -> Unit,
    viewModel: TracksListViewModel = hiltViewModel()
) {
    val tracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Маршруты") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Сортировка")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = viewModel.searchQuery.collectAsStateWithLifecycle().value,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text("Поиск маршрутов") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Tracks list
            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет сохраненных маршрутов",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tracks) { track ->
                        TrackItem(
                            track = track,
                            onClick = { onNavigateToTrackDetail(track.id) },
                            onDelete = { viewModel.deleteTrack(track) }
                        )
                    }
                }
            }
        }
    }

    // Sort menu
    if (showSortMenu) {
        SortMenu(
            currentSort = viewModel.sortOrder.collectAsStateWithLifecycle().value,
            onSortSelected = { sort ->
                viewModel.updateSortOrder(sort)
                showSortMenu = false
            },
            onDismiss = { showSortMenu = false }
        )
    }
}

@Composable
private fun SortMenu(
    currentSort: TracksListViewModel.SortOrder,
    onSortSelected: (TracksListViewModel.SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сортировка") },
        text = {
            Column {
                TracksListViewModel.SortOrder.values().forEach { sort ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == sort,
                            onClick = { onSortSelected(sort) }
                        )
                        Text(
                            text = getSortOrderName(sort),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

private fun getSortOrderName(sort: TracksListViewModel.SortOrder): String {
    return when (sort) {
        TracksListViewModel.SortOrder.DATE_DESC -> "Дата (новые сначала)"
        TracksListViewModel.SortOrder.DATE_ASC -> "Дата (старые сначала)"
        TracksListViewModel.SortOrder.DISTANCE_DESC -> "Дистанция (больше сначала)"
        TracksListViewModel.SortOrder.DISTANCE_ASC -> "Дистанция (меньше сначала)"
        TracksListViewModel.SortOrder.DURATION_DESC -> "Длительность (больше сначала)"
        TracksListViewModel.SortOrder.DURATION_ASC -> "Длительность (меньше сначала)"
        TracksListViewModel.SortOrder.NAME_ASC -> "Название (А-Я)"
        TracksListViewModel.SortOrder.NAME_DESC -> "Название (Я-А)"
    }
}




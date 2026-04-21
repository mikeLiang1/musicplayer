package org.example.project.features.playlist.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.ui.theme.appColors
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: String,
    playlistViewModel: PlaylistViewModel = koinViewModel(
        parameters = { parametersOf(playlistId) }
    )
) {
    val state by playlistViewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Playlist") }, navigationIcon = {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
            })
        }
    ) {

        Text(state.playlistId, color = appColors.textPrimary, modifier = Modifier.padding(it))
    }


}


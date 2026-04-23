package org.example.project.features.playlist.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.project.ui.component.SongItem
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.appColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    state: PlaylistUiState,
    onBackPressed: () -> Unit
) {
    Scaffold(
        containerColor = appColors.backgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Playlist") }, navigationIcon = {
                    IconButton(onClick = onBackPressed) {

                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                    }
                })
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                state.playlist?.let { playlist ->
                    LazyColumn(contentPadding = it) {
                        items(playlist.songs) { song ->
                            SongItem(song = song, onClick = {})
                        }
                    }
                } ?: run {
                    Text("Playlist not found")
                }
            }
        }


    }
}

@DevicePreviews
@Composable
private fun PlaylistPreview() {
    AppPreview {
        PlaylistScreen(
            state = PlaylistUiState(playlistId = "Heavy Metal Mix"),
            onBackPressed = {}
        )
    }
}

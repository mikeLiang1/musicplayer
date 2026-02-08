package org.example.project.features.musicPlayer.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.compose.buttons.PlayPauseButton

@Composable
fun MusicPlayerBar(viewModel: MusicPlayerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Button(onClick = {viewModel.onPlayPressed()}) {
        Text("Click")
    }
}

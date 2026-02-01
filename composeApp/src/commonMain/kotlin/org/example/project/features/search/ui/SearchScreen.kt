package org.example.project.features.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.example.project.core.model.Song
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(searchViewModel: SearchViewModel = koinViewModel()) {
    val state by searchViewModel.uiState.collectAsStateWithLifecycle()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        TextField(
            value = state.searchQuery,
            onValueChange = { searchViewModel.onQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search on YouTube Music...") },
            trailingIcon = {
                IconButton(onClick = {
                    searchViewModel.onSuggestionClicked(state.searchQuery)
                }) {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            leadingIcon = {
                if (state.songList.isNotEmpty()) {
                    IconButton(onClick = {
                        searchViewModel.onBackPressed()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            }
        )

        // Results List
        LazyColumn {
            if (state.songList.isNotEmpty()) {
                items(state.songList) { song ->
                    SongItem(song) {
                        println("Selected: ${song.title}")
                    }
                }
            }
            else {
                items(state.suggestions) { suggestion ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            searchViewModel.onSuggestionClicked(suggestion)
                        }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(suggestion)
                    }
                }
            }
        }
    }
}


@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage( // From Coil library
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(song.title, maxLines = 1, fontWeight = FontWeight.Bold)
            Text(song.artist, style = MaterialTheme.typography.bodySmall)
        }
    }
}



package org.example.project.core.repository


import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.example.project.core.model.PlaybackState
import org.example.project.core.model.Song


class PlaybackRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val SONG_JSON = stringPreferencesKey("last_song_json")
        val POSITION = longPreferencesKey("last_position_ms")
        val IS_PLAYING = booleanPreferencesKey("is_playing")
    }

    // Combine all keys into a single PlaybackState Flow
    val playbackState: Flow<PlaybackState> = dataStore.data.map { prefs ->
        val songJson = prefs[Keys.SONG_JSON]
        PlaybackState(
            song = songJson?.let { Json.decodeFromString<Song>(it) },
            positionMs = prefs[Keys.POSITION] ?: 0L,
            isPlaying = prefs[Keys.IS_PLAYING] ?: false
        )
    }

    suspend fun saveSong(song: Song) {
        try {
            dataStore.edit { prefs ->
                prefs[Keys.SONG_JSON] = Json.encodeToString(song)
                prefs[Keys.POSITION] = 0L
            }
        } catch (e: Exception) {
            // Log the error (e.g., Firebase Crashlytics or Log.e)
            // But don't crash the app!
            Log.e("PlaybackRepo", "Failed to save song state", e)
        }
    }

    suspend fun savePosition(position: Long?) {
        position?.let {
            try {
                dataStore.edit { prefs ->
                    prefs[Keys.POSITION] = position
                }
            } catch (e: Exception) {
                Log.e("PlaybackRepo", "Failed to save position", e)
            }
        }
    }
}

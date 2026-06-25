package org.example.project

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import okhttp3.OkHttpClient
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.PlayerNavigator
import org.koin.android.ext.android.inject
import org.schabi.newpipe.extractor.NewPipe


class MainActivity : ComponentActivity() {
    private val musicPlayerManager by inject<MusicPlayerManager>()
    private val playerNavigator by inject<PlayerNavigator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        handleOpenPlayerIntent(intent)
        setContent {
            var isInitialized by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                NewPipe.init(getDownloader())
                isInitialized = true
            }

            if (isInitialized) {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenPlayerIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        musicPlayerManager.initialise()
        musicPlayerManager.onAppEnteredForeground()
    }

    override fun onStop() {
        musicPlayerManager.onAppEnteredBackground()
        super.onStop()

    }

    private fun handleOpenPlayerIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true) {
            playerNavigator.requestOpenPlayer()
        }
    }

    private fun getDownloader(): DownloaderImpl {
        return DownloaderImpl.init(OkHttpClient.Builder())

    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "open_player"
    }
}


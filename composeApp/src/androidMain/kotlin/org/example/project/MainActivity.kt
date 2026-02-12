package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.core.manager.MusicPlayerManager
import org.koin.android.ext.android.inject
import org.schabi.newpipe.extractor.NewPipe


class MainActivity : ComponentActivity() {
    private val musicPlayerManager by inject<MusicPlayerManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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

    override fun onStart() {
        super.onStart()
        musicPlayerManager.initialise()
    }

    private fun getDownloader(): DownloaderImpl {
        return DownloaderImpl.init(null)

    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

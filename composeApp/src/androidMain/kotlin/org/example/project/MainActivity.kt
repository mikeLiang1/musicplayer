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
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.NewPipe.getDownloader


class MainActivity : ComponentActivity() {
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

    private fun getDownloader(): DownloaderImpl {
        return DownloaderImpl.init(null)

    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

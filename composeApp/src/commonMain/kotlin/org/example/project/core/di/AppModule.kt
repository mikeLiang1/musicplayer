package org.example.project.core.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.example.project.core.database.MusicDatabase
import org.example.project.core.database.getRoomDatabase
import org.example.project.core.manager.PlayerNavigator
import org.example.project.core.manager.QueueManager
import org.example.project.core.repository.InnerTubeRepository
import org.example.project.core.repository.NewPipeRepository
import org.example.project.core.repository.PlaybackRepository
import org.example.project.core.repository.RecentlyPlayedRepository
import org.example.project.core.SpeechRecognizer
import org.example.project.core.usecase.PlaySongUseCase
import org.example.project.features.home.ui.HomeViewModel
import org.example.project.features.library.ui.LibraryViewModel
import org.example.project.features.musicPlayer.ui.MusicPlayerViewModel
import org.example.project.features.playlist.repository.PlaylistRepository
import org.example.project.features.playlist.ui.PlaylistViewModel
import org.example.project.features.search.ui.SearchViewModel
import org.example.project.features.songMenu.ui.SongMenuViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val repositoryModule = module {
    single { InnerTubeRepository(get()) }
    single { PlaybackRepository(get<MusicDatabase>().playbackDao()) }
    single { PlaylistRepository(get()) }
    single { QueueManager() }
    single { PlayerNavigator() }
    single { RecentlyPlayedRepository(get()) }
    single { NewPipeRepository() }
}

// 2. Infrastructure/Core Module (Threading & Scopes)
val coreModule = module {
    single<CoroutineDispatcher> { Dispatchers.IO }
    single {
        CoroutineScope(get<CoroutineDispatcher>() + SupervisorJob())
    }
}

val networkModule = module {
    single {
        HttpClient(OkHttp) {
            expectSuccess = true

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                })
            }

            // Enhanced network configuration for better performance
            engine {
                config {
                    // Connection pool settings for better connection reuse
                    connectionPool(
                        okhttp3.ConnectionPool(
                            10, // maxIdleConnections
                            5, // keepAliveDuration
                            java.util.concurrent.TimeUnit.MINUTES
                        )
                    )

                    // Timeout configurations
                    connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)

                    // Enable HTTP/2 for better performance
                    protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))

                    // Retry on connection failure
                    retryOnConnectionFailure(true)

                    // Cache configuration for better performance
                    cache(
                        okhttp3.Cache(
                            directory = java.io.File(System.getProperty("java.io.tmpdir"), "http_cache"),
                            maxSize = 50L * 1024L * 1024L // 50 MB
                        )
                    )

                }
            }

            // Request timeout configuration
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
                connectTimeoutMillis = 30000
                socketTimeoutMillis = 60000
            }

            defaultRequest {
                url("https://music.youtube.com/youtubei/v1/")
                // Add common headers for better compatibility
                header("Accept", "application/json")
                header("Accept-Language", "en-US,en;q=0.9")
                header("Cache-Control", "no-cache")
            }
        }
    }
}

// 3. Database Module (Platform specific)
val databaseModule = module {
    single<MusicDatabase> { getRoomDatabase(get()) }
}

val useCaseModule = module {
    single { PlaySongUseCase(get(), get()) }
}

// 4. ViewModels Module (UI Layer)
val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { SongMenuViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get(), getOrNull<SpeechRecognizer>()) }
    viewModel { LibraryViewModel(get(), get(), get()) }
    viewModel { MusicPlayerViewModel(get(), get(), get()) }
    viewModel { params ->
        PlaylistViewModel(
            params.get(),
            get(), get(), get(), get()
        )
    }
}

package org.example.project.core.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.example.project.core.database.MusicDatabase
import org.example.project.core.database.getRoomDatabase
import org.example.project.core.manager.QueueManager
import org.example.project.core.repository.InnerTubeRepository
import org.example.project.core.repository.NewPipeRepository
import org.example.project.core.repository.PlaybackRepository
import org.example.project.core.repository.RecentlyPlayedRepository
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
    single { PlaybackRepository(get()) }
    single { PlaylistRepository(get()) }
    single { QueueManager() }
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
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
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
    viewModel { SearchViewModel(get(), get(), get(), get()) }
    viewModel { LibraryViewModel(get(), get(), get()) }
    viewModel { MusicPlayerViewModel(get(), get(), get()) }
    viewModel { params ->
        PlaylistViewModel(
            params.get(),
            get(), get(), get(), get()
        )
    }
}

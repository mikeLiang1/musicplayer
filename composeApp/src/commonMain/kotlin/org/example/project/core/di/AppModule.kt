package org.example.project.core.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.example.project.core.dao.MusicDatabase
import org.example.project.core.dao.getRoomDatabase
import org.example.project.core.manager.QueueManager
import org.example.project.core.repository.SavedDataRepository
import org.example.project.core.repository.YouTubeRepository
import org.example.project.features.home.ui.HomeViewModel
import org.example.project.features.library.ui.LibraryViewModel
import org.example.project.features.musicPlayer.ui.MusicPlayerViewModel
import org.example.project.features.playlist.ui.PlaylistViewModel
import org.example.project.features.search.ui.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.schabi.newpipe.extractor.timeago.patterns.it


val repositoryModule = module {
    single { YouTubeRepository() }
    single { SavedDataRepository(get()) }
    single { QueueManager() }
}

// 2. Infrastructure/Core Module (Threading & Scopes)
val coreModule = module {
    single<CoroutineDispatcher> { Dispatchers.IO }
    single {
        CoroutineScope(get<CoroutineDispatcher>() + SupervisorJob())
    }
}

// 3. Database Module (Platform specific)
val databaseModule = module {
    single<MusicDatabase> { getRoomDatabase(get()) }
}

// 4. ViewModels Module (UI Layer)
val viewModelModule = module {
    viewModel { HomeViewModel(get()) }
    viewModel { SearchViewModel(get(), get(), get()) }
    viewModel { LibraryViewModel(get(), get(), get()) }
    viewModel { MusicPlayerViewModel(get(), get(), get(), get()) }
    viewModel { params -> PlaylistViewModel(params.get(),get(), get(), get()) }
}

package org.example.project.core.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.example.project.core.dao.MusicDatabase
import org.example.project.core.dao.getRoomDatabase
import org.example.project.core.manager.QueueManager
import org.example.project.core.repository.SavedDataRepository
import org.example.project.core.repository.YouTubeRepository
import org.example.project.features.home.ui.HomeViewModel
import org.example.project.features.musicPlayer.ui.MusicPlayerViewModel
import org.example.project.features.search.ui.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Repositories are usually pure Kotlin, so they stay in common
    single { YouTubeRepository() }

    single<CoroutineDispatcher> { Dispatchers.IO }

    // Queue Manager (pure Kotlin, no platform dependencies)
    single { QueueManager() }

    viewModel { HomeViewModel(get()) }
    viewModel { SearchViewModel(get(), get(), get()) }

    // Original MusicPlayerViewModel (for backward compatibility)
//    viewModel { MusicPlayerViewModel(get(), get()) }

    // New MusicPlayerViewModel with QueueManager support
    viewModel { MusicPlayerViewModel(get(), get(), get(), get()) }

    single<MusicDatabase> {
        getRoomDatabase(get())
    }

    single { SavedDataRepository(get()) }

    // CoroutineScope for background operations
    single {
        kotlinx.coroutines.CoroutineScope(get<kotlinx.coroutines.CoroutineDispatcher>() + kotlinx.coroutines.SupervisorJob())
    }


}

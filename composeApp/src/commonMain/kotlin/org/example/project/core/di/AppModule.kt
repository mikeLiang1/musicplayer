package org.example.project.core.di

import org.example.project.core.dao.MusicDatabase
import org.example.project.core.dao.getRoomDatabase
import org.example.project.core.repository.PlaybackRepository
import org.example.project.core.repository.YouTubeRepository
import org.example.project.features.home.ui.HomeViewModel
import org.example.project.features.musicPlayer.ui.MusicPlayerViewModel
import org.example.project.features.search.ui.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Repositories are usually pure Kotlin, so they stay in common
    single { YouTubeRepository() }


    viewModel { HomeViewModel(get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { MusicPlayerViewModel(get(), get()) }

    single<MusicDatabase> {
        getRoomDatabase(get())
    }

    single { PlaybackRepository(get()) }

}

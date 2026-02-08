package org.example.project.core.di

import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.MusicPlayerManagerImpl
import org.koin.dsl.module

val androidModule = module {

    single<MusicPlayerManager> {
        MusicPlayerManagerImpl(get())
    }

}

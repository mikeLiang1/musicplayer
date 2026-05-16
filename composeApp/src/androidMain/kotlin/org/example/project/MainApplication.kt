package org.example.project

import android.app.Application
import org.example.project.core.di.androidModule
import org.example.project.core.di.coreModule
import org.example.project.core.di.databaseModule
import org.example.project.core.di.repositoryModule
import org.example.project.core.di.useCaseModule
import org.example.project.core.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

open class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            modules(androidModule, viewModelModule, coreModule, repositoryModule, databaseModule, useCaseModule)
        }
    }
}

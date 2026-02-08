package org.example.project

import android.app.Application
import org.example.project.core.di.androidModule
import org.example.project.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

open class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            modules(appModule + androidModule)
        }
    }
}

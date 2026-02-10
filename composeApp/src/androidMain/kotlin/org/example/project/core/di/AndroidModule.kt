package org.example.project.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.example.project.core.helper.createDataStore
import org.example.project.core.helper.dataStoreFileName
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.MusicPlayerManagerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {

    single<MusicPlayerManager> {
        MusicPlayerManagerImpl(get(), get(), get())
    }

    single<DataStore<Preferences>> {
        createDataStore {
            androidContext().filesDir.resolve(dataStoreFileName).absolutePath
        }
    }

}

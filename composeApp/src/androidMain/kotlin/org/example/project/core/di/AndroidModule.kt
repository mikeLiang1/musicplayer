package org.example.project.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import org.example.project.core.database.MusicDatabase
import org.example.project.core.dao.getDatabaseBuilder
import org.example.project.core.helper.createDataStore
import org.example.project.core.helper.dataStoreFileName
import org.example.project.core.SpeechRecognizer
import org.example.project.core.SpeechRecognizerImpl
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.MusicPlayerManagerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {

    single<RoomDatabase.Builder<MusicDatabase>> {
        getDatabaseBuilder(get())
    }

    // Updated MusicPlayerManagerImpl with SavedDataRepository for position restoration
    single<MusicPlayerManager> {
        MusicPlayerManagerImpl(get())
    }

    single<DataStore<Preferences>> {
        createDataStore {
            androidContext().filesDir.resolve(dataStoreFileName).absolutePath
        }
    }

    single<SpeechRecognizer> {
        SpeechRecognizerImpl(androidContext())
    }

}

package org.example.project.core.di

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import org.example.project.core.manager.MusicPlayerManager
import org.example.project.core.manager.MusicPlayerManagerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {

    single { ExoPlayer.Builder(androidContext()).build() }

    single<MusicPlayerManager> {
        MusicPlayerManagerImpl(get(), get<ExoPlayer>())
    }


    // 2. GIVE IT TO THE MANAGER (Logic)
    // PlayerManager uses it to obey the user.

    // 3. GIVE IT TO THE SESSION (OS)
    // MediaSession uses it to update the Lock Screen.
    single<MediaSession> {
        MediaSession.Builder(androidContext(), get<ExoPlayer>()).build()
    }

}

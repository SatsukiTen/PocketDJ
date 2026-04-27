package com.djapp.data.di

import android.content.Context
import androidx.room.Room
import com.djapp.data.db.AppDatabase
import com.djapp.data.db.BpmCacheDao
import com.djapp.data.repository.AudioEngineRepositoryImpl
import com.djapp.data.repository.BpmCacheRepositoryImpl
import com.djapp.data.repository.SettingsRepositoryImpl
import com.djapp.data.repository.TrackRepositoryImpl
import com.djapp.domain.repository.AudioEngineRepository
import com.djapp.domain.repository.BpmCacheRepository
import com.djapp.domain.repository.SettingsRepository
import com.djapp.domain.repository.TrackRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds @Singleton
    abstract fun bindAudioEngineRepository(impl: AudioEngineRepositoryImpl): AudioEngineRepository

    @Binds @Singleton
    abstract fun bindTrackRepository(impl: TrackRepositoryImpl): TrackRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindBpmCacheRepository(impl: BpmCacheRepositoryImpl): BpmCacheRepository

    companion object {

        @Provides @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "djapp.db")
                .fallbackToDestructiveMigration()
                .build()

        @Provides @Singleton
        fun provideBpmCacheDao(db: AppDatabase): BpmCacheDao = db.bpmCacheDao()
    }
}

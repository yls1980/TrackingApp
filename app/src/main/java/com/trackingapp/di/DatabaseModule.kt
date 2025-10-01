package com.trackingapp.di

import android.content.Context
import androidx.room.Room
import com.trackingapp.data.database.TrackingDatabase
import com.trackingapp.data.database.AppSettingsDao
import com.trackingapp.data.database.TrackDao
import com.trackingapp.data.database.TrackPointDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTrackingDatabase(@ApplicationContext context: Context): TrackingDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            TrackingDatabase::class.java,
            "tracking_database"
        ).build()
    }

    @Provides
    fun provideTrackDao(database: TrackingDatabase): TrackDao {
        return database.trackDao()
    }

    @Provides
    fun provideTrackPointDao(database: TrackingDatabase): TrackPointDao {
        return database.trackPointDao()
    }

    @Provides
    fun provideAppSettingsDao(database: TrackingDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }
}


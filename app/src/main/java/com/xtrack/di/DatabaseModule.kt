package com.xtrack.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xtrack.data.database.TrackingDatabase
import com.xtrack.data.database.AppSettingsDao
import com.xtrack.data.database.BufferedTrackPointDao
import com.xtrack.data.database.LastLocationDao
import com.xtrack.data.database.MapNoteDao
import com.xtrack.data.database.TrackDao
import com.xtrack.data.database.TrackPointDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Создаем таблицу map_notes
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `map_notes` (
                    `id` TEXT NOT NULL,
                    `trackId` TEXT NOT NULL,
                    `latitude` REAL NOT NULL,
                    `longitude` REAL NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `timestamp` TEXT NOT NULL,
                    `noteType` TEXT NOT NULL,
                    `mediaPath` TEXT,
                    `mediaType` TEXT,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Создаем таблицу last_location
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `last_location` (
                    `id` INTEGER NOT NULL,
                    `latitude` REAL NOT NULL,
                    `longitude` REAL NOT NULL,
                    `timestamp` TEXT NOT NULL,
                    `accuracy` REAL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Добавляем поле wasRecordingOnExit
            database.execSQL("ALTER TABLE app_settings ADD COLUMN wasRecordingOnExit INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Создаем таблицу буферизованных точек
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS buffered_track_points (
                    id TEXT PRIMARY KEY NOT NULL,
                    trackId TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    altitude REAL,
                    speed REAL,
                    bearing REAL,
                    accuracy REAL NOT NULL,
                    isSynced INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    @Provides
    @Singleton
    fun provideTrackingDatabase(@ApplicationContext context: Context): TrackingDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            TrackingDatabase::class.java,
            "tracking_database"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .fallbackToDestructiveMigration() // На случай проблем с миграцией
        .build()
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

    @Provides
    fun provideMapNoteDao(database: TrackingDatabase): MapNoteDao {
        return database.mapNoteDao()
    }

    @Provides
    fun provideLastLocationDao(database: TrackingDatabase): LastLocationDao {
        return database.lastLocationDao()
    }

    @Provides
    fun provideBufferedTrackPointDao(database: TrackingDatabase): BufferedTrackPointDao {
        return database.bufferedTrackPointDao()
    }
}


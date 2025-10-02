package com.xtrack.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.xtrack.data.model.AppSettings
import com.xtrack.data.model.BufferedTrackPoint
import com.xtrack.data.model.LastLocation
import com.xtrack.data.model.MapNote
import com.xtrack.data.model.Track
import com.xtrack.data.model.TrackPoint

@Database(
    entities = [Track::class, TrackPoint::class, AppSettings::class, MapNote::class, LastLocation::class, BufferedTrackPoint::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrackingDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun mapNoteDao(): MapNoteDao
    abstract fun lastLocationDao(): LastLocationDao
    abstract fun bufferedTrackPointDao(): BufferedTrackPointDao

    companion object {
        @Volatile
        private var INSTANCE: TrackingDatabase? = null

        // Миграция с версии 3 на 4 - добавляем поле wasRecordingOnExit
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE app_settings ADD COLUMN wasRecordingOnExit INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Миграция с версии 4 на 5 - добавляем таблицу буферизованных точек
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
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

        fun getDatabase(context: Context): TrackingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackingDatabase::class.java,
                    "tracking_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration() // Fallback на случай других проблем
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Временная функция для очистки базы данных при проблемах с миграцией
        fun clearDatabase(context: Context) {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                context.deleteDatabase("tracking_database")
            }
        }
    }
}


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
    version = 11,
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

        // Миграция с версии 5 на 6 - добавляем поля для уведомлений о расстоянии
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE app_settings ADD COLUMN distanceNotificationIntervalMeters INTEGER NOT NULL DEFAULT 1000")
                database.execSQL("ALTER TABLE app_settings ADD COLUMN distanceNotificationsEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Миграция с версии 6 на 7 - исправляем проблемы с целостностью данных
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Пересоздаем таблицу настроек для исправления проблем с целостностью
                database.execSQL("DROP TABLE IF EXISTS app_settings_backup")
                database.execSQL("CREATE TABLE app_settings_backup AS SELECT * FROM app_settings")
                database.execSQL("DROP TABLE app_settings")
                database.execSQL("""
                    CREATE TABLE app_settings (
                        id INTEGER PRIMARY KEY NOT NULL,
                        locationAccuracy TEXT NOT NULL DEFAULT 'BALANCED',
                        locationIntervalMs INTEGER NOT NULL DEFAULT 5000,
                        minDistanceMeters REAL NOT NULL DEFAULT 10.0,
                        accuracyThresholdMeters REAL NOT NULL DEFAULT 50.0,
                        autoPauseEnabled INTEGER NOT NULL DEFAULT 0,
                        autoPauseSpeedThreshold REAL NOT NULL DEFAULT 1.0,
                        autoPauseDurationSec INTEGER NOT NULL DEFAULT 30,
                        defaultExportFormat TEXT NOT NULL DEFAULT 'GPX',
                        wasRecordingOnExit INTEGER NOT NULL DEFAULT 0,
                        distanceNotificationIntervalMeters INTEGER NOT NULL DEFAULT 1000,
                        distanceNotificationsEnabled INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("INSERT INTO app_settings SELECT * FROM app_settings_backup")
                database.execSQL("DROP TABLE app_settings_backup")
            }
        }

        // Миграция с версии 7 на 8 - добавляем поле elevationGainMeters в таблицу tracks
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tracks ADD COLUMN elevationGainMeters REAL NOT NULL DEFAULT 0.0")
            }
        }

        // Миграция с версии 8 на 9 - обновление хэша схемы после внутренних изменений
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Структура базы данных не изменилась, но обновляем версию
                // чтобы исправить несоответствие хэшей схемы
            }
        }

        // Миграция с версии 9 на 10 - добавлены TypeConverters для LocationAccuracy и ExportFormat
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Структура базы данных не изменилась
                // Добавлены TypeConverters для правильного сохранения enum полей
                // locationAccuracy и defaultExportFormat в таблице app_settings
                // Существующие данные остаются валидными, так как enum уже сохранялись как строки
            }
        }

        // Миграция с версии 10 на 11 - заменяем wasRecordingOnExit на appExitState
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Создаем резервную копию настроек
                database.execSQL("DROP TABLE IF EXISTS app_settings_backup")
                database.execSQL("CREATE TABLE app_settings_backup AS SELECT * FROM app_settings")
                
                // Удаляем старую таблицу
                database.execSQL("DROP TABLE app_settings")
                
                // Создаем новую таблицу с appExitState
                database.execSQL("""
                    CREATE TABLE app_settings (
                        id INTEGER PRIMARY KEY NOT NULL,
                        locationAccuracy TEXT NOT NULL DEFAULT 'BALANCED',
                        locationIntervalMs INTEGER NOT NULL DEFAULT 5000,
                        minDistanceMeters REAL NOT NULL DEFAULT 10.0,
                        accuracyThresholdMeters REAL NOT NULL DEFAULT 50.0,
                        autoPauseEnabled INTEGER NOT NULL DEFAULT 0,
                        autoPauseSpeedThreshold REAL NOT NULL DEFAULT 1.0,
                        autoPauseDurationSec INTEGER NOT NULL DEFAULT 30,
                        defaultExportFormat TEXT NOT NULL DEFAULT 'GPX',
                        appExitState TEXT NOT NULL DEFAULT 'STOPPED',
                        distanceNotificationIntervalMeters INTEGER NOT NULL DEFAULT 1000,
                        distanceNotificationsEnabled INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // Переносим данные из резервной копии, преобразуя wasRecordingOnExit в appExitState
                database.execSQL("""
                    INSERT INTO app_settings (
                        id, locationAccuracy, locationIntervalMs, minDistanceMeters, 
                        accuracyThresholdMeters, autoPauseEnabled, autoPauseSpeedThreshold, 
                        autoPauseDurationSec, defaultExportFormat, appExitState, 
                        distanceNotificationIntervalMeters, distanceNotificationsEnabled
                    ) 
                    SELECT 
                        id, locationAccuracy, locationIntervalMs, minDistanceMeters, 
                        accuracyThresholdMeters, autoPauseEnabled, autoPauseSpeedThreshold, 
                        autoPauseDurationSec, defaultExportFormat,
                        CASE 
                            WHEN wasRecordingOnExit = 1 THEN 'RECORDING'
                            ELSE 'STOPPED'
                        END,
                        distanceNotificationIntervalMeters, distanceNotificationsEnabled
                    FROM app_settings_backup
                """.trimIndent())
                
                // Удаляем резервную копию
                database.execSQL("DROP TABLE app_settings_backup")
            }
        }

        fun getDatabase(context: Context): TrackingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackingDatabase::class.java,
                    "tracking_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .fallbackToDestructiveMigration() // Fallback на случай других проблем
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


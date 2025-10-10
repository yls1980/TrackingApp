package com.xtrack.data.database

import androidx.room.TypeConverter
import com.xtrack.data.model.AppExitState
import com.xtrack.data.model.ExportFormat
import com.xtrack.data.model.LocationAccuracy
import com.xtrack.data.model.MediaType
import com.xtrack.data.model.NoteType
import kotlinx.datetime.Instant

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toInstant(value: String?): Instant? {
        return value?.let { Instant.parse(it) }
    }

    @TypeConverter
    fun fromNoteType(value: NoteType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toNoteType(value: String?): NoteType? {
        return value?.let { NoteType.valueOf(it) }
    }

    @TypeConverter
    fun fromMediaType(value: MediaType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toMediaType(value: String?): MediaType? {
        return value?.let { MediaType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromLocationAccuracy(value: LocationAccuracy?): String? {
        return value?.name
    }

    @TypeConverter
    fun toLocationAccuracy(value: String?): LocationAccuracy? {
        return value?.let { LocationAccuracy.valueOf(it) }
    }
    
    @TypeConverter
    fun fromExportFormat(value: ExportFormat?): String? {
        return value?.name
    }

    @TypeConverter
    fun toExportFormat(value: String?): ExportFormat? {
        return value?.let { ExportFormat.valueOf(it) }
    }
    
    @TypeConverter
    fun fromAppExitState(value: AppExitState?): String? {
        return value?.name
    }

    @TypeConverter
    fun toAppExitState(value: String?): AppExitState? {
        return value?.let { AppExitState.valueOf(it) }
    }
}


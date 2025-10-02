package com.xtrack.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.xtrack.data.parser.GeoJsonGenerator
import com.xtrack.data.parser.GeoJsonParser
import com.xtrack.data.parser.GpxGenerator
import com.xtrack.data.parser.GpxParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setPrettyPrinting()
            .create()
    }

    @Provides
    @Singleton
    fun provideGpxParser(): GpxParser = GpxParser()

    @Provides
    @Singleton
    fun provideGpxGenerator(): GpxGenerator = GpxGenerator()

    @Provides
    @Singleton
    fun provideGeoJsonParser(gson: Gson): GeoJsonParser = GeoJsonParser(gson)

    @Provides
    @Singleton
    fun provideGeoJsonGenerator(): GeoJsonGenerator = GeoJsonGenerator()
}






package com.example.notepadforcompanycomposeui.di

import android.content.Context
import com.example.notepadforcompanycomposeui.data.AppDatabase
import com.example.notepadforcompanycomposeui.data.dao.DateDao
import com.example.notepadforcompanycomposeui.data.dao.FirebaseDao
import com.example.notepadforcompanycomposeui.data.dao.NotesByDateDao
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideDateDao(database: AppDatabase): DateDao {
        return database.dateDao()
    }

    @Provides
    @Singleton
    fun provideNotesByDateDao(database: AppDatabase): NotesByDateDao {
        return database.notesByDateDao()
    }

    @Provides
    @Singleton
    fun provideFirebaseDao(database: AppDatabase): FirebaseDao {
        return database.firebaseDao()
    }
}
package com.pokeapp.di

import android.content.Context
import androidx.room.Room
import com.pokeapp.data.local.CollectionDao
import com.pokeapp.data.local.PokeCollectionDao
import com.pokeapp.data.local.PokeDatabase
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
    fun providePokeDatabase(@ApplicationContext context: Context): PokeDatabase =
        Room.databaseBuilder(context, PokeDatabase::class.java, "poke-database")
            // Pre-release app, no real user data to preserve yet — simpler than
            // writing migrations for every schema tweak during active development.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideCollectionDao(database: PokeDatabase): CollectionDao = database.collectionDao()

    @Provides
    @Singleton
    fun providePokeCollectionDao(database: PokeDatabase): PokeCollectionDao = database.pokeCollectionDao()
}

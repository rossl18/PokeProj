package com.pokeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CollectionEntryEntity::class, PokeCollectionEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class PokeDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun pokeCollectionDao(): PokeCollectionDao
}

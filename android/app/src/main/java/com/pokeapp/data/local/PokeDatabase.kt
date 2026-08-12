package com.pokeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CollectionEntryEntity::class], version = 2, exportSchema = false)
abstract class PokeDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
}

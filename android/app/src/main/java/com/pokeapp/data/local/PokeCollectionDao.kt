package com.pokeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PokeCollectionDao {
    @Query("SELECT * FROM poke_collections ORDER BY createdAt ASC")
    fun getAll(): Flow<List<PokeCollectionEntity>>

    @Query("SELECT * FROM poke_collections ORDER BY createdAt ASC")
    suspend fun getAllSnapshot(): List<PokeCollectionEntity>

    @Insert
    suspend fun insert(collection: PokeCollectionEntity): Long

    @Update
    suspend fun update(collection: PokeCollectionEntity)

    @Query("DELETE FROM poke_collections WHERE id = :id")
    suspend fun delete(id: Long)
}

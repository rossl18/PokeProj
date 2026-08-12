package com.pokeapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collection_entries WHERE collectionId = :collectionId ORDER BY dateAdded DESC")
    fun getAllInCollection(collectionId: Long): Flow<List<CollectionEntryEntity>>

    /** Every owned card across every collection — used to batch-refresh prices in one call. */
    @Query("SELECT * FROM collection_entries")
    suspend fun getAllEntriesSnapshot(): List<CollectionEntryEntity>

    @Query("SELECT * FROM collection_entries WHERE collectionId = :collectionId AND cardId = :cardId AND variant = :variant LIMIT 1")
    suspend fun getByCardAndVariant(collectionId: Long, cardId: String, variant: String): CollectionEntryEntity?

    @Insert
    suspend fun insert(entry: CollectionEntryEntity): Long

    @Update
    suspend fun update(entry: CollectionEntryEntity)

    @Query("UPDATE collection_entries SET lastKnownPriceCents = :priceCents, lastPriceUpdatedAt = :updatedAt WHERE cardId = :cardId AND variant = :variant")
    suspend fun updateLastKnownPrice(cardId: String, variant: String, priceCents: Long, updatedAt: Long)

    @Delete
    suspend fun delete(entry: CollectionEntryEntity)

    @Query("DELETE FROM collection_entries WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM collection_entries WHERE collectionId = :collectionId")
    suspend fun deleteAllInCollection(collectionId: Long)
}

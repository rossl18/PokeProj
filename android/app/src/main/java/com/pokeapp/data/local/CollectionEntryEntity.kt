package com.pokeapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collection_entries",
    indices = [Index(value = ["collectionId", "cardId", "variant"], unique = true)],
)
data class CollectionEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: Long,
    val cardId: String,
    val variant: String,
    val cardName: String,
    val imageUrl: String?,
    val setName: String? = null,
    val cardNumber: String? = null,
    val quantity: Int,
    val dateAdded: Long,
    val lastKnownPriceCents: Long? = null,
    val lastPriceUpdatedAt: Long? = null,
)

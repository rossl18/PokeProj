package com.pokeapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "poke_collections")
data class PokeCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

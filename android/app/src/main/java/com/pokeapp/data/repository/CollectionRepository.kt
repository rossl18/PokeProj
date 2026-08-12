package com.pokeapp.data.repository

import com.pokeapp.data.local.CollectionDao
import com.pokeapp.data.local.CollectionEntryEntity
import com.pokeapp.data.local.PokeCollectionDao
import com.pokeapp.data.local.PokeCollectionEntity
import com.pokeapp.data.remote.PokeApi
import com.pokeapp.data.remote.dto.BatchPriceRequestDto
import com.pokeapp.data.remote.dto.CardVariantKeyDto
import com.pokeapp.domain.model.Card
import com.pokeapp.domain.model.CollectionItem
import com.pokeapp.domain.model.UserCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private data class CardKey(val cardId: String, val variant: String)

private const val MIN_REFRESH_INTERVAL_MS = 60_000L
private const val DEFAULT_COLLECTION_NAME = "My Collection"

@Singleton
class CollectionRepository @Inject constructor(
    private val dao: CollectionDao,
    private val collectionsDao: PokeCollectionDao,
    private val api: PokeApi,
) {
    private val priceCache = MutableStateFlow<Map<CardKey, Double?>>(emptyMap())
    private var lastRefreshedAt = 0L

    private val _selectedCollectionId = MutableStateFlow<Long?>(null)
    /** The collection currently shown in the Collection tab; also the default target when adding a card from elsewhere. */
    val selectedCollectionId: Flow<Long?> = _selectedCollectionId

    val collections: Flow<List<UserCollection>> = collectionsDao.getAll().map { list ->
        list.map { UserCollection(it.id, it.name) }
    }

    /** Ensures at least one collection exists and a selection is active. Safe to call repeatedly. */
    suspend fun ensureInitialized() {
        var all = collectionsDao.getAllSnapshot()
        if (all.isEmpty()) {
            collectionsDao.insert(PokeCollectionEntity(name = DEFAULT_COLLECTION_NAME, createdAt = System.currentTimeMillis()))
            all = collectionsDao.getAllSnapshot()
        }
        if (_selectedCollectionId.value == null) {
            _selectedCollectionId.value = all.first().id
        }
    }

    fun selectCollection(id: Long) {
        _selectedCollectionId.value = id
    }

    suspend fun createCollection(name: String): Long {
        val id = collectionsDao.insert(PokeCollectionEntity(name = name, createdAt = System.currentTimeMillis()))
        _selectedCollectionId.value = id
        return id
    }

    suspend fun deleteCollection(id: Long) {
        val remaining = collectionsDao.getAllSnapshot().filter { it.id != id }
        if (remaining.isEmpty()) return // always keep at least one collection

        dao.deleteAllInCollection(id)
        collectionsDao.delete(id)
        if (_selectedCollectionId.value == id) {
            _selectedCollectionId.value = remaining.first().id
        }
    }

    fun items(collectionId: Long): Flow<List<CollectionItem>> =
        combine(dao.getAllInCollection(collectionId), priceCache) { entries, prices ->
            entries.map { entry ->
                val livePrice = prices[CardKey(entry.cardId, entry.variant)]
                CollectionItem(
                    id = entry.id,
                    cardId = entry.cardId,
                    variant = entry.variant,
                    cardName = entry.cardName,
                    imageUrl = entry.imageUrl,
                    setName = entry.setName,
                    cardNumber = entry.cardNumber,
                    quantity = entry.quantity,
                    dateAdded = entry.dateAdded,
                    currentPrice = livePrice,
                    lastKnownPrice = entry.lastKnownPriceCents?.let { it / 100.0 },
                )
            }
        }

    /**
     * Adds a new owned card to the given collection, or increments quantity if
     * this card/variant is already in that collection (matches how TCGPlayer's
     * own app behaves).
     */
    suspend fun addOrIncrement(card: Card, quantity: Int, collectionId: Long) {
        val existing = dao.getByCardAndVariant(collectionId, card.cardId, card.variant)
        if (existing != null) {
            dao.update(existing.copy(quantity = existing.quantity + quantity))
        } else {
            dao.insert(
                CollectionEntryEntity(
                    collectionId = collectionId,
                    cardId = card.cardId,
                    variant = card.variant,
                    cardName = card.cardName,
                    imageUrl = card.imageUrl,
                    setName = card.setName,
                    cardNumber = card.cardNumber,
                    quantity = quantity,
                    dateAdded = System.currentTimeMillis(),
                )
            )
        }
    }

    suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    /**
     * Refreshes prices for everything currently owned, across every collection,
     * via one batched call. No-ops if the last refresh was recent, since server
     * prices only change hourly.
     */
    suspend fun refreshPrices(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshedAt < MIN_REFRESH_INTERVAL_MS) return

        val entries = dao.getAllEntriesSnapshot()
        if (entries.isEmpty()) return

        val keys = entries.map { CardKey(it.cardId, it.variant) }.distinct()
        val results = api.batchPrices(
            BatchPriceRequestDto(keys.map { CardVariantKeyDto(it.cardId, it.variant) })
        )

        val updated = priceCache.value.toMutableMap()
        for (result in results) {
            val key = CardKey(result.cardId, result.variant)
            updated[key] = result.marketPrice
            result.marketPrice?.let { price ->
                dao.updateLastKnownPrice(result.cardId, result.variant, (price * 100).toLong(), now)
            }
        }
        priceCache.value = updated
        lastRefreshedAt = now
    }
}

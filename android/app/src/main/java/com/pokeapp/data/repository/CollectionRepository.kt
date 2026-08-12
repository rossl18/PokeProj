package com.pokeapp.data.repository

import com.pokeapp.data.local.CollectionDao
import com.pokeapp.data.local.CollectionEntryEntity
import com.pokeapp.data.remote.PokeApi
import com.pokeapp.data.remote.dto.BatchPriceRequestDto
import com.pokeapp.data.remote.dto.CardVariantKeyDto
import com.pokeapp.domain.model.Card
import com.pokeapp.domain.model.CollectionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private data class CardKey(val cardId: String, val variant: String)

private const val MIN_REFRESH_INTERVAL_MS = 60_000L

@Singleton
class CollectionRepository @Inject constructor(
    private val dao: CollectionDao,
    private val api: PokeApi,
) {
    private val priceCache = MutableStateFlow<Map<CardKey, Double?>>(emptyMap())
    private var lastRefreshedAt = 0L

    val items: Flow<List<CollectionItem>> = combine(dao.getAll(), priceCache) { entries, prices ->
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
     * Adds a new owned card, or increments quantity if this card/variant is
     * already in the collection (matches how TCGPlayer's own app behaves).
     */
    suspend fun addOrIncrement(card: Card, quantity: Int) {
        val existing = dao.getByCardAndVariant(card.cardId, card.variant)
        if (existing != null) {
            dao.update(existing.copy(quantity = existing.quantity + quantity))
        } else {
            dao.insert(
                CollectionEntryEntity(
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
     * Refreshes prices for everything currently owned via one batched call.
     * No-ops if the last refresh was recent, since server prices only change hourly.
     */
    suspend fun refreshPrices(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshedAt < MIN_REFRESH_INTERVAL_MS) return

        val entries = dao.getAll().first()
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

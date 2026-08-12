package com.pokeapp.data.repository

import com.pokeapp.data.remote.PokeApi
import com.pokeapp.data.remote.dto.CardDto
import com.pokeapp.domain.model.Card
import com.pokeapp.domain.model.PricePoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val api: PokeApi,
) {
    suspend fun search(query: String?, limit: Int = 50): List<Card> =
        api.searchCards(search = query, limit = limit).map { it.toDomain() }

    suspend fun getVariants(cardId: String): List<Card> =
        api.getCardVariants(cardId).map { it.toDomain() }

    suspend fun getHistory(cardId: String, variant: String?): List<PricePoint> =
        api.getHistory(cardId, variant).map {
            PricePoint(fetchedAt = it.fetchedAt, marketPrice = it.marketPrice)
        }
}

fun CardDto.toDomain() = Card(
    cardId = cardId,
    variant = variant,
    cardName = cardName,
    marketPrice = marketPrice,
    lowPrice = lowPrice,
    midPrice = midPrice,
    highPrice = highPrice,
    imageUrl = imageUrl,
    setName = setName,
    cardNumber = cardNumber,
)

package com.pokeapp.domain.model

data class CollectionItem(
    val id: Long,
    val cardId: String,
    val variant: String,
    val cardName: String,
    val imageUrl: String?,
    val setName: String?,
    val cardNumber: String?,
    val quantity: Int,
    val dateAdded: Long,
    val currentPrice: Double?,
    val lastKnownPrice: Double?,
) {
    val effectivePrice: Double? get() = currentPrice ?: lastKnownPrice
    val isStale: Boolean get() = currentPrice == null
    val lineTotal: Double get() = (effectivePrice ?: 0.0) * quantity

    val setLabel: String?
        get() = when {
            setName != null && cardNumber != null -> "$setName #$cardNumber"
            setName != null -> setName
            cardNumber != null -> "#$cardNumber"
            else -> null
        }
}

enum class CollectionSortOption {
    NAME,
    PRICE_ASC,
    PRICE_DESC,
    LINE_TOTAL_ASC,
    LINE_TOTAL_DESC,
    QUANTITY,
    RECENTLY_ADDED,
}

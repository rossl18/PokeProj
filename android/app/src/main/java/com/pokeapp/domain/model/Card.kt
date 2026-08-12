package com.pokeapp.domain.model

data class Card(
    val cardId: String,
    val variant: String,
    val cardName: String,
    val marketPrice: Double?,
    val lowPrice: Double?,
    val midPrice: Double?,
    val highPrice: Double?,
    val imageUrl: String?,
    val setName: String?,
    val cardNumber: String?,
) {
    val thumbnailUrl: String? get() = imageUrl?.let { "$it/low.webp" }
    val fullImageUrl: String? get() = imageUrl?.let { "$it/high.webp" }

    /** e.g. "Phantom Forces #117" — falls back gracefully if either part is missing. */
    val setLabel: String?
        get() = when {
            setName != null && cardNumber != null -> "$setName #$cardNumber"
            setName != null -> setName
            cardNumber != null -> "#$cardNumber"
            else -> null
        }
}

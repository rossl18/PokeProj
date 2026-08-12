package com.pokeapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CardVariantKeyDto(
    @Json(name = "card_id") val cardId: String,
    val variant: String,
)

@JsonClass(generateAdapter = true)
data class BatchPriceRequestDto(
    val items: List<CardVariantKeyDto>,
)

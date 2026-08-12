package com.pokeapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CardDto(
    @Json(name = "card_id") val cardId: String,
    val variant: String,
    @Json(name = "card_name") val cardName: String,
    @Json(name = "market_price") val marketPrice: Double?,
    @Json(name = "low_price") val lowPrice: Double?,
    @Json(name = "mid_price") val midPrice: Double?,
    @Json(name = "high_price") val highPrice: Double?,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "set_name") val setName: String?,
    @Json(name = "card_number") val cardNumber: String?,
    @Json(name = "updated_at") val updatedAt: String?,
)

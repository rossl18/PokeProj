package com.pokeapp.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CardHistoryPointDto(
    @Json(name = "card_id") val cardId: String,
    @Json(name = "card_name") val cardName: String,
    val variant: String,
    @Json(name = "market_price") val marketPrice: Double?,
    @Json(name = "low_price") val lowPrice: Double?,
    @Json(name = "mid_price") val midPrice: Double?,
    @Json(name = "high_price") val highPrice: Double?,
    @Json(name = "fetched_at") val fetchedAt: String,
)

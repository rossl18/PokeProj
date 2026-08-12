package com.pokeapp.data.remote

import com.pokeapp.data.remote.dto.BatchPriceRequestDto
import com.pokeapp.data.remote.dto.CardDto
import com.pokeapp.data.remote.dto.CardHistoryPointDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PokeApi {
    @GET("cards")
    suspend fun searchCards(
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 50,
    ): List<CardDto>

    @GET("cards/{cardId}")
    suspend fun getCardVariants(@Path("cardId") cardId: String): List<CardDto>

    @GET("cards/{cardId}/history")
    suspend fun getHistory(
        @Path("cardId") cardId: String,
        @Query("variant") variant: String? = null,
    ): List<CardHistoryPointDto>

    @POST("cards/batch")
    suspend fun batchPrices(@Body request: BatchPriceRequestDto): List<CardDto>
}

package com.cryptowallet.data.remote.api

import com.cryptowallet.data.remote.dto.CoinGeckoCoin
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinGeckoApi {

    @GET("markets")
    suspend fun getAllCoins(
        @Query("vs_currency") vsCurrency: String = "usd"
    ): List<CoinGeckoCoin>

    @GET("markets")
    suspend fun getCoinById(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("ids") ids: String = "bitcoin"
    ): List<CoinGeckoCoin>

    @GET("{id}/ohlc")
    suspend fun getCoinOhlcGraph(
        @Path("id") id: String = "bitcoin",
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("days") days: String = "1"
    ): List<List<Double>>
}
package com.cryptowallet.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApi {
    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 100
    ): List<List<Any>>
}

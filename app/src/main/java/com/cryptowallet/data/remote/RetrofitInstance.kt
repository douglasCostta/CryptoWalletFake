package com.cryptowallet.data.remote

import com.cryptowallet.data.remote.api.CoinGeckoApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = "https://api.coingecko.com/api/v3/coins/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: CoinGeckoApi = retrofit.create(CoinGeckoApi::class.java)
}
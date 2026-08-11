package com.cryptowallet.data.remote

import com.cryptowallet.data.remote.api.CoinGeckoApi
import com.cryptowallet.data.remote.api.BinanceApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3/coins/"
    private const val BINANCE_BASE_URL = "https://api.binance.com/"
    private val gsonConverter = GsonConverterFactory.create()

    private val coinGeckoRetrofit = Retrofit.Builder()
        .baseUrl(COINGECKO_BASE_URL)
        .addConverterFactory(gsonConverter)
        .build()

    private val binanceRetrofit = Retrofit.Builder()
        .baseUrl(BINANCE_BASE_URL)
        .addConverterFactory(gsonConverter)
        .build()

    val coinGeckoApi: CoinGeckoApi = coinGeckoRetrofit.create(CoinGeckoApi::class.java)
    val binanceApi: BinanceApi = binanceRetrofit.create(BinanceApi::class.java)

    val api: CoinGeckoApi get() = coinGeckoApi
}
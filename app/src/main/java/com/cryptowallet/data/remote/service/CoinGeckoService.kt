package com.cryptowallet.data.remote.service

import com.cryptowallet.data.remote.api.CoinGeckoApi

class CoinGeckoService (private val api: CoinGeckoApi) {

    suspend fun getAllCoins(
        vsCurrency: String = "usd"
    ) = api.getAllCoins(vsCurrency)

    suspend fun getCoinById(
        vsCurrency: String = "usd",
        ids: String = "bitcoin"
    ) = api.getCoinById(vsCurrency, ids)

    suspend fun getCoinOhlcGraph(
        id: String = "bitcoin",
        vsCurrency: String = "usd",
        days: String = "1"
    ) = api.getCoinOhlcGraph(id, vsCurrency, days)
}
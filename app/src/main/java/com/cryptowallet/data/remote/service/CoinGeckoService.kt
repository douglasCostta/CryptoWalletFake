package com.cryptowallet.data.remote.service

import com.cryptowallet.data.remote.api.CoinGeckoApi

class CoinGeckoService (private val api: CoinGeckoApi) {

    suspend fun getAllCoins() = api.getAllCoins()

    suspend fun getCoinById(ids: String = "bitcoin") = api.getCoinById(ids)

    suspend fun getCoinOhlcGraph(id: String = "bitcoin", days: String = "1") = api.getCoinOhlcGraph(id, days)
}
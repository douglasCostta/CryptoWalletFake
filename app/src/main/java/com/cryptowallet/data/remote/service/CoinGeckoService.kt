package com.cryptowallet.data.remote.service

import com.cryptowallet.data.remote.api.CoinGeckoApi
import com.cryptowallet.data.remote.constants.CurrencyConstants

class CoinGeckoService (private val api: CoinGeckoApi) {

    suspend fun getAllCoins(vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY) =
        api.getAllCoins(vsCurrency = vsCurrency)

    suspend fun getCoinById(
        vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY,
        ids: String = "bitcoin",
    ) = api.getCoinById(vsCurrency = vsCurrency, ids = ids)

    suspend fun getCoinOhlcGraph(id: String = "bitcoin", days: String = "1") =
        api.getCoinOhlcGraph(id = id, days = days)
}
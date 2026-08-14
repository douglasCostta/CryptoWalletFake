package com.cryptowallet.data.remote.service

import com.cryptowallet.data.remote.constants.CurrencyConstants
import com.cryptowallet.data.remote.api.CoinGeckoApi
import com.cryptowallet.data.remote.dto.CoinGeckoCoin
import com.cryptowallet.data.remote.dto.CoinMarketChartResponse

class CoinGeckoService (private val api: CoinGeckoApi) {

    suspend fun getAllCoins(
        vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY
    ): List<CoinGeckoCoin> {
        val result = api.getAllCoins(vsCurrency)
        return result
    }

    suspend fun getCoinById(
        ids: String = "bitcoin",
        vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY
    ): List<CoinGeckoCoin> {
        val result = api.getCoinById(ids = ids, vsCurrency = vsCurrency)
        return result
    }

    suspend fun getCoinOhlcGraph(
        id: String = "bitcoin",
        vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY,
        days: String = "1"
    ): List<List<Double>> {
        val result = api.getCoinOhlcGraph(id = id, vsCurrency = vsCurrency, days = days)
        return result
    }

    suspend fun getCoinMarketChart(
        id: String = "bitcoin",
        vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY,
        days: String = "1",
    ): CoinMarketChartResponse {
        return api.getCoinMarketChart(
            id = id,
            vsCurrency = vsCurrency,
            days = days,
        )
    }
}

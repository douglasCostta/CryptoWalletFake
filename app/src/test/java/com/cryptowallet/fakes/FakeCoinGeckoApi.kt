package com.cryptowallet.fakes

import com.cryptowallet.data.remote.api.CoinGeckoApi
import com.cryptowallet.data.remote.dto.CoinGeckoCoin
import com.cryptowallet.data.remote.dto.CoinMarketChartResponse

class FakeCoinGeckoApi(
    private val coins: List<CoinGeckoCoin> = defaultCoins(),
) : CoinGeckoApi {

    var lastVsCurrency: String? = null
    var lastIds: String? = null

    override suspend fun getAllCoins(vsCurrency: String): List<CoinGeckoCoin> {
        lastVsCurrency = vsCurrency
        return coins
    }

    override suspend fun getCoinById(vsCurrency: String, ids: String): List<CoinGeckoCoin> {
        lastVsCurrency = vsCurrency
        lastIds = ids
        val requestedIds = ids.split(",")
        return coins.filter { it.id in requestedIds }
    }

    override suspend fun getCoinOhlcGraph(id: String, vsCurrency: String, days: String): List<List<Double>> {
        return emptyList()
    }

    override suspend fun getCoinMarketChart(id: String, vsCurrency: String, days: String): CoinMarketChartResponse {
        return CoinMarketChartResponse(prices = emptyList())
    }

    companion object {
        fun defaultCoins(): List<CoinGeckoCoin> = listOf(
            CoinGeckoCoin(
                id = "bitcoin", symbol = "btc", name = "Bitcoin", image = "",
                currentPrice = 262515.351, priceChange24h = 1000.0, priceChangePercentage24h = 0.4,
            ),
            CoinGeckoCoin(
                id = "ethereum", symbol = "eth", name = "Ethereum", image = "",
                currentPrice = 1939.74, priceChange24h = -20.0, priceChangePercentage24h = -1.02,
            ),
            CoinGeckoCoin(
                id = "binancecoin", symbol = "bnb", name = "BNB", image = "",
                currentPrice = 600.0, priceChange24h = 5.0, priceChangePercentage24h = 0.83,
            ),
            CoinGeckoCoin(
                id = "usd-coin", symbol = "usdc", name = "USD Coin", image = "",
                currentPrice = 5.0, priceChange24h = 0.0, priceChangePercentage24h = 0.0,
            ),
        )
    }
}

package com.cryptowallet.data.repository

import android.util.Log
import com.cryptowallet.data.remote.constants.CurrencyConstants
import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.data.remote.enums.ChartRangeEnum
import com.cryptowallet.model.CoinGraphPoints
import com.cryptowallet.model.CoinDashboard
import com.cryptowallet.model.CoinDetails
import com.cryptowallet.model.GraphCoinDashboard
import com.cryptowallet.model.toCoinChartPointOrNull
import com.cryptowallet.model.toCoinDashboard
import com.cryptowallet.model.toCoinListItem

class CoinGeckoRepository(private val service: CoinGeckoService) {

    suspend fun getCoins(vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY): List<CoinDetails> {
        return service.getAllCoins(vsCurrency = vsCurrency).map { it.toCoinListItem() }
    }

    suspend fun getCoinsByIds(
        ids: List<String>,
        vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY,
    ): List<CoinDetails> {
        if (ids.isEmpty()) return emptyList()
        return service.getCoinById(vsCurrency = vsCurrency, ids = ids.joinToString(",")).map { it.toCoinListItem() }
    }

    suspend fun getCoinDashboard(coinId: String, vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY): CoinDashboard {
        val coin = service.getCoinById(
            ids = coinId,
            vsCurrency = vsCurrency
        ).firstOrNull() ?: error("Moeda não encontrada para o id: $coinId")

        return coin.toCoinDashboard()
    }

    suspend fun getCoinChart(coinId: String, range: ChartRangeEnum, vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY): List<CoinGraphPoints> {
        return service.getCoinOhlcGraph(
            id = coinId, vsCurrency = vsCurrency,
            days = range.apiDays
        ).mapNotNull { it.toCoinChartPointOrNull() }
         .sortedBy { it.timestamp }
    }

    suspend fun getCoinDashboardPayload(coinId: String, vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY): GraphCoinDashboard {
        val dashboard = getCoinDashboard(
            coinId = coinId,
            vsCurrency = vsCurrency
        )

        return GraphCoinDashboard(dashboard = dashboard)
    }

    suspend fun getCoinLineChart(
        coinId: String,
        range: ChartRangeEnum,
        vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY,
    ): List<CoinGraphPoints> {
        try {

            val response = service.getCoinMarketChart(
                id = coinId,
                vsCurrency = vsCurrency,
                days = range.apiDays,
            )

            return response.prices
                .mapNotNull { point ->
                    if (point.size < 2) {
                        return@mapNotNull null
                    }
                    CoinGraphPoints(
                        timestamp = point[0].toLong(),
                        open = point[1],
                        high = point[1],
                        low = point[1],
                        close = point[1],
                    )
                }
                .sortedBy { it.timestamp }

        } catch (e: Exception) {
            Log.e(
                "COINGECKO_ERROR",
                "FAILED range=${range.label} days=${range.apiDays}",
                e
            )
            throw e
        }
    }
}

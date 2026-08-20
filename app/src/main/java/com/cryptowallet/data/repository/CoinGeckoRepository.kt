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

    private var cachedAllCoins: List<CoinDetails>? = null
    private var lastFetchAllCoins: Long = 0
    private var cachedCoinsByIds: Map<String, List<CoinDetails>> = emptyMap()
    private var lastFetchCoinsByIds: Map<String, Long> = emptyMap()
    private val CACHE_EXPIRATION_MS = 2 * 60 * 1000 // 2 minutos

    suspend fun getCoins(vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY): List<CoinDetails> {
        val now = System.currentTimeMillis()
        if (cachedAllCoins != null && (now - lastFetchAllCoins) < CACHE_EXPIRATION_MS) {
            return cachedAllCoins!!
        }

        return service.getAllCoins(vsCurrency = vsCurrency)
            .map { it.toCoinListItem() }
            .also {
                cachedAllCoins = it
                lastFetchAllCoins = now
            }
    }

    suspend fun getCoinsByIds(
        ids: List<String>,
        vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY,
    ): List<CoinDetails> {
        if (ids.isEmpty()) return emptyList()

        val cacheKey = "${vsCurrency}_${ids.sorted().joinToString(",")}"
        val now = System.currentTimeMillis()

        if (cachedCoinsByIds.containsKey(cacheKey) && (now - (lastFetchCoinsByIds[cacheKey] ?: 0)) < CACHE_EXPIRATION_MS) {
            return cachedCoinsByIds[cacheKey]!!
        }

        return service.getCoinById(vsCurrency = vsCurrency, ids = ids.joinToString(","))
            .map { it.toCoinListItem() }
            .also {
                cachedCoinsByIds = cachedCoinsByIds + (cacheKey to it)
                lastFetchCoinsByIds = lastFetchCoinsByIds + (cacheKey to now)
            }
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

package com.cryptowallet.data.repository

import com.cryptowallet.data.remote.constants.CurrencyConstants
import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.model.ChartRange
import com.cryptowallet.model.CoinChartPoint
import com.cryptowallet.model.CoinDashboard
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.GraphCoinDashboard
import com.cryptowallet.model.toCoinChartPointOrNull
import com.cryptowallet.model.toCoinDashboard
import com.cryptowallet.model.toCoinListItem

class CoinGeckoRepository(private val service: CoinGeckoService) {

    suspend fun getCoins(vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY): List<CoinListItem> {
        return service.getAllCoins(vsCurrency = vsCurrency).map { it.toCoinListItem() }
    }

    suspend fun getCoinsByIds(
        ids: List<String>,
        vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY,
    ): List<CoinListItem> {
        if (ids.isEmpty()) return emptyList()
        return service.getCoinById(vsCurrency = vsCurrency, ids = ids.joinToString(",")).map { it.toCoinListItem() }
    }

    suspend fun getCoinDashboard(coinId: String): CoinDashboard {
        val coin = service.getCoinById(ids = coinId).firstOrNull() ?: error("Moeda não encontrada para o id: $coinId")
        return coin.toCoinDashboard()
    }

    suspend fun getCoinChart(coinId: String, range: ChartRange): List<CoinChartPoint> {
        return service.getCoinOhlcGraph(id = coinId, days = range.apiDays)
            .mapNotNull { it.toCoinChartPointOrNull() }
            .sortedBy { it.timestamp }
    }

    suspend fun getCoinDashboardPayload(coinId: String, range: ChartRange = ChartRange.ONE_DAY): GraphCoinDashboard {
        val dashboard = getCoinDashboard(coinId = coinId)
        val chartPoints = getCoinChart(coinId = coinId, range = range)

        return GraphCoinDashboard(dashboard = dashboard, selectedRange = range, chartPoints = chartPoints)
    }
}


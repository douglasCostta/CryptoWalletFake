package com.cryptowallet.data.repository

import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.model.ChartRange
import com.cryptowallet.model.CoinChartPoint
import com.cryptowallet.model.CoinDashboard
import com.cryptowallet.model.CoinDashboardPayload
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.toCoinChartPointOrNull
import com.cryptowallet.model.toCoinDashboard
import com.cryptowallet.model.toCoinListItem

class CoinGeckoRepository(
    private val service: CoinGeckoService,
) {

    suspend fun getCoins(
        vsCurrency: String = "usd",
    ): List<CoinListItem> {
        return service
            .getAllCoins(vsCurrency = vsCurrency)
            .map { it.toCoinListItem() }
    }

    suspend fun getCoinDashboard(
        coinId: String,
        vsCurrency: String = "usd",
    ): CoinDashboard {
        val coin = service
            .getCoinById(vsCurrency = vsCurrency, ids = coinId)
            .firstOrNull()
            ?: error("Moeda não encontrada para o id: $coinId")

        return coin.toCoinDashboard()
    }

    suspend fun getCoinChart(
        coinId: String,
        range: ChartRange,
        vsCurrency: String = "usd",
    ): List<CoinChartPoint> {
        return service
            .getCoinOhlcGraph(
                id = coinId,
                vsCurrency = vsCurrency,
                days = range.apiDays,
            )
            .mapNotNull { it.toCoinChartPointOrNull() }
            .sortedBy { it.timestamp }
    }

    suspend fun getCoinDashboardPayload(
        coinId: String,
        range: ChartRange = ChartRange.ONE_DAY,
        vsCurrency: String = "usd",
    ): CoinDashboardPayload {
        val dashboard = getCoinDashboard(
            coinId = coinId,
            vsCurrency = vsCurrency,
        )

        val chartPoints = getCoinChart(
            coinId = coinId,
            range = range,
            vsCurrency = vsCurrency,
        )

        return CoinDashboardPayload(
            dashboard = dashboard,
            selectedRange = range,
            chartPoints = chartPoints,
        )
    }
}


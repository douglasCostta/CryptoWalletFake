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

class CoinGeckoRepository(private val service: CoinGeckoService) {

    suspend fun getCoins(): List<CoinListItem> {
        return service.getAllCoins().map{ it.toCoinListItem() }
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

    suspend fun getCoinDashboardPayload(coinId: String, range: ChartRange = ChartRange.ONE_DAY): CoinDashboardPayload {
        val dashboard = getCoinDashboard(coinId = coinId)
        val chartPoints = getCoinChart(coinId = coinId, range = range)

        return CoinDashboardPayload(dashboard = dashboard, selectedRange = range, chartPoints = chartPoints)
    }
}


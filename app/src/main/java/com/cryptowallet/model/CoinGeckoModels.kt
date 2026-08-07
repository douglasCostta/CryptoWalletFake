package com.cryptowallet.model

import java.io.Serializable

/**
 * Item usado na listagem principal de moedas
 */
data class CoinListItem(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val currentPrice: Double,
    val priceChange24h: Double,
    val priceChangePercentage24h: Double,
    val marketCapRank: Int?,
) : Serializable

data class CoinDashboard(
    val coin: CoinListItem,
    val marketData: CoinMarketData,
    val supplyData: CoinSupplyData,
    val performanceData: CoinPerformanceData,
    val lastUpdated: String,
) : Serializable

data class CoinMarketData(
    val marketCap: Double,
    val totalVolume24h: Double,
    val high24h: Double,
    val low24h: Double,
) : Serializable

data class CoinSupplyData(
    val circulatingSupply: Double,
    val totalSupply: Double?,
    val maxSupply: Double?,
) : Serializable

data class CoinPerformanceData(
    val ath: Double,
    val athChangePercentage: Double,
    val athDate: String,
    val atl: Double,
    val atlChangePercentage: Double,
    val atlDate: String,
) : Serializable

enum class ChartRange(
    val label: String,
    val apiDays: String,
) {
    REAL_TIME("Tempo Real", "1"),
    ONE_DAY("1D", "1"),
    ONE_WEEK("1W", "7"),
    ONE_MONTH("1M", "30"),
    THREE_MONTHS("3M", "90"),
    SIX_MONTHS("6M", "180"),
}

data class CoinChartPoint(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
) : Serializable

data class GraphCoinDashboard(
    val dashboard: CoinDashboard,
    val selectedRange: ChartRange,
    val chartPoints: List<CoinChartPoint>,
) : Serializable


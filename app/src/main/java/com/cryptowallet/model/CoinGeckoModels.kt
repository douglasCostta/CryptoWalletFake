package com.cryptowallet.model

import java.io.Serializable

data class CoinDetails (
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
    val coinDetails: CoinDetails,
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

data class GraphCoinDashboard(val dashboard: CoinDashboard) : Serializable


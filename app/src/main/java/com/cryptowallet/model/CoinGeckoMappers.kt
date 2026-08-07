package com.cryptowallet.model

import com.cryptowallet.data.remote.dto.CoinGeckoCoin

fun CoinGeckoCoin.toCoinListItem(): CoinListItem {
    return CoinListItem(
        id = id.orEmpty(),
        symbol = symbol.orEmpty(),
        name = name.orEmpty(),
        imageUrl = image.orEmpty(),
        currentPrice = currentPrice ?: 0.0,
        priceChange24h = priceChange24h ?: 0.0,
        priceChangePercentage24h = priceChangePercentage24h ?: 0.0,
        marketCapRank = marketCapRank,
    )
}

fun CoinGeckoCoin.toCoinDashboard(): CoinDashboard {
    return CoinDashboard(
        coin = toCoinListItem(),
        marketData = CoinMarketData(
            marketCap = marketCap ?: 0.0,
            totalVolume24h = totalVolume ?: 0.0,
            high24h = high24h ?: 0.0,
            low24h = low24h ?: 0.0,
        ),
        supplyData = CoinSupplyData(
            circulatingSupply = circulatingSupply ?: 0.0,
            totalSupply = totalSupply,
            maxSupply = maxSupply,
        ),
        performanceData = CoinPerformanceData(
            ath = ath ?: 0.0,
            athChangePercentage = athChangePercentage ?: 0.0,
            athDate = athDate.orEmpty(),
            atl = atl ?: 0.0,
            atlChangePercentage = atlChangePercentage ?: 0.0,
            atlDate = atlDate.orEmpty(),
        ),
        lastUpdated = lastUpdated.orEmpty(),
    )
}

fun List<Double>.toCoinChartPointOrNull(): CoinChartPoint? {
    if (size < 5) return null

    return CoinChartPoint(
        timestamp = get(0).toLong(),
        open = get(1),
        high = get(2),
        low = get(3),
        close = get(4),
    )
}


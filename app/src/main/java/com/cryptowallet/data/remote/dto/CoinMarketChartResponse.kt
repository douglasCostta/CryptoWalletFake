package com.cryptowallet.data.remote.dto

data class CoinMarketChartResponse(
    val prices: List<List<Double>>,
)
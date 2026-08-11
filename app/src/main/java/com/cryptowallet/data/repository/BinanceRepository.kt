package com.cryptowallet.data.repository

import com.cryptowallet.data.remote.api.BinanceApi
import com.cryptowallet.data.remote.enums.ChartRangeEnum
import com.cryptowallet.model.CoinGraphPoints

class BinanceRepository(private val api: BinanceApi) {

    private fun mapRangeToInterval(range: ChartRangeEnum): String {
        return range.binanceInterval
    }

    suspend fun getHistoricalKlines(
        coinSymbol: String,
        range: ChartRangeEnum,
        limit: Int = 100
    ): List<CoinGraphPoints> {
        val symbol = "${coinSymbol.uppercase()}USDT"
        val interval = mapRangeToInterval(range)

        return try {
            val response = api.getKlines(symbol, interval, limit)
            response.map { list ->
                CoinGraphPoints(
                    timestamp = (list[0] as Double).toLong(),
                    open = (list[1] as String).toDouble(),
                    high = (list[2] as String).toDouble(),
                    low = (list[3] as String).toDouble(),
                    close = (list[4] as String).toDouble()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

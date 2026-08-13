package com.cryptowallet.data.remote.enums

enum class ChartRangeEnum(
    val label: String,
    val apiDays: String,
    val binanceInterval: String
) {
    REAL_TIME("Real time", "1", "1s"),
    TODAY("Today", "1", "1m"),
    ONE_WEEK("1W", "7", "1h"),
    ONE_MONTH("1M", "30", "4h"),
    THREE_MONTHS("3M", "90", "1d"),
    SIX_MONTHS("6M", "180", "1d")
}

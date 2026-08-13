package com.cryptowallet.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BinanceKlineEvent(
    @SerializedName("e") val eventType: String,
    @SerializedName("E") val eventTime: Long,
    @SerializedName("s") val symbol: String,
    @SerializedName("k") val kline: KlineData
)

data class KlineData(
    @SerializedName("t") val startTime: Long,
    @SerializedName("T") val closeTime: Long,
    @SerializedName("i") val interval: String,
    @SerializedName("o") val open: String,
    @SerializedName("c") val close: String,
    @SerializedName("h") val high: String,
    @SerializedName("l") val low: String,
    @SerializedName("x") val isClosed: Boolean
)

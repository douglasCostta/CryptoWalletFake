package com.cryptowallet.model

import java.io.Serializable

data class CoinGraphPoints (
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
) : Serializable

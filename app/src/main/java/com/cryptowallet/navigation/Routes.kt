package com.cryptowallet.navigation

import com.cryptowallet.model.TransactionType
import kotlinx.serialization.Serializable

sealed class Routes {

    @Serializable
    data object Wallet : Routes()

    @Serializable
    data class Trade(val type: TransactionType, val preselectedCoinId: String? = null) : Routes()

    @Serializable
    data class TransactionSuccess(
        val type: TransactionType,
        val coinSymbol: String,
        val amount: Double,
    ) : Routes()

    @Serializable
    data class Dashboard(val coinId: String) : Routes()
}

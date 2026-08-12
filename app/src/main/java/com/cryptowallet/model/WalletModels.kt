package com.cryptowallet.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType { BUY, SELL }

data class WalletHolding(
    val coinId: String,
    val symbol: String,
    val amount: Double,
)

data class WalletState(
    val cashBalanceReais: Double,
    val holdings: List<WalletHolding>,
)

data class CoinBalance(
    val coin: CoinListItem,
    val amountOwned: Double,
    val valueInReais: Double,
)

data class WalletBalance(
    val totalReais: Double,
    val changeAmount24h: Double,
    val changePercentage24h: Double,
)

data class TransactionRequest(
    val type: TransactionType,
    val coinId: String,
    val amountInReais: Double,
    val amountInCoin: Double,
)

data class TransactionResult(
    val type: TransactionType,
    val coinSymbol: String,
    val amountInCoin: Double,
    val newWalletState: WalletState,
)

/**
 * Derives the total balance + 24h change purely from already-fetched [coinBalances] and this
 * state's cash figure, with no network access - lets callers avoid re-fetching coin prices just
 * to compute a total when they already have [coinBalances] from elsewhere.
 */
fun WalletState.toWalletBalance(coinBalances: List<CoinBalance>): WalletBalance {
    val totalReais = cashBalanceReais + coinBalances.sumOf { it.valueInReais }
    val totalPrevious = cashBalanceReais + coinBalances.sumOf { balance ->
        val previousPrice = balance.coin.currentPrice - balance.coin.priceChange24h
        balance.amountOwned * previousPrice
    }
    val changeAmount = totalReais - totalPrevious
    val changePercentage = if (totalPrevious == 0.0) {
        0.0
    } else {
        (changeAmount / totalPrevious) * 100.0
    }

    return WalletBalance(
        totalReais = totalReais,
        changeAmount24h = changeAmount,
        changePercentage24h = changePercentage,
    )
}

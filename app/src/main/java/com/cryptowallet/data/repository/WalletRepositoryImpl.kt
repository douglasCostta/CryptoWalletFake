package com.cryptowallet.data.repository

import com.cryptowallet.data.local.WalletLocalDataSource
import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinDetails
import com.cryptowallet.model.TransactionRequest
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.TransactionType
import com.cryptowallet.model.WalletBalance
import com.cryptowallet.model.WalletHolding
import com.cryptowallet.model.WalletState
import com.cryptowallet.model.toWalletBalance

class WalletRepositoryImpl(
    private val coinGeckoRepository: CoinGeckoRepository,
    private val localDataSource: WalletLocalDataSource,
) : WalletRepository {

    override suspend fun getTradableCoins(): List<CoinDetails> =
        coinGeckoRepository.getCoinsByIds(ids = SUPPORTED_COIN_IDS, vsCurrency = VS_CURRENCY)

    override suspend fun getCoinBalances(): List<CoinBalance> {
        val state = localDataSource.getWalletState()
        val coins = getTradableCoins()
        return state.holdings.mapNotNull { holding ->
            val coin = coins.find { it.id == holding.coinId } ?: return@mapNotNull null
            CoinBalance(
                coin = coin,
                amountOwned = holding.amount,
                valueInReais = holding.amount * coin.currentPrice,
            )
        }
    }

    override suspend fun getAllCoins(): List<CoinDetails> =
        coinGeckoRepository.getCoins(vsCurrency = VS_CURRENCY)

    override suspend fun getWalletState(): WalletState = localDataSource.getWalletState()

    override suspend fun getWalletBalance(): WalletBalance {
        val state = localDataSource.getWalletState()
        return state.toWalletBalance(getCoinBalances())
    }

    override suspend fun getCoinPrice(coinId: String): Double =
        getTradableCoins().find { it.id == coinId }?.currentPrice
            ?: error("Coin not found: $coinId")

    override suspend fun buy(request: TransactionRequest): TransactionResult = execute(request)

    override suspend fun sell(request: TransactionRequest): TransactionResult = execute(request)

    private suspend fun execute(request: TransactionRequest): TransactionResult {
        var coinSymbol = request.coinId

        val newState = localDataSource.updateWalletState { state ->
            val holding = state.holdings.find { it.coinId == request.coinId }
                ?: error("Unknown coin in wallet: ${request.coinId}")
            coinSymbol = holding.symbol

            check(request.amountInReais > 0.0 && request.amountInCoin > 0.0) {
                "O valor da transação deve ser maior que zero."
            }

            when (request.type) {
                TransactionType.BUY -> {
                    check(request.amountInReais <= state.cashBalanceReais) {
                        "Saldo insuficiente em R$ para completar a compra."
                    }
                    state.copy(
                        cashBalanceReais = state.cashBalanceReais - request.amountInReais,
                        holdings = state.holdings.updateAmount(request.coinId) { it + request.amountInCoin },
                    )
                }
                TransactionType.SELL -> {
                    check(request.amountInCoin <= holding.amount) {
                        "Saldo insuficiente da moeda para completar a venda."
                    }
                    state.copy(
                        cashBalanceReais = state.cashBalanceReais + request.amountInReais,
                        holdings = state.holdings.updateAmount(request.coinId) { it - request.amountInCoin },
                    )
                }
            }
        }

        return TransactionResult(
            type = request.type,
            coinSymbol = coinSymbol,
            amountInCoin = request.amountInCoin,
            newWalletState = newState,
        )
    }

    private fun List<WalletHolding>.updateAmount(coinId: String, transform: (Double) -> Double): List<WalletHolding> =
        map { if (it.coinId == coinId) it.copy(amount = transform(it.amount)) else it }

    companion object {
        const val VS_CURRENCY = "brl"
        val SUPPORTED_COIN_IDS = listOf("bitcoin", "ethereum", "binancecoin", "usd-coin")
    }
}

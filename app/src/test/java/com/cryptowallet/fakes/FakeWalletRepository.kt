package com.cryptowallet.fakes

import com.cryptowallet.data.repository.WalletRepository
import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.TransactionRequest
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.WalletBalance
import com.cryptowallet.model.WalletState

class FakeWalletRepository(
    var walletBalanceProvider: suspend () -> WalletBalance = {
        WalletBalance(totalReais = 20802.0, changePercentage24h = 4.23)
    },
    var coinBalancesProvider: suspend () -> List<CoinBalance> = { emptyList() },
    var allCoinsProvider: suspend () -> List<CoinListItem> = { emptyList() },
    var tradableCoinsProvider: suspend () -> List<CoinListItem> = { emptyList() },
    var walletStateProvider: suspend () -> WalletState = {
        WalletState(cashBalanceReais = 1074.32, holdings = emptyList())
    },
    var transactionProvider: suspend (TransactionRequest) -> TransactionResult = {
        error("transactionProvider not configured in fake")
    },
) : WalletRepository {

    var lastRequest: TransactionRequest? = null

    override suspend fun getWalletBalance(): WalletBalance = walletBalanceProvider()
    override suspend fun getCoinBalances(): List<CoinBalance> = coinBalancesProvider()
    override suspend fun getAllCoins(): List<CoinListItem> = allCoinsProvider()
    override suspend fun getTradableCoins(): List<CoinListItem> = tradableCoinsProvider()
    override suspend fun getWalletState(): WalletState = walletStateProvider()

    override suspend fun getCoinPrice(coinId: String): Double =
        tradableCoinsProvider().find { it.id == coinId }?.currentPrice
            ?: error("Coin not found: $coinId")

    override suspend fun buy(request: TransactionRequest): TransactionResult {
        lastRequest = request
        return transactionProvider(request)
    }

    override suspend fun sell(request: TransactionRequest): TransactionResult {
        lastRequest = request
        return transactionProvider(request)
    }
}

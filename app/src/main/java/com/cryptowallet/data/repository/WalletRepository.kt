package com.cryptowallet.data.repository

import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.TransactionRequest
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.WalletBalance
import com.cryptowallet.model.WalletState

interface WalletRepository {
    suspend fun getWalletBalance(): WalletBalance
    suspend fun getCoinBalances(): List<CoinBalance>
    suspend fun getAllCoins(): List<CoinListItem>
    suspend fun getTradableCoins(): List<CoinListItem>
    suspend fun getWalletState(): WalletState
    suspend fun getCoinPrice(coinId: String): Double
    suspend fun buy(request: TransactionRequest): TransactionResult
    suspend fun sell(request: TransactionRequest): TransactionResult
}

package com.cryptowallet.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.cryptowallet.data.local.WalletLocalDataSource
import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.fakes.FakeCoinGeckoApi
import com.cryptowallet.model.TransactionRequest
import com.cryptowallet.model.TransactionType
import com.cryptowallet.model.WalletHolding
import com.cryptowallet.model.WalletState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WalletRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun localDataSource(): WalletLocalDataSource {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempFolder.root, "test-${System.nanoTime()}.preferences_pb") },
        )
        return WalletLocalDataSource(dataStore)
    }

    private fun repository(local: WalletLocalDataSource) = WalletRepositoryImpl(
        coinGeckoRepository = CoinGeckoRepository(CoinGeckoService(FakeCoinGeckoApi())),
        localDataSource = local,
    )

    @Test
    fun `getWalletBalance sums cash plus the reais value of all holdings`() = runTest {
        val local = localDataSource()
        local.saveWalletState(
            WalletState(
                cashBalanceReais = 1000.0,
                holdings = listOf(
                    WalletHolding(coinId = "bitcoin", symbol = "BTC", amount = 0.01),
                    WalletHolding(coinId = "ethereum", symbol = "ETH", amount = 1.0),
                ),
            ),
        )
        val repo = repository(local)

        val balance = repo.getWalletBalance()

        val expectedTotal = 1000.0 + (262515.351 * 0.01) + (1939.74 * 1.0)
        assertEquals(expectedTotal, balance.totalReais, 0.001)
    }

    @Test
    fun `getCoinBalances merges owned amount with the live price`() = runTest {
        val local = localDataSource()
        local.saveWalletState(
            WalletState(
                cashBalanceReais = 0.0,
                holdings = listOf(WalletHolding(coinId = "ethereum", symbol = "ETH", amount = 2.0)),
            ),
        )
        val repo = repository(local)

        val balances = repo.getCoinBalances()

        assertEquals(1, balances.size)
        assertEquals("ethereum", balances[0].coin.id)
        assertEquals(2.0, balances[0].amountOwned, 0.0)
        assertEquals(1939.74 * 2.0, balances[0].valueInReais, 0.001)
    }

    @Test
    fun `buy deducts cash and increases the coin holding`() = runTest {
        val local = localDataSource()
        local.saveWalletState(
            WalletState(cashBalanceReais = 1000.0, holdings = listOf(WalletHolding("bitcoin", "BTC", 0.0))),
        )
        val repo = repository(local)

        val result = repo.buy(
            TransactionRequest(TransactionType.BUY, coinId = "bitcoin", amountInReais = 100.0, amountInCoin = 0.0003810),
        )

        assertEquals(TransactionType.BUY, result.type)
        assertEquals(0.0003810, result.amountInCoin, 0.0000001)
        val newState = local.getWalletState()
        assertEquals(900.0, newState.cashBalanceReais, 0.001)
        assertEquals(0.0003810, newState.holdings.first { it.coinId == "bitcoin" }.amount, 0.0000001)
    }

    @Test
    fun `buy fails when amountInReais exceeds available cash and leaves state unchanged`() = runTest {
        val local = localDataSource()
        val initialState = WalletState(cashBalanceReais = 50.0, holdings = listOf(WalletHolding("bitcoin", "BTC", 0.0)))
        local.saveWalletState(initialState)
        val repo = repository(local)

        try {
            repo.buy(TransactionRequest(TransactionType.BUY, coinId = "bitcoin", amountInReais = 100.0, amountInCoin = 0.0003810))
            org.junit.Assert.fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // expected
        }

        assertEquals(initialState, local.getWalletState())
    }

    @Test
    fun `sell increases cash and decreases the coin holding`() = runTest {
        val local = localDataSource()
        local.saveWalletState(
            WalletState(cashBalanceReais = 0.0, holdings = listOf(WalletHolding("ethereum", "ETH", 1.0))),
        )
        val repo = repository(local)

        val result = repo.sell(
            TransactionRequest(TransactionType.SELL, coinId = "ethereum", amountInReais = 1939.74, amountInCoin = 1.0),
        )

        assertEquals(TransactionType.SELL, result.type)
        val newState = local.getWalletState()
        assertEquals(1939.74, newState.cashBalanceReais, 0.001)
        assertEquals(0.0, newState.holdings.first { it.coinId == "ethereum" }.amount, 0.0000001)
    }

    @Test
    fun `sell fails when amountInCoin exceeds owned amount and leaves state unchanged`() = runTest {
        val local = localDataSource()
        val initialState = WalletState(cashBalanceReais = 0.0, holdings = listOf(WalletHolding("ethereum", "ETH", 0.5)))
        local.saveWalletState(initialState)
        val repo = repository(local)

        try {
            repo.sell(TransactionRequest(TransactionType.SELL, coinId = "ethereum", amountInReais = 1939.74, amountInCoin = 1.0))
            org.junit.Assert.fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // expected
        }

        assertEquals(initialState, local.getWalletState())
    }

    @Test
    fun `buy and sell reject negative amounts and leave state unchanged`() = runTest {
        val local = localDataSource()
        val initialState = WalletState(cashBalanceReais = 1000.0, holdings = listOf(WalletHolding("bitcoin", "BTC", 1.0)))
        local.saveWalletState(initialState)
        val repo = repository(local)

        try {
            repo.buy(
                TransactionRequest(TransactionType.BUY, coinId = "bitcoin", amountInReais = -100.0, amountInCoin = -0.001),
            )
            org.junit.Assert.fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // expected
        }
        assertEquals(initialState, local.getWalletState())

        try {
            repo.sell(
                TransactionRequest(TransactionType.SELL, coinId = "bitcoin", amountInReais = -100.0, amountInCoin = -0.001),
            )
            org.junit.Assert.fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // expected
        }
        assertEquals(initialState, local.getWalletState())
    }

    @Test(expected = IllegalStateException::class)
    fun `buy fails loudly when coinId is not a known holding`() = runTest {
        val local = localDataSource()
        local.saveWalletState(
            WalletState(cashBalanceReais = 1000.0, holdings = listOf(WalletHolding("bitcoin", "BTC", 0.0))),
        )
        val repo = repository(local)

        repo.buy(TransactionRequest(TransactionType.BUY, coinId = "dogecoin", amountInReais = 10.0, amountInCoin = 1.0))
    }
}

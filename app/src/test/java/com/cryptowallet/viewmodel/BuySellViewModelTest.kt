package com.cryptowallet.viewmodel

import com.cryptowallet.MainDispatcherRule
import com.cryptowallet.fakes.FakeWalletRepository
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.TransactionType
import com.cryptowallet.model.WalletHolding
import com.cryptowallet.model.WalletState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuySellViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun ethCoin() = CoinListItem(
        id = "ethereum", symbol = "eth", name = "Ethereum", imageUrl = "",
        currentPrice = 2000.0, priceChange24h = -20.0, priceChangePercentage24h = -1.0, marketCapRank = 2,
    )

    @Test
    fun `selects the first available coin and loads balance before on init`() = runTest {
        val repository = FakeWalletRepository(
            tradableCoinsProvider = { listOf(ethCoin()) },
            walletStateProvider = {
                WalletState(cashBalanceReais = 1074.32, holdings = listOf(WalletHolding("ethereum", "ETH", 0.0)))
            },
        )

        val viewModel = BuySellViewModel(repository, TransactionType.BUY)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("ethereum", state.selectedCoin?.id)
        assertEquals(2000.0, state.currentPrice, 0.0)
        assertEquals(1074.32, state.balanceBefore?.cashBalanceReais)
    }

    @Test
    fun `typing an amount in reais fills the coin amount and balance after`() = runTest {
        val repository = FakeWalletRepository(
            tradableCoinsProvider = { listOf(ethCoin()) },
            walletStateProvider = {
                WalletState(cashBalanceReais = 1000.0, holdings = listOf(WalletHolding("ethereum", "ETH", 0.0)))
            },
        )
        val viewModel = BuySellViewModel(repository, TransactionType.BUY)
        advanceUntilIdle()

        viewModel.onAmountInReaisChanged("200")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0.1, state.amountInCoin.toDouble(), 0.0001)
        assertEquals(800.0, state.balanceAfter?.cashBalanceReais)
        assertEquals(
            0.1,
            state.balanceAfter?.holdings?.first { it.coinId == "ethereum" }?.amount ?: 0.0,
            0.0001,
        )
    }

    @Test
    fun `typing an amount in coin fills the reais amount`() = runTest {
        val repository = FakeWalletRepository(
            tradableCoinsProvider = { listOf(ethCoin()) },
            walletStateProvider = {
                WalletState(cashBalanceReais = 1000.0, holdings = listOf(WalletHolding("ethereum", "ETH", 1.0)))
            },
        )
        val viewModel = BuySellViewModel(repository, TransactionType.SELL)
        advanceUntilIdle()

        viewModel.onAmountInCoinChanged("0.5")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1000.0, state.amountInReais.toDouble(), 0.0001)
    }

    @Test
    fun `confirm success stores the result and updates balance after`() = runTest {
        val expectedResult = TransactionResult(
            type = TransactionType.BUY,
            coinSymbol = "ETH",
            amountInCoin = 0.1,
            newWalletState = WalletState(cashBalanceReais = 800.0, holdings = listOf(WalletHolding("ethereum", "ETH", 0.1))),
        )
        val repository = FakeWalletRepository(
            tradableCoinsProvider = { listOf(ethCoin()) },
            walletStateProvider = {
                WalletState(cashBalanceReais = 1000.0, holdings = listOf(WalletHolding("ethereum", "ETH", 0.0)))
            },
            transactionProvider = { expectedResult },
        )
        val viewModel = BuySellViewModel(repository, TransactionType.BUY)
        advanceUntilIdle()
        viewModel.onAmountInReaisChanged("200")
        advanceUntilIdle()

        viewModel.onConfirmClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(expectedResult, state.result)
        assertEquals(false, state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `confirm failure surfaces an error message and keeps result null`() = runTest {
        val repository = FakeWalletRepository(
            tradableCoinsProvider = { listOf(ethCoin()) },
            walletStateProvider = {
                WalletState(cashBalanceReais = 1000.0, holdings = listOf(WalletHolding("ethereum", "ETH", 0.0)))
            },
            transactionProvider = { error("Saldo insuficiente em R$ para completar a compra.") },
        )
        val viewModel = BuySellViewModel(repository, TransactionType.BUY)
        advanceUntilIdle()
        viewModel.onAmountInReaisChanged("200")
        advanceUntilIdle()

        viewModel.onConfirmClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.result)
        assertEquals("Saldo insuficiente em R$ para completar a compra.", state.errorMessage)
    }

    @Test
    fun `confirm with negative amount sets an error and does not call the repository`() = runTest {
        val repository = FakeWalletRepository(
            tradableCoinsProvider = { listOf(ethCoin()) },
            walletStateProvider = {
                WalletState(cashBalanceReais = 1000.0, holdings = listOf(WalletHolding("ethereum", "ETH", 0.0)))
            },
        )
        val viewModel = BuySellViewModel(repository, TransactionType.BUY)
        advanceUntilIdle()
        viewModel.onAmountInReaisChanged("-100")
        advanceUntilIdle()

        viewModel.onConfirmClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.result)
        assertEquals(false, state.errorMessage.isNullOrBlank())
        assertNull(repository.lastRequest)
    }
}

package com.cryptowallet.viewmodel

import com.cryptowallet.MainDispatcherRule
import com.cryptowallet.fakes.FakeWalletRepository
import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinDetails
import com.cryptowallet.model.WalletState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun ethCoin() = CoinDetails(
        id = "ethereum", symbol = "eth", name = "Ethereum", imageUrl = "",
        currentPrice = 1939.74, priceChange24h = -20.0, priceChangePercentage24h = -1.02, marketCapRank = 2,
    )

    @Test
    fun `loads balance and coin lists on init`() = runTest {
        val repository = FakeWalletRepository(
            walletStateProvider = { WalletState(cashBalanceReais = 1000.0, holdings = emptyList()) },
            coinBalancesProvider = { listOf(CoinBalance(coin = ethCoin(), amountOwned = 1.0, valueInReais = 1939.74)) },
            allCoinsProvider = { listOf(ethCoin()) },
        )

        val viewModel = WalletViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        // 1000.0 cash + 1939.74 from the single ETH holding, derived locally from
        // coinBalancesProvider's result (no separate getWalletBalance() network call).
        assertEquals(2939.74, state.balance?.totalReais ?: 0.0, 0.001)
        assertEquals(1, state.yourCoins.size)
        assertEquals(1, state.allCoins.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onTabSelected updates the selected tab`() = runTest {
        val viewModel = WalletViewModel(FakeWalletRepository())
        advanceUntilIdle()

        viewModel.onTabSelected(WalletTab.ALL_COINS)

        assertEquals(WalletTab.ALL_COINS, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `refresh surfaces a repository failure as an error message`() = runTest {
        val repository = FakeWalletRepository(
            coinBalancesProvider = { error("network down") },
        )

        val viewModel = WalletViewModel(repository)
        advanceUntilIdle()

        assertEquals("network down", viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
}

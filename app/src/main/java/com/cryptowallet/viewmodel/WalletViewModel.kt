package com.cryptowallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.data.repository.WalletRepository
import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinDetails
import com.cryptowallet.model.WalletBalance
import com.cryptowallet.model.toWalletBalance
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WalletTab { YOUR_COINS, ALL_COINS }

data class WalletUiState(
    val isLoading: Boolean = false,
    val balance: WalletBalance? = null,
    val yourCoins: List<CoinBalance> = emptyList(),
    val allCoins: List<CoinDetails> = emptyList(),
    val selectedTab: WalletTab = WalletTab.YOUR_COINS,
    val errorMessage: String? = null,
)

class WalletViewModel(private val repository: WalletRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onTabSelected(tab: WalletTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching {
                coroutineScope {
                    // getWalletState() is a local-only read (no network); getCoinBalances() and
                    // getAllCoins() are the only two calls that actually hit the CoinGecko API.
                    // The total balance is derived from getCoinBalances()'s result instead of
                    // also calling getWalletBalance() separately, which would re-fetch the same
                    // coin prices a second time.
                    val walletStateDeferred = async { repository.getWalletState() }
                    val yourCoinsDeferred = async { repository.getCoinBalances() }
                    val allCoinsDeferred = async { repository.getAllCoins() }
                    val yourCoins = yourCoinsDeferred.await()
                    val balance = walletStateDeferred.await().toWalletBalance(yourCoins)
                    Triple(balance, yourCoins, allCoinsDeferred.await())
                }
            }.onSuccess { (balance, yourCoins, allCoins) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        balance = balance,
                        yourCoins = yourCoins,
                        allCoins = allCoins,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Erro ao carregar carteira.",
                    )
                }
            }
        }
    }
}

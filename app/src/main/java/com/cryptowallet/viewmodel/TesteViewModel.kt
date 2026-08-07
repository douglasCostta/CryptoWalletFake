package com.cryptowallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.data.remote.RetrofitInstance
import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.data.repository.CoinGeckoRepository
import com.cryptowallet.model.ChartRange
import com.cryptowallet.model.CoinDashboardPayload
import com.cryptowallet.model.CoinListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CoinUiState(
    val isLoadingCoins: Boolean = false,
    val isLoadingDashboard: Boolean = false,
    val coins: List<CoinListItem> = emptyList(),
    val selectedCoinId: String? = null,
    val dashboardPayload: CoinDashboardPayload? = null,
    val selectedRange: ChartRange = ChartRange.ONE_DAY,
    val errorMessage: String? = null,
)

class TesteViewModel() : ViewModel() {

    private val _service = CoinGeckoService(RetrofitInstance.api)
    private val _repository = CoinGeckoRepository(_service)

    private val _uiState = MutableStateFlow(CoinUiState())
    val uiState: StateFlow<CoinUiState> = _uiState.asStateFlow()

    fun loadCoins(vsCurrency: String = "usd") {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingCoins = true,
                    errorMessage = null,
                )
            }

            runCatching {
                _repository.getCoins(vsCurrency = vsCurrency)
            }.onSuccess { coins ->
                _uiState.update {
                    it.copy(
                        isLoadingCoins = false,
                        coins = coins,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingCoins = false,
                        errorMessage = throwable.message ?: "Erro ao carregar moedas.",
                    )
                }
            }
        }
    }
}


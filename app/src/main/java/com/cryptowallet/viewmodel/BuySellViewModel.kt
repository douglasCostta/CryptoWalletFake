package com.cryptowallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.data.repository.WalletRepository
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.TransactionRequest
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.TransactionType
import com.cryptowallet.model.WalletState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TradeUiState(
    val type: TransactionType,
    val availableCoins: List<CoinListItem> = emptyList(),
    val selectedCoin: CoinListItem? = null,
    val currentPrice: Double = 0.0,
    val balanceBefore: WalletState? = null,
    val amountInReais: String = "",
    val amountInCoin: String = "",
    val balanceAfter: WalletState? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val result: TransactionResult? = null,
)

class BuySellViewModel(
    private val repository: WalletRepository,
    type: TransactionType,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TradeUiState(type = type))
    val uiState: StateFlow<TradeUiState> = _uiState.asStateFlow()

    private var loadBalanceJob: Job? = null

    init {
        loadCoins()
    }

    private fun loadCoins() {
        viewModelScope.launch {
            runCatching { repository.getTradableCoins() }
                .onSuccess { coins ->
                    _uiState.update { it.copy(availableCoins = coins) }
                    coins.firstOrNull()?.let(::onCoinSelected)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(errorMessage = throwable.message ?: "Erro ao carregar moedas.") }
                }
        }
    }

    fun onCoinSelected(coin: CoinListItem) {
        _uiState.update {
            it.copy(
                selectedCoin = coin,
                currentPrice = coin.currentPrice,
                amountInReais = "",
                amountInCoin = "",
                balanceAfter = null,
            )
        }
        loadBalanceJob?.cancel()
        loadBalanceJob = viewModelScope.launch {
            runCatching { repository.getWalletState() }
                .onSuccess { state -> _uiState.update { it.copy(balanceBefore = state, balanceAfter = state) } }
                .onFailure { throwable ->
                    _uiState.update { it.copy(errorMessage = throwable.message ?: "Erro ao carregar saldo.") }
                }
        }
    }

    fun onAmountInReaisChanged(text: String) {
        val price = _uiState.value.currentPrice
        val reais = text.replace(",", ".").toDoubleOrNull()
        val coinAmount = if (reais != null && price > 0.0) reais / price else null
        _uiState.update {
            val updated = it.copy(
                amountInReais = text,
                amountInCoin = coinAmount?.toString().orEmpty(),
                errorMessage = null,
                result = null,
            )
            updated.copy(balanceAfter = computeBalanceAfter(updated))
        }
    }

    fun onAmountInCoinChanged(text: String) {
        val price = _uiState.value.currentPrice
        val coinAmount = text.replace(",", ".").toDoubleOrNull()
        val reais = coinAmount?.let { it * price }
        _uiState.update {
            val updated = it.copy(
                amountInCoin = text,
                amountInReais = reais?.toString().orEmpty(),
                errorMessage = null,
                result = null,
            )
            updated.copy(balanceAfter = computeBalanceAfter(updated))
        }
    }

    fun onConfirmClick() {
        val state = _uiState.value
        val coin = state.selectedCoin ?: return
        val amountInReais = state.amountInReais.toDoubleOrNull()?.takeIf { it > 0.0 } ?: run {
            _uiState.update { it.copy(errorMessage = "Informe um valor válido maior que zero.") }
            return
        }
        val amountInCoin = state.amountInCoin.toDoubleOrNull()?.takeIf { it > 0.0 } ?: run {
            _uiState.update { it.copy(errorMessage = "Informe um valor válido maior que zero.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val request = TransactionRequest(
                type = state.type,
                coinId = coin.id,
                amountInReais = amountInReais,
                amountInCoin = amountInCoin,
            )

            val outcome = if (state.type == TransactionType.BUY) {
                runCatching { repository.buy(request) }
            } else {
                runCatching { repository.sell(request) }
            }

            outcome
                .onSuccess { result ->
                    _uiState.update { it.copy(isLoading = false, result = result, balanceAfter = result.newWalletState) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message ?: "Erro ao confirmar transação.")
                    }
                }
        }
    }

    private fun computeBalanceAfter(state: TradeUiState): WalletState? {
        val before = state.balanceBefore ?: return null
        val coin = state.selectedCoin ?: return before
        val amountInReais = state.amountInReais.toDoubleOrNull() ?: return before
        val amountInCoin = state.amountInCoin.toDoubleOrNull() ?: return before

        return when (state.type) {
            TransactionType.BUY -> before.copy(
                cashBalanceReais = before.cashBalanceReais - amountInReais,
                holdings = before.holdings.map {
                    if (it.coinId == coin.id) it.copy(amount = it.amount + amountInCoin) else it
                },
            )
            TransactionType.SELL -> before.copy(
                cashBalanceReais = before.cashBalanceReais + amountInReais,
                holdings = before.holdings.map {
                    if (it.coinId == coin.id) it.copy(amount = it.amount - amountInCoin) else it
                },
            )
        }
    }
}

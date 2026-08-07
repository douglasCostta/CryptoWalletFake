package com.cryptowallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.data.remote.RetrofitInstance
import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.data.repository.CoinGeckoRepository
import com.cryptowallet.model.GraphCoinDashboard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val coinSelected: String = "",
    var isLoadingCoins: Boolean = false,
    var errorMessage: String? = null,
    var graph: GraphCoinDashboard? = null
)

class DashboardViewModel() : ViewModel() {
    private val _service = CoinGeckoService(RetrofitInstance.api)
    private val _repository = CoinGeckoRepository(_service)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()


    init {
        loadGraph(_uiState.value.coinSelected)
    }

    fun loadGraph(coinId: String) {
        viewModelScope.launch {
            try {
                _uiState.value.isLoadingCoins = true
                _uiState.value.graph = _repository.getCoinDashboardPayload(coinId)

            } catch (e: Exception) {
                _uiState.value.errorMessage = "Error loading graph: ${e.message}"
            }
        }
    }


}
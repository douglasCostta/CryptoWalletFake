package com.cryptowallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.data.remote.BinanceWebSocketManager
import com.cryptowallet.data.remote.enums.ChartRangeEnum
import com.cryptowallet.data.remote.RetrofitInstance
import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.data.repository.BinanceRepository
import com.cryptowallet.data.repository.CoinGeckoRepository
import com.cryptowallet.model.CoinGraphPoints
import com.cryptowallet.model.GraphCoinDashboard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val coinSelected: String = "bitcoin", //TODO: obter da tela HOME
    val selectedRange: ChartRangeEnum = ChartRangeEnum.TODAY,
    var isLoadingCoins: Boolean = false,
    var errorMessage: String? = null,
    var infoCoin: GraphCoinDashboard? = null,
    val graphPoints: List<CoinGraphPoints> = emptyList(),
    val marketCap: Double = 0.0,
    val totalVolume24h: Double = 0.0,
    val imageUrl: String = "",
    val coinName: String = "",
    val currentPrice: Double = 0.0,
    val priceChange24h: Double = 0.0,
    val priceChangePercentage24h: Double = 0.0,
    val symbol: String = ""
)

class DashboardViewModel() : ViewModel() {
    private val _service = CoinGeckoService(RetrofitInstance.api)
    private val _repository = CoinGeckoRepository(_service)
    private val _binanceRepository = BinanceRepository(RetrofitInstance.binanceApi)
    private var _binanceWS: BinanceWebSocketManager? = null
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadCoinGraph(_uiState.value.coinSelected)
    }

    fun loadCoinGraph(coinId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingCoins = true, errorMessage = null) }
                
                val infoCoin = _repository.getCoinDashboardPayload(coinId = coinId)

                _uiState.update { it.copy(
                            infoCoin = infoCoin,
                            marketCap = infoCoin.dashboard.marketData.marketCap,
                            totalVolume24h = infoCoin.dashboard.marketData.totalVolume24h,
                            imageUrl = infoCoin.dashboard.coinDetails.imageUrl,
                            coinName = infoCoin.dashboard.coinDetails.name,
                            currentPrice = infoCoin.dashboard.coinDetails.currentPrice,
                            priceChange24h = infoCoin.dashboard.coinDetails.priceChange24h,
                            priceChangePercentage24h = infoCoin.dashboard.coinDetails.priceChangePercentage24h,
                    )
                }

                loadGraphInternal(_uiState.value.selectedRange)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error loading graph: ${e.message}", isLoadingCoins = false) }
            }
        }
    }

    private fun updateRealTimePoint(newPoint: CoinGraphPoints) {
        _uiState.update { state ->
            val currentPoints = state.graphPoints.toMutableList()
            if (currentPoints.isEmpty()) return@update state
            
            val last = currentPoints.last()
            if (newPoint.timestamp >= last.timestamp) {
                val index = currentPoints.indexOfLast { it.timestamp == newPoint.timestamp }
                if (index != -1) {
                    currentPoints[index] = newPoint
                } else {
                    currentPoints.add(newPoint)
                    if (currentPoints.size > 50) currentPoints.removeAt(0)
                }
            }

            var newCurrentPrice = state.currentPrice
            var newPriceChange24h = state.priceChange24h
            var newPriceChangePercentage24h = state.priceChangePercentage24h

            val updatedGraph = if (state.selectedRange == ChartRangeEnum.REAL_TIME && state.infoCoin != null) {
                val currentCoin = state.infoCoin!!.dashboard.coinDetails
                val newPrice = newPoint.close
                
                val price24hAgo = currentCoin.currentPrice / (1 + (currentCoin.priceChangePercentage24h / 100))
                val newChange = newPrice - price24hAgo
                val newPercentage = (newChange / price24hAgo) * 100

                newCurrentPrice = newPrice
                newPriceChange24h = newChange
                newPriceChangePercentage24h = newPercentage

                state.infoCoin!!.copy(
                    dashboard = state.infoCoin!!.dashboard.copy(
                        coinDetails = currentCoin.copy(
                            currentPrice = newPrice,
                            priceChange24h = newChange,
                            priceChangePercentage24h = newPercentage
                        )
                    )
                )
            } else {
                state.infoCoin
            }

            state.copy(
                graphPoints = currentPoints,
                infoCoin = updatedGraph,
                currentPrice = newCurrentPrice,
                priceChange24h = newPriceChange24h,
                priceChangePercentage24h = newPriceChangePercentage24h
            )
        }
    }

    fun selectRange(range: ChartRangeEnum) {
        if (range == _uiState.value.selectedRange) return
        _uiState.update {
            it.copy(
                selectedRange = range,
                graphPoints = emptyList(),
                errorMessage = null
            )
        }
        viewModelScope.launch {
            loadGraphInternal(range)
        }
    }

    private suspend fun loadGraphInternal(range: ChartRangeEnum) {
        val symbol = _uiState.value.infoCoin?.dashboard?.coinDetails?.symbol?.uppercase() ?: return
        val points = _binanceRepository.getHistoricalKlines(symbol, range)

        _uiState.update { it.copy(
                isLoadingCoins = false,
                graphPoints = points,
                errorMessage = null,
                symbol = symbol
            )
        }
        setupWebSocket(symbol, range)
    }

    private fun setupWebSocket(symbol: String, range: ChartRangeEnum) {
        _binanceWS?.disconnect()

        if (range == ChartRangeEnum.REAL_TIME || range == ChartRangeEnum.TODAY) {
            val interval = if (range == ChartRangeEnum.REAL_TIME) "1s" else "1m"
            _binanceWS = BinanceWebSocketManager { newPoint ->
                updateRealTimePoint(newPoint)
            }
            _binanceWS?.connect(symbol, interval)
        }
    }

}
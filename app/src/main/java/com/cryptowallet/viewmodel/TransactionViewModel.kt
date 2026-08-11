package com.cryptowallet.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransactionViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(CoinUiState())
    val uiState: StateFlow<CoinUiState> = _uiState.asStateFlow()

    fun buy() {

    }

    fun sell() {

    }

    fun convertValue() {}
}


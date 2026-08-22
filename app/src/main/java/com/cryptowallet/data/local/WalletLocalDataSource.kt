package com.cryptowallet.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cryptowallet.model.WalletHolding
import com.cryptowallet.model.WalletState
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

class WalletLocalDataSource(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson = Gson(),
) {

    suspend fun getWalletState(): WalletState {
        val json = dataStore.data.first()[WALLET_STATE_KEY]
        return decodeState(json)
    }

    suspend fun saveWalletState(state: WalletState) {
        val json = gson.toJson(state)
        dataStore.edit { preferences -> preferences[WALLET_STATE_KEY] = json }
    }

    suspend fun updateWalletState(transform: (WalletState) -> WalletState): WalletState {
        val prefs = dataStore.edit { preferences ->
            val current = decodeState(preferences[WALLET_STATE_KEY])
            preferences[WALLET_STATE_KEY] = gson.toJson(transform(current))
        }
        return gson.fromJson(prefs[WALLET_STATE_KEY], WalletState::class.java)
    }

    private fun decodeState(json: String?): WalletState {
        if (json == null) return SEED_STATE
        val decoded = runCatching { gson.fromJson(json, WalletState::class.java) }.getOrNull()
        return decoded?.takeIf { it.holdings != null } ?: SEED_STATE
    }

    companion object {
        val WALLET_STATE_KEY = stringPreferencesKey("wallet_state")

        val SEED_STATE = WalletState(
            cashBalanceReais = 10000.00,
            holdings = listOf(
                WalletHolding(coinId = "bitcoin", symbol = "BTC", amount = 0.0043),
                WalletHolding(coinId = "binancecoin", symbol = "BNB", amount = 0.07),
            ),
        )
    }
}

package com.cryptowallet.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.cryptowallet.model.WalletHolding
import com.cryptowallet.model.WalletState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WalletLocalDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createDataStore(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { File(tempFolder.root, "test-${System.nanoTime()}.preferences_pb") },
        )
    }

    private fun createDataSource(dataStore: DataStore<Preferences> = createDataStore()): WalletLocalDataSource {
        return WalletLocalDataSource(dataStore)
    }

    @Test
    fun `returns the seed state when nothing has been saved yet`() = runTest {
        val dataSource = createDataSource()

        val state = dataSource.getWalletState()

        assertEquals(WalletLocalDataSource.SEED_STATE, state)
    }

    @Test
    fun `returns the previously saved state`() = runTest {
        val dataSource = createDataSource()
        val newState = WalletState(
            cashBalanceReais = 500.0,
            holdings = listOf(WalletHolding(coinId = "bitcoin", symbol = "BTC", amount = 0.01)),
        )

        dataSource.saveWalletState(newState)
        val state = dataSource.getWalletState()

        assertEquals(newState, state)
    }

    @Test
    fun `falls back to seed state when stored json is corrupted`() = runTest {
        val dataStore = createDataStore()
        val dataSource = createDataSource(dataStore)
        dataStore.edit { it[WalletLocalDataSource.WALLET_STATE_KEY] = "not valid json" }

        val state = dataSource.getWalletState()

        assertEquals(WalletLocalDataSource.SEED_STATE, state)
    }

    @Test
    fun `falls back to seed state when stored json is structurally incomplete`() = runTest {
        val dataStore = createDataStore()
        val dataSource = createDataSource(dataStore)
        dataStore.edit { it[WalletLocalDataSource.WALLET_STATE_KEY] = "{}" }

        val state = dataSource.getWalletState()

        assertEquals(WalletLocalDataSource.SEED_STATE, state)
    }

    @Test
    fun `updateWalletState atomically transforms the current state`() = runTest {
        val dataSource = createDataSource()

        val updated = dataSource.updateWalletState { it.copy(cashBalanceReais = it.cashBalanceReais - 100.0) }

        assertEquals(
            WalletLocalDataSource.SEED_STATE.cashBalanceReais - 100.0,
            updated.cashBalanceReais,
            0.0001,
        )
        val state = dataSource.getWalletState()
        assertEquals(
            WalletLocalDataSource.SEED_STATE.cashBalanceReais - 100.0,
            state.cashBalanceReais,
            0.0001,
        )
    }
}

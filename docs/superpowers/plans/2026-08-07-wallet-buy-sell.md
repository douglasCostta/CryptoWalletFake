# Wallet Buy/Sell Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Git note:** the user has asked that nothing be committed and no PR be created until they've validated the work. **Skip every "commit" step a template would normally include.** Do not run `git add`/`git commit`/`git push` at any point while executing this plan — leave all changes staged/unstaged in the working tree for manual review.

**Goal:** Add a Wallet → Buy/Sell → Transaction-success flow to the CryptoWalletFake app, simulating balance and trades entirely client-side (no real backend), on top of the existing CoinGecko-backed market data layer.

**Architecture:** MVVM, matching existing conventions (plain `ViewModel` + `StateFlow`, manual construction, no DI framework). New `WalletRepository` composes the existing `CoinGeckoRepository` (live BRL prices) with a new `WalletLocalDataSource` (Preferences DataStore, JSON-serialized `WalletState`) to simulate a wallet with no server-side balance/transaction API.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose (type-safe routes via kotlinx.serialization), Retrofit/Gson (existing), AndroidX DataStore Preferences (new), kotlinx-coroutines-test (new, test-only).

Spec: `docs/superpowers/specs/2026-08-07-wallet-buy-sell-design.md`

---

### Task 1: Add new dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add version entries**

In `gradle/libs.versions.toml`, add to `[versions]` (after `composeBom = "2024.09.00"`):

```toml
datastorePreferences = "1.1.1"
kotlinxSerializationJson = "1.7.3"
kotlinxCoroutinesTest = "1.8.1"
lifecycleRuntimeCompose = "2.11.0"
```

- [ ] **Step 2: Add library entries**

In `gradle/libs.versions.toml`, add to `[libraries]` (after `androidx-compose-material3 = ...`):

```toml
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeCompose" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutinesTest" }
```

- [ ] **Step 3: Add the serialization plugin entry**

In `gradle/libs.versions.toml`, add to `[plugins]` (after `kotlin-compose = ...`):

```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 4: Register the plugin at the root**

In `build.gradle.kts`, change:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

to:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 5: Apply the plugin and add dependencies in the app module**

In `app/build.gradle.kts`, change:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}
```

to:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
```

Then add these lines to the `dependencies { }` block (after the existing `implementation("com.patrykandpatrick.vico:core:2.0.0")` line):

```kotlin
implementation(libs.androidx.datastore.preferences)
implementation(libs.androidx.lifecycle.runtime.compose)
implementation(libs.kotlinx.serialization.json)
testImplementation(libs.kotlinx.coroutines.test)
```

- [ ] **Step 6: Verify the project still syncs/compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

---

### Task 2: Wallet domain models

**Files:**
- Create: `app/src/main/java/com/cryptowallet/model/WalletModels.kt`

No test needed for this task — these are plain data holders with no behavior to verify (mirrors the existing `CoinGeckoModels.kt`, which also has no dedicated test).

- [ ] **Step 1: Create the models file**

```kotlin
package com.cryptowallet.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType { BUY, SELL }

data class WalletHolding(
    val coinId: String,
    val symbol: String,
    val amount: Double,
)

data class WalletState(
    val cashBalanceReais: Double,
    val holdings: List<WalletHolding>,
)

data class CoinBalance(
    val coin: CoinListItem,
    val amountOwned: Double,
    val valueInReais: Double,
)

data class WalletBalance(
    val totalReais: Double,
    val changePercentage24h: Double,
)

data class TransactionRequest(
    val type: TransactionType,
    val coinId: String,
    val amountInReais: Double,
    val amountInCoin: Double,
)

data class TransactionResult(
    val type: TransactionType,
    val coinSymbol: String,
    val amountInCoin: Double,
    val newWalletState: WalletState,
)
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

---

### Task 3: Extend `CoinGeckoRepository` with BRL support and multi-id lookup

The wallet feature needs prices in BRL (the existing repository only offers whatever `vs_currency` the caller hardcodes) and needs to fetch several specific coins at once (existing code only fetches one at a time via `getCoinDashboard`). Both additions use default parameters so `TesteViewModel`/`DashboardViewModel` keep compiling unchanged.

**Files:**
- Create: `app/src/test/java/com/cryptowallet/fakes/FakeCoinGeckoApi.kt`
- Create: `app/src/test/java/com/cryptowallet/data/repository/CoinGeckoRepositoryTest.kt`
- Modify: `app/src/main/java/com/cryptowallet/data/repository/CoinGeckoRepository.kt`

- [ ] **Step 1: Create a fake `CoinGeckoApi` for tests**

```kotlin
package com.cryptowallet.fakes

import com.cryptowallet.data.remote.api.CoinGeckoApi
import com.cryptowallet.data.remote.dto.CoinGeckoCoin

class FakeCoinGeckoApi(
    private val coins: List<CoinGeckoCoin> = defaultCoins(),
) : CoinGeckoApi {

    var lastVsCurrency: String? = null
    var lastIds: String? = null

    override suspend fun getAllCoins(vsCurrency: String): List<CoinGeckoCoin> {
        lastVsCurrency = vsCurrency
        return coins
    }

    override suspend fun getCoinById(vsCurrency: String, ids: String): List<CoinGeckoCoin> {
        lastVsCurrency = vsCurrency
        lastIds = ids
        val requestedIds = ids.split(",")
        return coins.filter { it.id in requestedIds }
    }

    override suspend fun getCoinOhlcGraph(id: String, vsCurrency: String, days: String): List<List<Double>> {
        return emptyList()
    }

    companion object {
        fun defaultCoins(): List<CoinGeckoCoin> = listOf(
            CoinGeckoCoin(
                id = "bitcoin", symbol = "btc", name = "Bitcoin", image = "",
                currentPrice = 262515.351, priceChange24h = 1000.0, priceChangePercentage24h = 0.4,
            ),
            CoinGeckoCoin(
                id = "ethereum", symbol = "eth", name = "Ethereum", image = "",
                currentPrice = 1939.74, priceChange24h = -20.0, priceChangePercentage24h = -1.02,
            ),
            CoinGeckoCoin(
                id = "binancecoin", symbol = "bnb", name = "BNB", image = "",
                currentPrice = 600.0, priceChange24h = 5.0, priceChangePercentage24h = 0.83,
            ),
            CoinGeckoCoin(
                id = "usd-coin", symbol = "usdc", name = "USD Coin", image = "",
                currentPrice = 5.0, priceChange24h = 0.0, priceChangePercentage24h = 0.0,
            ),
        )
    }
}
```

- [ ] **Step 2: Write the failing tests**

```kotlin
package com.cryptowallet.data.repository

import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.fakes.FakeCoinGeckoApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CoinGeckoRepositoryTest {

    @Test
    fun `getCoins passes vsCurrency through to the API`() = runTest {
        val api = FakeCoinGeckoApi()
        val repository = CoinGeckoRepository(CoinGeckoService(api))

        repository.getCoins(vsCurrency = "brl")

        assertEquals("brl", api.lastVsCurrency)
    }

    @Test
    fun `getCoinsByIds requests only the given ids and maps them`() = runTest {
        val api = FakeCoinGeckoApi()
        val repository = CoinGeckoRepository(CoinGeckoService(api))

        val result = repository.getCoinsByIds(ids = listOf("bitcoin", "ethereum"), vsCurrency = "brl")

        assertEquals("brl", api.lastVsCurrency)
        assertEquals("bitcoin,ethereum", api.lastIds)
        assertEquals(setOf("bitcoin", "ethereum"), result.map { it.id }.toSet())
    }

    @Test
    fun `getCoinsByIds returns empty list for empty ids without calling the API`() = runTest {
        val api = FakeCoinGeckoApi()
        val repository = CoinGeckoRepository(CoinGeckoService(api))

        val result = repository.getCoinsByIds(ids = emptyList(), vsCurrency = "brl")

        assertEquals(emptyList<String>(), result.map { it.id })
        assertEquals(null, api.lastIds)
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cryptowallet.data.repository.CoinGeckoRepositoryTest"`
Expected: FAIL — `getCoinsByIds` is unresolved (doesn't exist yet), `getCoins` doesn't accept a `vsCurrency` argument yet.

- [ ] **Step 4: Update `CoinGeckoRepository`**

Change `app/src/main/java/com/cryptowallet/data/repository/CoinGeckoRepository.kt` from:

```kotlin
package com.cryptowallet.data.repository

import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.model.ChartRange
import com.cryptowallet.model.CoinChartPoint
import com.cryptowallet.model.CoinDashboard
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.GraphCoinDashboard
import com.cryptowallet.model.toCoinChartPointOrNull
import com.cryptowallet.model.toCoinDashboard
import com.cryptowallet.model.toCoinListItem

class CoinGeckoRepository(private val service: CoinGeckoService) {

    suspend fun getCoins(): List<CoinListItem> {
        return service.getAllCoins().map{ it.toCoinListItem() }
    }
```

to:

```kotlin
package com.cryptowallet.data.repository

import com.cryptowallet.data.remote.constants.CurrencyConstants
import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.model.ChartRange
import com.cryptowallet.model.CoinChartPoint
import com.cryptowallet.model.CoinDashboard
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.GraphCoinDashboard
import com.cryptowallet.model.toCoinChartPointOrNull
import com.cryptowallet.model.toCoinDashboard
import com.cryptowallet.model.toCoinListItem

class CoinGeckoRepository(private val service: CoinGeckoService) {

    suspend fun getCoins(vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY): List<CoinListItem> {
        return service.getAllCoins(vsCurrency = vsCurrency).map { it.toCoinListItem() }
    }

    suspend fun getCoinsByIds(
        ids: List<String>,
        vsCurrency: String = CurrencyConstants.DEFAULT_VS_CURRENCY,
    ): List<CoinListItem> {
        if (ids.isEmpty()) return emptyList()
        return service.getCoinById(vsCurrency = vsCurrency, ids = ids.joinToString(",")).map { it.toCoinListItem() }
    }
```

Leave the rest of the file (`getCoinDashboard`, `getCoinChart`, `getCoinDashboardPayload`) unchanged.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cryptowallet.data.repository.CoinGeckoRepositoryTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

---

### Task 4: `WalletLocalDataSource` (DataStore-backed persistence)

**Files:**
- Create: `app/src/main/java/com/cryptowallet/data/local/WalletDataStore.kt`
- Create: `app/src/main/java/com/cryptowallet/data/local/WalletLocalDataSource.kt`
- Create: `app/src/test/java/com/cryptowallet/data/local/WalletLocalDataSourceTest.kt`

- [ ] **Step 1: Create the DataStore extension property**

```kotlin
package com.cryptowallet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.walletDataStore: DataStore<Preferences> by preferencesDataStore(name = "wallet")
```

- [ ] **Step 2: Write the failing tests for `WalletLocalDataSource`**

```kotlin
package com.cryptowallet.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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

    private fun createDataSource(): WalletLocalDataSource {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempFolder.root, "test-${System.nanoTime()}.preferences_pb") },
        )
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
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cryptowallet.data.local.WalletLocalDataSourceTest"`
Expected: FAIL — `WalletLocalDataSource` doesn't exist yet (compile error).

- [ ] **Step 4: Implement `WalletLocalDataSource`**

```kotlin
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

    /**
     * Atomically reads, transforms, and persists the wallet state within a single
     * DataStore `edit` transaction, so concurrent buy/sell calls can't clobber each other.
     */
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
        // Gson bypasses Kotlin's constructor via reflection, so a structurally-incomplete
        // stored value (e.g. "{}") can still decode successfully with a null `holdings`
        // despite the non-nullable Kotlin type — guard against that explicitly.
        return decoded?.takeIf { it.holdings != null } ?: SEED_STATE
    }

    companion object {
        val WALLET_STATE_KEY = stringPreferencesKey("wallet_state")

        val SEED_STATE = WalletState(
            cashBalanceReais = 1074.32,
            holdings = listOf(
                WalletHolding(coinId = "bitcoin", symbol = "BTC", amount = 0.0043),
                WalletHolding(coinId = "ethereum", symbol = "ETH", amount = 0.039),
                WalletHolding(coinId = "binancecoin", symbol = "BNB", amount = 0.07),
                WalletHolding(coinId = "usd-coin", symbol = "USDC", amount = 2478.0),
            ),
        )
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cryptowallet.data.local.WalletLocalDataSourceTest"`
Expected: `BUILD SUCCESSFUL`. (Code review during execution found the initial 2-test version had two gaps — a read-modify-write race and no fallback for corrupted/incomplete stored JSON — both were fixed during Task 4's execution and are now covered by 5 tests total: seed default, save/read roundtrip, corrupted-JSON fallback, structurally-incomplete-JSON fallback, and atomic `updateWalletState`.)

**Note for Task 5:** `WalletRepositoryImpl.buy`/`sell` must use `updateWalletState { state -> ... }` for the read-modify-write, NOT separate `getWalletState()` + `saveWalletState()` calls — the latter has the race condition described above.

---

### Task 5: `WalletRepository` interface + `WalletRepositoryImpl`

**Files:**
- Create: `app/src/main/java/com/cryptowallet/data/repository/WalletRepository.kt`
- Create: `app/src/main/java/com/cryptowallet/data/repository/WalletRepositoryImpl.kt`
- Create: `app/src/test/java/com/cryptowallet/data/repository/WalletRepositoryImplTest.kt`

- [ ] **Step 1: Create the `WalletRepository` interface**

```kotlin
package com.cryptowallet.data.repository

import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.TransactionRequest
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.WalletBalance
import com.cryptowallet.model.WalletState

interface WalletRepository {
    suspend fun getWalletBalance(): WalletBalance
    suspend fun getCoinBalances(): List<CoinBalance>
    suspend fun getAllCoins(): List<CoinListItem>
    suspend fun getTradableCoins(): List<CoinListItem>
    suspend fun getWalletState(): WalletState
    suspend fun getCoinPrice(coinId: String): Double
    suspend fun buy(request: TransactionRequest): TransactionResult
    suspend fun sell(request: TransactionRequest): TransactionResult
}
```

- [ ] **Step 2: Write the failing tests for `WalletRepositoryImpl`**

```kotlin
package com.cryptowallet.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.cryptowallet.data.local.WalletLocalDataSource
import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.fakes.FakeCoinGeckoApi
import com.cryptowallet.model.TransactionRequest
import com.cryptowallet.model.TransactionType
import com.cryptowallet.model.WalletHolding
import com.cryptowallet.model.WalletState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WalletRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun localDataSource(): WalletLocalDataSource {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempFolder.root, "test-${System.nanoTime()}.preferences_pb") },
        )
        return WalletLocalDataSource(dataStore)
    }

    private fun repository(local: WalletLocalDataSource) = WalletRepositoryImpl(
        coinGeckoRepository = CoinGeckoRepository(CoinGeckoService(FakeCoinGeckoApi())),
        localDataSource = local,
    )

    @Test
    fun `getWalletBalance sums cash plus the reais value of all holdings`() = runTest {
        val local = localDataSource()
        local.saveWalletState(
            WalletState(
                cashBalanceReais = 1000.0,
                holdings = listOf(
                    WalletHolding(coinId = "bitcoin", symbol = "BTC", amount = 0.01),
                    WalletHolding(coinId = "ethereum", symbol = "ETH", amount = 1.0),
                ),
            ),
        )
        val repo = repository(local)

        val balance = repo.getWalletBalance()

        val expectedTotal = 1000.0 + (262515.351 * 0.01) + (1939.74 * 1.0)
        assertEquals(expectedTotal, balance.totalReais, 0.001)
    }

    @Test
    fun `getCoinBalances merges owned amount with the live price`() = runTest {
        val local = localDataSource()
        local.saveWalletState(
            WalletState(
                cashBalanceReais = 0.0,
                holdings = listOf(WalletHolding(coinId = "ethereum", symbol = "ETH", amount = 2.0)),
            ),
        )
        val repo = repository(local)

        val balances = repo.getCoinBalances()

        assertEquals(1, balances.size)
        assertEquals("ethereum", balances[0].coin.id)
        assertEquals(2.0, balances[0].amountOwned, 0.0)
        assertEquals(1939.74 * 2.0, balances[0].valueInReais, 0.001)
    }

    @Test
    fun `buy deducts cash and increases the coin holding`() = runTest {
        val local = localDataSource()
        local.saveWalletState(
            WalletState(cashBalanceReais = 1000.0, holdings = listOf(WalletHolding("bitcoin", "BTC", 0.0))),
        )
        val repo = repository(local)

        val result = repo.buy(
            TransactionRequest(TransactionType.BUY, coinId = "bitcoin", amountInReais = 100.0, amountInCoin = 0.0003810),
        )

        assertEquals(TransactionType.BUY, result.type)
        assertEquals(0.0003810, result.amountInCoin, 0.0000001)
        val newState = local.getWalletState()
        assertEquals(900.0, newState.cashBalanceReais, 0.001)
        assertEquals(0.0003810, newState.holdings.first { it.coinId == "bitcoin" }.amount, 0.0000001)
    }

    @Test(expected = IllegalStateException::class)
    fun `buy fails when amountInReais exceeds available cash`() = runTest {
        val local = localDataSource()
        local.saveWalletState(
            WalletState(cashBalanceReais = 50.0, holdings = listOf(WalletHolding("bitcoin", "BTC", 0.0))),
        )
        val repo = repository(local)

        repo.buy(TransactionRequest(TransactionType.BUY, coinId = "bitcoin", amountInReais = 100.0, amountInCoin = 0.0003810))
    }

    @Test
    fun `sell increases cash and decreases the coin holding`() = runTest {
        val local = localDataSource()
        local.saveWalletState(
            WalletState(cashBalanceReais = 0.0, holdings = listOf(WalletHolding("ethereum", "ETH", 1.0))),
        )
        val repo = repository(local)

        val result = repo.sell(
            TransactionRequest(TransactionType.SELL, coinId = "ethereum", amountInReais = 1939.74, amountInCoin = 1.0),
        )

        assertEquals(TransactionType.SELL, result.type)
        val newState = local.getWalletState()
        assertEquals(1939.74, newState.cashBalanceReais, 0.001)
        assertEquals(0.0, newState.holdings.first { it.coinId == "ethereum" }.amount, 0.0000001)
    }

    @Test(expected = IllegalStateException::class)
    fun `sell fails when amountInCoin exceeds owned amount`() = runTest {
        val local = localDataSource()
        local.saveWalletState(
            WalletState(cashBalanceReais = 0.0, holdings = listOf(WalletHolding("ethereum", "ETH", 0.5))),
        )
        val repo = repository(local)

        repo.sell(TransactionRequest(TransactionType.SELL, coinId = "ethereum", amountInReais = 1939.74, amountInCoin = 1.0))
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cryptowallet.data.repository.WalletRepositoryImplTest"`
Expected: FAIL — `WalletRepositoryImpl` doesn't exist yet (compile error).

- [ ] **Step 4: Implement `WalletRepositoryImpl`**

```kotlin
package com.cryptowallet.data.repository

import com.cryptowallet.data.local.WalletLocalDataSource
import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.TransactionRequest
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.TransactionType
import com.cryptowallet.model.WalletBalance
import com.cryptowallet.model.WalletHolding
import com.cryptowallet.model.WalletState

class WalletRepositoryImpl(
    private val coinGeckoRepository: CoinGeckoRepository,
    private val localDataSource: WalletLocalDataSource,
) : WalletRepository {

    override suspend fun getTradableCoins(): List<CoinListItem> =
        coinGeckoRepository.getCoinsByIds(ids = SUPPORTED_COIN_IDS, vsCurrency = VS_CURRENCY)

    override suspend fun getCoinBalances(): List<CoinBalance> {
        val state = localDataSource.getWalletState()
        val coins = getTradableCoins()
        return state.holdings.mapNotNull { holding ->
            val coin = coins.find { it.id == holding.coinId } ?: return@mapNotNull null
            CoinBalance(
                coin = coin,
                amountOwned = holding.amount,
                valueInReais = holding.amount * coin.currentPrice,
            )
        }
    }

    override suspend fun getAllCoins(): List<CoinListItem> =
        coinGeckoRepository.getCoins(vsCurrency = VS_CURRENCY)

    override suspend fun getWalletState(): WalletState = localDataSource.getWalletState()

    override suspend fun getWalletBalance(): WalletBalance {
        val state = localDataSource.getWalletState()
        val coinBalances = getCoinBalances()

        val totalReais = state.cashBalanceReais + coinBalances.sumOf { it.valueInReais }
        val totalPrevious = state.cashBalanceReais + coinBalances.sumOf { balance ->
            val previousPrice = balance.coin.currentPrice - balance.coin.priceChange24h
            balance.amountOwned * previousPrice
        }
        val changePercentage = if (totalPrevious == 0.0) {
            0.0
        } else {
            ((totalReais - totalPrevious) / totalPrevious) * 100.0
        }

        return WalletBalance(totalReais = totalReais, changePercentage24h = changePercentage)
    }

    override suspend fun getCoinPrice(coinId: String): Double =
        getTradableCoins().find { it.id == coinId }?.currentPrice
            ?: error("Coin not found: $coinId")

    override suspend fun buy(request: TransactionRequest): TransactionResult = execute(request)

    override suspend fun sell(request: TransactionRequest): TransactionResult = execute(request)

    private suspend fun execute(request: TransactionRequest): TransactionResult {
        var coinSymbol = request.coinId

        val newState = localDataSource.updateWalletState { state ->
            val holding = state.holdings.find { it.coinId == request.coinId }
                ?: error("Unknown coin in wallet: ${request.coinId}")
            coinSymbol = holding.symbol

            when (request.type) {
                TransactionType.BUY -> {
                    check(request.amountInReais <= state.cashBalanceReais) {
                        "Saldo insuficiente em R$ para completar a compra."
                    }
                    state.copy(
                        cashBalanceReais = state.cashBalanceReais - request.amountInReais,
                        holdings = state.holdings.updateAmount(request.coinId) { it + request.amountInCoin },
                    )
                }
                TransactionType.SELL -> {
                    check(request.amountInCoin <= holding.amount) {
                        "Saldo insuficiente da moeda para completar a venda."
                    }
                    state.copy(
                        cashBalanceReais = state.cashBalanceReais + request.amountInReais,
                        holdings = state.holdings.updateAmount(request.coinId) { it - request.amountInCoin },
                    )
                }
            }
        }

        return TransactionResult(
            type = request.type,
            coinSymbol = coinSymbol,
            amountInCoin = request.amountInCoin,
            newWalletState = newState,
        )
    }

    private fun List<WalletHolding>.updateAmount(coinId: String, transform: (Double) -> Double): List<WalletHolding> =
        map { if (it.coinId == coinId) it.copy(amount = transform(it.amount)) else it }

    companion object {
        const val VS_CURRENCY = "brl"
        val SUPPORTED_COIN_IDS = listOf("bitcoin", "ethereum", "binancecoin", "usd-coin")
    }
}
```

Note: `execute()` uses `localDataSource.updateWalletState { }` (added during Task 4's execution) rather than separate `getWalletState()`/`saveWalletState()` calls, to keep the read-check-mutate sequence atomic. It also fails loudly (`error(...)`) if `request.coinId` isn't a known holding, rather than silently no-op-ing the credit/debit — a gap a code review caught during execution.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cryptowallet.data.repository.WalletRepositoryImplTest"`
Expected: `BUILD SUCCESSFUL`. (Code review during execution added an "unknown coin" failure test and strengthened the two insufficient-funds/insufficient-holdings tests to also assert persisted state is unchanged after a failure — 7 tests total.)

---

### Task 6: Test infrastructure for ViewModel tests

**Files:**
- Create: `app/src/test/java/com/cryptowallet/MainDispatcherRule.kt`
- Create: `app/src/test/java/com/cryptowallet/fakes/FakeWalletRepository.kt`

No behavior to assert here — this is shared test scaffolding consumed by Tasks 7 and 8.

- [ ] **Step 1: Create the main-dispatcher JUnit rule**

`viewModelScope.launch` dispatches onto `Dispatchers.Main` by default, which isn't available in a plain JVM unit test. This rule swaps it for a `TestDispatcher` for the duration of each test.

```kotlin
package com.cryptowallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

- [ ] **Step 2: Create the fake `WalletRepository`**

```kotlin
package com.cryptowallet.fakes

import com.cryptowallet.data.repository.WalletRepository
import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.TransactionRequest
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.WalletBalance
import com.cryptowallet.model.WalletState

class FakeWalletRepository(
    var walletBalanceProvider: suspend () -> WalletBalance = {
        WalletBalance(totalReais = 20802.0, changePercentage24h = 4.23)
    },
    var coinBalancesProvider: suspend () -> List<CoinBalance> = { emptyList() },
    var allCoinsProvider: suspend () -> List<CoinListItem> = { emptyList() },
    var tradableCoinsProvider: suspend () -> List<CoinListItem> = { emptyList() },
    var walletStateProvider: suspend () -> WalletState = {
        WalletState(cashBalanceReais = 1074.32, holdings = emptyList())
    },
    var transactionProvider: suspend (TransactionRequest) -> TransactionResult = {
        error("transactionProvider not configured in fake")
    },
) : WalletRepository {

    var lastRequest: TransactionRequest? = null

    override suspend fun getWalletBalance(): WalletBalance = walletBalanceProvider()
    override suspend fun getCoinBalances(): List<CoinBalance> = coinBalancesProvider()
    override suspend fun getAllCoins(): List<CoinListItem> = allCoinsProvider()
    override suspend fun getTradableCoins(): List<CoinListItem> = tradableCoinsProvider()
    override suspend fun getWalletState(): WalletState = walletStateProvider()

    override suspend fun getCoinPrice(coinId: String): Double =
        tradableCoinsProvider().find { it.id == coinId }?.currentPrice
            ?: error("Coin not found: $coinId")

    override suspend fun buy(request: TransactionRequest): TransactionResult {
        lastRequest = request
        return transactionProvider(request)
    }

    override suspend fun sell(request: TransactionRequest): TransactionResult {
        lastRequest = request
        return transactionProvider(request)
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugUnitTestKotlin`
Expected: `BUILD SUCCESSFUL`

---

### Task 7: `WalletViewModel`

**Files:**
- Create: `app/src/main/java/com/cryptowallet/viewmodel/WalletViewModel.kt`
- Create: `app/src/test/java/com/cryptowallet/viewmodel/WalletViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.cryptowallet.viewmodel

import com.cryptowallet.MainDispatcherRule
import com.cryptowallet.fakes.FakeWalletRepository
import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.WalletBalance
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class WalletViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun ethCoin() = CoinListItem(
        id = "ethereum", symbol = "eth", name = "Ethereum", imageUrl = "",
        currentPrice = 1939.74, priceChange24h = -20.0, priceChangePercentage24h = -1.02, marketCapRank = 2,
    )

    @Test
    fun `loads balance and coin lists on init`() = runTest {
        val repository = FakeWalletRepository(
            walletBalanceProvider = { WalletBalance(totalReais = 20802.0, changePercentage24h = 4.23) },
            coinBalancesProvider = { listOf(CoinBalance(coin = ethCoin(), amountOwned = 39.74, valueInReais = 39.74 * 1939.74)) },
            allCoinsProvider = { listOf(ethCoin()) },
        )

        val viewModel = WalletViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(20802.0, state.balance?.totalReais)
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
            walletBalanceProvider = { error("network down") },
        )

        val viewModel = WalletViewModel(repository)
        advanceUntilIdle()

        assertEquals("network down", viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cryptowallet.viewmodel.WalletViewModelTest"`
Expected: FAIL — `WalletViewModel`/`WalletTab` don't exist yet (compile error).

- [ ] **Step 3: Implement `WalletViewModel`**

```kotlin
package com.cryptowallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.data.repository.WalletRepository
import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.WalletBalance
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
    val allCoins: List<CoinListItem> = emptyList(),
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching {
                Triple(repository.getWalletBalance(), repository.getCoinBalances(), repository.getAllCoins())
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
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cryptowallet.viewmodel.WalletViewModelTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

---

### Task 8: `BuySellViewModel`

**Files:**
- Create: `app/src/main/java/com/cryptowallet/viewmodel/BuySellViewModel.kt`
- Create: `app/src/test/java/com/cryptowallet/viewmodel/BuySellViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.cryptowallet.viewmodel

import com.cryptowallet.MainDispatcherRule
import com.cryptowallet.fakes.FakeWalletRepository
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.TransactionType
import com.cryptowallet.model.WalletHolding
import com.cryptowallet.model.WalletState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

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
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cryptowallet.viewmodel.BuySellViewModelTest"`
Expected: FAIL — `BuySellViewModel` doesn't exist yet (compile error).

- [ ] **Step 3: Implement `BuySellViewModel`**

```kotlin
package com.cryptowallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.data.repository.WalletRepository
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.TransactionRequest
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.TransactionType
import com.cryptowallet.model.WalletState
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
        viewModelScope.launch {
            runCatching { repository.getWalletState() }
                .onSuccess { state -> _uiState.update { it.copy(balanceBefore = state) } }
                .onFailure { throwable ->
                    _uiState.update { it.copy(errorMessage = throwable.message ?: "Erro ao carregar saldo.") }
                }
        }
    }

    fun onAmountInReaisChanged(text: String) {
        val price = _uiState.value.currentPrice
        val reais = text.toDoubleOrNull()
        val coinAmount = if (reais != null && price > 0.0) reais / price else null
        _uiState.update {
            val updated = it.copy(amountInReais = text, amountInCoin = coinAmount?.toString().orEmpty())
            updated.copy(balanceAfter = computeBalanceAfter(updated))
        }
    }

    fun onAmountInCoinChanged(text: String) {
        val price = _uiState.value.currentPrice
        val coinAmount = text.toDoubleOrNull()
        val reais = coinAmount?.let { it * price }
        _uiState.update {
            val updated = it.copy(amountInCoin = text, amountInReais = reais?.toString().orEmpty())
            updated.copy(balanceAfter = computeBalanceAfter(updated))
        }
    }

    fun onConfirmClick() {
        val state = _uiState.value
        val coin = state.selectedCoin ?: return
        val amountInReais = state.amountInReais.toDoubleOrNull() ?: return
        val amountInCoin = state.amountInCoin.toDoubleOrNull() ?: return

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
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.cryptowallet.viewmodel.BuySellViewModelTest"`
Expected: `BUILD SUCCESSFUL`, 5 tests passed.

---

### Task 9: Navigation routes

**Files:**
- Create: `app/src/main/java/com/cryptowallet/navigation/Routes.kt`

- [ ] **Step 1: Create the sealed route hierarchy**

```kotlin
package com.cryptowallet.navigation

import com.cryptowallet.model.TransactionType
import kotlinx.serialization.Serializable

sealed class Routes {

    @Serializable
    data object Wallet : Routes()

    @Serializable
    data class Trade(val type: TransactionType) : Routes()

    @Serializable
    data class TransactionSuccess(
        val type: TransactionType,
        val coinSymbol: String,
        val amount: Double,
    ) : Routes()
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

---

### Task 10: `WalletScreen` composable

**Files:**
- Create: `app/src/main/java/com/cryptowallet/view/WalletScreen.kt`

No automated test — this codebase has no Compose UI test infrastructure set up (only `TesteScreen.kt` exists as a precedent, and it isn't tested either). Verification happens by running the app in Task 14.

- [ ] **Step 1: Create the screen**

```kotlin
package com.cryptowallet.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.viewmodel.WalletTab
import com.cryptowallet.viewmodel.WalletUiState
import com.cryptowallet.viewmodel.WalletViewModel
import java.text.NumberFormat
import java.util.Locale

private val ReaisFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WalletContent(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onBuyClick = onBuyClick,
        onSellClick = onSellClick,
    )
}

@Composable
private fun WalletContent(
    uiState: WalletUiState,
    onTabSelected: (WalletTab) -> Unit,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
) {
    if (uiState.isLoading && uiState.balance == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Wallet", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        uiState.errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        uiState.balance?.let { balance ->
            Text(
                text = ReaisFormatter.format(balance.totalReais),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            val changeColor = if (balance.changePercentage24h < 0) Color(0xFFFF6B6B) else Color(0xFF35C759)
            Text(
                text = String.format(Locale.US, "%+.2f%% Last 24 hours", balance.changePercentage24h),
                color = changeColor,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBuyClick, modifier = Modifier.fillMaxWidth().weight(1f)) { Text("Buy") }
            Button(onClick = onSellClick, modifier = Modifier.fillMaxWidth().weight(1f)) { Text("Sell") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
            Tab(
                selected = uiState.selectedTab == WalletTab.YOUR_COINS,
                onClick = { onTabSelected(WalletTab.YOUR_COINS) },
                text = { Text("Your coins") },
            )
            Tab(
                selected = uiState.selectedTab == WalletTab.ALL_COINS,
                onClick = { onTabSelected(WalletTab.ALL_COINS) },
                text = { Text("All coins") },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (uiState.selectedTab) {
            WalletTab.YOUR_COINS -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = uiState.yourCoins, key = { it.coin.id }) { balance -> YourCoinRow(balance) }
            }
            WalletTab.ALL_COINS -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = uiState.allCoins, key = { it.id }) { coin -> AllCoinRow(coin) }
            }
        }
    }
}

@Composable
private fun YourCoinRow(balance: CoinBalance) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = balance.coin.symbol.uppercase(), fontWeight = FontWeight.Bold)
                Text(text = String.format(Locale.US, "%.6f", balance.amountOwned), style = MaterialTheme.typography.bodySmall)
            }
            Text(text = ReaisFormatter.format(balance.valueInReais))
        }
    }
}

@Composable
private fun AllCoinRow(coin: CoinListItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = coin.name)
            Text(text = ReaisFormatter.format(coin.currentPrice))
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

---

### Task 11: `BuySellScreen` composable

**Files:**
- Create: `app/src/main/java/com/cryptowallet/view/BuySellScreen.kt`

- [ ] **Step 1: Create the screen**

```kotlin
package com.cryptowallet.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.TransactionType
import com.cryptowallet.viewmodel.BuySellViewModel
import com.cryptowallet.viewmodel.TradeUiState
import java.text.NumberFormat
import java.util.Locale

private val ReaisFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

@Composable
fun BuySellScreen(
    viewModel: BuySellViewModel,
    onConfirmed: (TransactionResult) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.result) {
        uiState.result?.let(onConfirmed)
    }

    BuySellContent(
        uiState = uiState,
        onCoinSelected = viewModel::onCoinSelected,
        onAmountInReaisChanged = viewModel::onAmountInReaisChanged,
        onAmountInCoinChanged = viewModel::onAmountInCoinChanged,
        onConfirmClick = viewModel::onConfirmClick,
    )
}

@Composable
private fun BuySellContent(
    uiState: TradeUiState,
    onCoinSelected: (CoinListItem) -> Unit,
    onAmountInReaisChanged: (String) -> Unit,
    onAmountInCoinChanged: (String) -> Unit,
    onConfirmClick: () -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val title = if (uiState.type == TransactionType.BUY) "Buy" else "Sell"
    val symbol = uiState.selectedCoin?.symbol?.uppercase().orEmpty()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Box {
            OutlinedButton(onClick = { dropdownExpanded = true }) {
                Text(text = uiState.selectedCoin?.name ?: "Selecione uma moeda")
            }
            DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                uiState.availableCoins.forEach { coin ->
                    DropdownMenuItem(
                        text = { Text(coin.name) },
                        onClick = {
                            onCoinSelected(coin)
                            dropdownExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Current Price: ${ReaisFormatter.format(uiState.currentPrice)}")

        Spacer(modifier = Modifier.height(16.dp))
        uiState.balanceBefore?.let { before ->
            val ownedBefore = before.holdings.find { it.coinId == uiState.selectedCoin?.id }?.amount ?: 0.0
            Text(text = "Balance before")
            Text(text = "R$ ${"%.2f".format(before.cashBalanceReais)}   $symbol ${"%.6f".format(ownedBefore)}")
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.amountInReais,
            onValueChange = onAmountInReaisChanged,
            label = { Text("Amount in R$") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.amountInCoin,
            onValueChange = onAmountInCoinChanged,
            label = { Text("Amount in $symbol") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))
        uiState.balanceAfter?.let { after ->
            val ownedAfter = after.holdings.find { it.coinId == uiState.selectedCoin?.id }?.amount ?: 0.0
            Text(text = "Balance after")
            Text(text = "R$ ${"%.2f".format(after.cashBalanceReais)}   $symbol ${"%.6f".format(ownedAfter)}")
        }

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onConfirmClick,
            enabled = !uiState.isLoading && uiState.selectedCoin != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Confirm")
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

---

### Task 12: `TransactionSuccessScreen` composable

**Files:**
- Create: `app/src/main/java/com/cryptowallet/view/TransactionSuccessScreen.kt`

- [ ] **Step 1: Create the screen**

```kotlin
package com.cryptowallet.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cryptowallet.model.TransactionType
import java.util.Locale

@Composable
fun TransactionSuccessScreen(
    type: TransactionType,
    coinSymbol: String,
    amount: Double,
    onBackToWallet: () -> Unit,
) {
    val verb = if (type == TransactionType.BUY) "bought" else "sold"

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF35C759),
            modifier = Modifier.size(96.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Transaction Completed", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "You successfully $verb ${String.format(Locale.US, "%.6f", amount)} $coinSymbol")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBackToWallet, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Wallet")
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

---

### Task 13: `CryptoWalletNavHost` and wiring `MainActivity`

**Files:**
- Create: `app/src/main/java/com/cryptowallet/navigation/CryptoWalletNavHost.kt`
- Modify: `app/src/main/java/com/cryptowallet/MainActivity.kt`

- [ ] **Step 1: Create the NavHost**

```kotlin
package com.cryptowallet.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cryptowallet.data.repository.WalletRepository
import com.cryptowallet.model.TransactionType
import com.cryptowallet.view.BuySellScreen
import com.cryptowallet.view.TransactionSuccessScreen
import com.cryptowallet.view.WalletScreen
import com.cryptowallet.viewmodel.BuySellViewModel
import com.cryptowallet.viewmodel.WalletViewModel

@Composable
fun CryptoWalletNavHost(
    walletRepository: WalletRepository,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.Wallet) {
        composable<Routes.Wallet> {
            val viewModel: WalletViewModel = viewModel(
                factory = viewModelFactory { initializer { WalletViewModel(walletRepository) } },
            )
            WalletScreen(
                viewModel = viewModel,
                onBuyClick = { navController.navigate(Routes.Trade(TransactionType.BUY)) },
                onSellClick = { navController.navigate(Routes.Trade(TransactionType.SELL)) },
            )
        }
        composable<Routes.Trade> { backStackEntry ->
            val route = backStackEntry.toRoute<Routes.Trade>()
            val viewModel: BuySellViewModel = viewModel(
                key = route.type.name,
                factory = viewModelFactory { initializer { BuySellViewModel(walletRepository, route.type) } },
            )
            BuySellScreen(
                viewModel = viewModel,
                onConfirmed = { result ->
                    navController.navigate(
                        Routes.TransactionSuccess(
                            type = result.type,
                            coinSymbol = result.coinSymbol,
                            amount = result.amountInCoin,
                        ),
                    ) {
                        popUpTo(Routes.Wallet)
                    }
                },
            )
        }
        composable<Routes.TransactionSuccess> { backStackEntry ->
            val route = backStackEntry.toRoute<Routes.TransactionSuccess>()
            TransactionSuccessScreen(
                type = route.type,
                coinSymbol = route.coinSymbol,
                amount = route.amount,
                onBackToWallet = { navController.popBackStack(Routes.Wallet, inclusive = false) },
            )
        }
    }
}
```

- [ ] **Step 2: Wire `MainActivity`**

Replace the full contents of `app/src/main/java/com/cryptowallet/MainActivity.kt` with:

```kotlin
package com.cryptowallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cryptowallet.data.local.WalletLocalDataSource
import com.cryptowallet.data.local.walletDataStore
import com.cryptowallet.data.remote.RetrofitInstance
import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.data.repository.CoinGeckoRepository
import com.cryptowallet.data.repository.WalletRepository
import com.cryptowallet.data.repository.WalletRepositoryImpl
import com.cryptowallet.navigation.CryptoWalletNavHost
import com.cryptowallet.ui.theme.CryptoWalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val walletRepository: WalletRepository = WalletRepositoryImpl(
            coinGeckoRepository = CoinGeckoRepository(CoinGeckoService(RetrofitInstance.api)),
            localDataSource = WalletLocalDataSource(applicationContext.walletDataStore),
        )

        setContent {
            CryptoWalletTheme {
                CryptoWalletNavHost(walletRepository = walletRepository)
            }
        }
    }
}
```

(This drops the template's unused `Greeting`/`GreetingPreview` composables along with the direct `TesteScreen()` call. `TesteScreen`, `TesteViewModel`, and `DashboardViewModel` remain in the codebase, just no longer reachable from the app's entry point.)

- [ ] **Step 3: Verify the full project compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

---

### Task 14: Run the full test suite and verify the app manually

**Files:** none (verification only)

- [ ] **Step 1: Run every unit test**

Run: `./gradlew :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests (existing `ExampleUnitTest` + the new ones from Tasks 3–8) pass.

- [ ] **Step 2: Install and launch the app**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`, app installs on the connected device/emulator.

- [ ] **Step 3: Walk the golden path manually**

Launch the app and verify, in order:
1. `WalletScreen` opens on launch, shows a total R$ balance and 24h % change, with "Your coins" selected by default showing BTC/ETH/BNB/USDC rows.
2. Switching to "All coins" shows the broader CoinGecko market list in R$.
3. Tapping "Buy" opens `BuySellScreen`, a coin is pre-selected, current price and "Balance before" are populated.
4. Typing an amount in the "Amount in R$" field fills "Amount in [SYMBOL]" and updates "Balance after".
5. Typing an amount in the "Amount in [SYMBOL]" field fills "Amount in R$".
6. Tapping "Confirm" navigates to `TransactionSuccessScreen` showing "Transaction Completed" and the correct quantity.
7. Tapping "Back to Wallet" returns to `WalletScreen`, and the balance/coin amounts reflect the completed purchase.
8. Repeat steps 3–7 with "Sell" and confirm the coin/cash amounts move in the opposite direction.
9. Force-close and relaunch the app — the wallet balance persists (reflects the prior buy/sell), confirming DataStore persistence works.

- [ ] **Step 4: Report results**

Note any discrepancies from the expected behavior above before considering the feature complete. Do not commit — per the git note at the top of this plan, leave everything for the user to review first.

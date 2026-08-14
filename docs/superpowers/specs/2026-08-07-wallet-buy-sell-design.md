# Wallet Buy/Sell flow — design

## Context

The app (`com.cryptowallet`, "CryptoWalletFake") currently has a read-only coin dashboard backed by CoinGecko's public market API (`CoinGeckoApi` / `CoinGeckoRepository`). There is no backend for balance or transactions — CoinGecko is a market-data API only. This feature adds a wallet flow (Wallet → Buy/Sell → Transaction success) with buy/sell simulated entirely client-side, persisted locally.

Existing conventions this design follows:
- No DI framework — ViewModels manually construct `Service`/`Repository` (see `TesteViewModel`, `DashboardViewModel`).
- No `BaseViewModel` or sealed `ApiResult` — plain `data class ...UiState` + `MutableStateFlow`, updated via `runCatching { }.onSuccess/.onFailure`.
- `navigation-compose:2.9.8` is a dependency but unused — `MainActivity` currently calls `TesteScreen()` directly.
- Prices are fetched from CoinGecko's `/coins/markets` endpoint (`vs_currency` query param); other screens use `usd`, this feature uses `brl` to match the R$ mockup, avoiding manual currency conversion.

## Decisions made during brainstorming

- **Persistence**: local only (Preferences DataStore), not backed by any server. No Room — the data shape (one cash figure + up to 4 holdings) doesn't need relational storage.
- **DI**: manual construction, matching existing ViewModels — no Hilt/Koin introduced.
- **Navigation**: `MainActivity` hosts a new `NavHost` (`CryptoWalletNavHost`) that becomes the entry point, replacing the direct `TesteScreen()` call. `TesteScreen`/`DashboardViewModel`/`TesteViewModel` remain in the codebase, unreferenced from the entry point.
- **"USD" row in the mockup** = the `usd-coin` (USDC) stablecoin, fetched and traded exactly like BTC/ETH/BNB — not a special-cased fiat balance.
- **BuySellViewModel**: one shared ViewModel parameterized by `TransactionType`, not two separate ViewModels — Buy and Sell are structurally identical (same fields, same bidirectional R$↔coin conversion math).

## 1. Models (`com.cryptowallet.model`)

```kotlin
enum class TransactionType { BUY, SELL }

data class WalletHolding(
    val coinId: String,   // CoinGecko id, e.g. "bitcoin", "usd-coin"
    val symbol: String,   // "BTC"
    val amount: Double,
)

data class WalletState(
    val cashBalanceReais: Double,
    val holdings: List<WalletHolding>,
)

// Derived at read time, not persisted directly — WalletState + live CoinListItem prices
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

`CoinListItem` (existing) is reused as-is for both the "All coins" tab and as the price source backing each `CoinBalance`.

## 2. Persistence & Repository

**`WalletLocalDataSource`**: wraps a Preferences DataStore instance, storing `WalletState` serialized to JSON under a single key (Gson, matching the converter already used for network). On first read (no stored value), returns a hardcoded seed `WalletState` (small BTC/ETH/BNB/USDC amounts + starting cash, roughly matching the mockup's ~R$20,802 total).

**`WalletRepository`** (new interface — justified because it composes two sources, unlike the existing single-source `CoinGeckoRepository`):

```kotlin
interface WalletRepository {
    suspend fun getWalletBalance(): WalletBalance
    suspend fun getCoinBalances(): List<CoinBalance>      // "Your coins"
    suspend fun getAllCoins(): List<CoinListItem>          // "All coins"
    suspend fun getCoinPrice(coinId: String): Double
    suspend fun buy(request: TransactionRequest): TransactionResult
    suspend fun sell(request: TransactionRequest): TransactionResult
}

class WalletRepositoryImpl(
    private val coinGeckoRepository: CoinGeckoRepository,
    private val localDataSource: WalletLocalDataSource,
) : WalletRepository
```

Error handling matches the existing style: suspend functions return the value or throw; callers use `runCatching`. No `Result<T>`/sealed wrapper is introduced, since none exists in the codebase today.

Buy/sell math lives in `WalletRepositoryImpl`: given the request's `amountInReais`/`amountInCoin` (already reconciled by the ViewModel) and the current price, validate against the current `WalletState` (can't sell more than owned, can't buy beyond available cash), mutate holdings, persist via `WalletLocalDataSource`, return `TransactionResult`.

## 3. ViewModels

**`WalletViewModel`**:

```kotlin
enum class WalletTab { YOUR_COINS, ALL_COINS }

data class WalletUiState(
    val isLoading: Boolean = false,
    val balance: WalletBalance? = null,
    val yourCoins: List<CoinBalance> = emptyList(),
    val allCoins: List<CoinListItem> = emptyList(),
    val selectedTab: WalletTab = WalletTab.YOUR_COINS,
    val errorMessage: String? = null,
)
```
Functions: `onTabSelected(tab)`, `refresh()` (called from `init`, and again when returning from a completed transaction).

**`BuySellViewModel`** (shared, takes `TransactionType` at construction/nav-arg time):

```kotlin
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
```
Functions: `onCoinSelected(coin)`, `onAmountInReaisChanged(text)` (derives `amountInCoin = value / currentPrice` and recomputes `balanceAfter`), `onAmountInCoinChanged(text)` (inverse derivation), `onConfirmClick()` (calls `repository.buy`/`sell` depending on `type` in `viewModelScope.launch`, sets `result` on success which the screen observes to navigate to the success route).

## 4. Compose screens & navigation

`com.cryptowallet.navigation.CryptoWalletNavHost` becomes `MainActivity`'s content, replacing the direct `TesteScreen()` call. `TesteScreen`, `TesteViewModel`, `DashboardViewModel` remain in the codebase but unreferenced from the entry point.

```kotlin
sealed class Routes {
    @Serializable data object Wallet : Routes()
    @Serializable data class Trade(val type: TransactionType) : Routes()
    @Serializable data class TransactionSuccess(
        val type: TransactionType,
        val coinSymbol: String,
        val amount: Double,
    ) : Routes()
}
```
(Navigation Compose type-safe routes, matching `navigation-compose:2.9.8` already declared.)

Screens:
- `WalletScreen` — total balance card (R$, 24h % change), Buy/Sell buttons, tab row ("Your coins"/"All coins"), `LazyColumn` of coin rows. Coin row visual reuses the pattern from `TesteScreen.kt`'s `CoinRow`, extended to show owned amount for the "Your coins" tab.
- `BuySellScreen(type: TransactionType)` — single composable for both Buy and Sell; title/accent color/button label swap based on `type`. Coin dropdown, current price, balance before, R$/coin input fields (bidirectional), balance after, Confirm button.
- `TransactionSuccessScreen` — check icon, "Transaction Completed", quantity summary, "Back to Wallet" button popping back to `Routes.Wallet` (clearing the trade screen from the back stack).

Formatting: currency via `NumberFormat.getCurrencyInstance` with a `pt-BR` locale (mirrors the `TesteScreen.kt` pattern, which currently uses `en-US`); crypto amounts formatted with a fixed decimal pattern (e.g. `%.6f`) to display values like `0.000001 ETH`.

## New dependency

`androidx.datastore:datastore-preferences` — not yet present in `libs.versions.toml` / `app/build.gradle.kts`.

## Out of scope

- No transaction history screen/list (mockup only shows the immediate success confirmation).
- No real backend integration — this is entirely local simulation, matching the "Fake" nature of the app.
- No changes to `TesteScreen`/`DashboardViewModel`/`TesteViewModel` beyond no longer being the entry point.

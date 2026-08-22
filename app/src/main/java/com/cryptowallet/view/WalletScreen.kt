package com.cryptowallet.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinDetails
import com.cryptowallet.model.WalletBalance
import com.cryptowallet.ui.theme.AuthFieldBorder
import com.cryptowallet.ui.theme.AuthLabelMuted
import com.cryptowallet.ui.theme.CryptoOrange
import com.cryptowallet.ui.theme.WalletNegative
import com.cryptowallet.ui.theme.WalletPositive
import com.cryptowallet.ui.theme.WalletTextPrimary
import com.cryptowallet.view.component.AppBackground2
import com.cryptowallet.view.component.AppButton
import com.cryptowallet.view.component.AppCard
import com.cryptowallet.view.component.AppPageTitle
import com.cryptowallet.view.component.LocalScaffoldPadding
import com.cryptowallet.view.component.SegmentedTabs
import com.cryptowallet.viewmodel.WalletTab
import com.cryptowallet.viewmodel.WalletUiState
import com.cryptowallet.viewmodel.WalletViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private val ReaisFormatter: NumberFormat =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    onCoinClick: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WalletContent(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onBuyClick = onBuyClick,
        onSellClick = onSellClick,
        onCoinClick = onCoinClick,
        onRetryClick = viewModel::refresh,
    )
}

@Composable
private fun WalletContent(
    uiState: WalletUiState,
    onTabSelected: (WalletTab) -> Unit,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    onCoinClick: (String) -> Unit,
    onRetryClick: () -> Unit,
) {
    val scaffoldPadding = LocalScaffoldPadding.current
    val listBottomPadding = scaffoldPadding.calculateBottomPadding() + 12.dp

    AppBackground2(
        applyBottomScaffoldPadding = false,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            AppPageTitle("Wallet")
            Spacer(modifier = Modifier.height(30.dp))

            if (uiState.isLoading && uiState.balance == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CryptoOrange)
                }
                return@Column
            }

            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tentar novamente",
                    color = AuthLabelMuted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onRetryClick),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            AppCard {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    uiState.balance?.let { balance ->
                        Text(
                            text = ReaisFormatter.format(balance.totalReais),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = WalletTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val isPositive = balance.changePercentage24h >= 0
                        val changeColor = if (isPositive) WalletPositive else WalletNegative
                        val amountSign = if (balance.changeAmount24h >= 0) "+" else "-"
                        Text(
                            text = "$amountSign${ReaisFormatter.format(abs(balance.changeAmount24h))} " +
                                "(${String.format(Locale.US, "%+.2f%%", balance.changePercentage24h)})",
                            color = changeColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Last 24 hours",
                            color = AuthLabelMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton(text = "Buy", onClick = onBuyClick, modifier = Modifier.weight(1f))
                AppButton(text = "Sell", onClick = onSellClick, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            SegmentedTabs(
                items = listOf(WalletTab.YOUR_COINS to "Your coins", WalletTab.ALL_COINS to "All coins"),
                selected = uiState.selectedTab,
                onSelected = onTabSelected,
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (uiState.selectedTab) {
                WalletTab.YOUR_COINS -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = listBottomPadding),
                ) {
                    items(items = uiState.yourCoins, key = { it.coin.id }) { balance ->
                        YourCoinListRow(balance, onClick = { onCoinClick(balance.coin.id) })
                    }
                }
                WalletTab.ALL_COINS -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = listBottomPadding),
                ) {
                    items(items = uiState.allCoins, key = { it.id }) { coin ->
                        AllCoinCard(coin, onClick = { onCoinClick(coin.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinCard(
    imageUrl: String,
    imageDescription: String,
    symbol: String,
    changePercentage24h: Double,
    primaryValue: String,
    secondaryValue: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AuthFieldBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = imageDescription,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = symbol.uppercase(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = String.format(Locale.US, "%+.0f%%", changePercentage24h),
                    color = if (changePercentage24h >= 0) WalletPositive else WalletNegative,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = primaryValue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                text = secondaryValue,
                style = MaterialTheme.typography.bodySmall,
                color = AuthLabelMuted,
            )
        }
    }
}

@Composable
private fun YourCoinListRow(balance: CoinBalance, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AuthFieldBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = balance.coin.imageUrl,
                    contentDescription = balance.coin.name,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = balance.coin.symbol.uppercase(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                // Valor investido: o app não guarda preço médio de compra/custo, então usamos o
                // valor atual da posição (amountOwned * currentPrice) como melhor aproximação real.
                Text(text = ReaisFormatter.format(balance.valueInReais), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ReaisFormatter.format(balance.coin.currentPrice),
                        style = MaterialTheme.typography.bodySmall,
                        color = AuthLabelMuted,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, "%+.0f%%", balance.coin.priceChangePercentage24h),
                        color = if (balance.coin.priceChangePercentage24h >= 0) WalletPositive else WalletNegative,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "${String.format(Locale.US, "%.6f", balance.amountOwned)} ${balance.coin.symbol.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuthLabelMuted,
                )
            }
        }
    }
}

@Composable
private fun AllCoinCard(coin: CoinDetails, onClick: () -> Unit) {
    CoinCard(
        imageUrl = coin.imageUrl,
        imageDescription = coin.name,
        symbol = coin.symbol,
        changePercentage24h = coin.priceChangePercentage24h,
        primaryValue = ReaisFormatter.format(coin.currentPrice),
        secondaryValue = coin.name,
        onClick = onClick,
    )
}


@Preview(showBackground = true)
@Composable
fun WalletContentPreview() {
    WalletContent(
        uiState = WalletUiState(
            isLoading = false,
            balance = WalletBalance(
                totalReais = 2000.0,
                changeAmount24h = 1.0,
                changePercentage24h = 0.0
            ),
            selectedTab = WalletTab.ALL_COINS,
//            selectedTab = WalletTab.YOUR_COINS,
            allCoins = listOf(
                CoinDetails(
                    id = "btc",
                    name = "Bitcoin",
                    symbol = "BTC",
                    currentPrice = 500.0,
                    priceChange24h = 115.0,
                    priceChangePercentage24h = 1.0,
                    imageUrl = "",
                    marketCapRank = null
                ),
                CoinDetails(
                    id = "etc",
                    name = "Eth",
                    symbol = "ETH",
                    currentPrice = 100.0,
                    priceChange24h = 15.0,
                    priceChangePercentage24h = 1.5,
                    imageUrl = "",
                    marketCapRank = null
                ),
                CoinDetails(
                    id = "oth",
                    name = "Oth",
                    symbol = "OTH",
                    currentPrice = 5.0,
                    priceChange24h = 0.0,
                    priceChangePercentage24h = 0.0,
                    imageUrl = "",
                    marketCapRank = null
                )
            ),
            yourCoins = listOf(
                CoinBalance(
                    coin = CoinDetails(
                        id = "btc",
                        name = "Bitcoin",
                        symbol = "BTC",
                        currentPrice = 500.0,
                        priceChange24h = 115.0,
                        priceChangePercentage24h = 1.0,
                        imageUrl = "",
                        marketCapRank = null
                    ),
                    amountOwned = 1.0,
                    valueInReais = 1000.0,
                )
            ),
        ),
        onTabSelected = {},
        onBuyClick = {},
        onSellClick = {},
        onCoinClick = {},
        onRetryClick = {},
    )
}
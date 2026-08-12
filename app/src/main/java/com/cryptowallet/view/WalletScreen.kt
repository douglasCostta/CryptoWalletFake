package com.cryptowallet.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cryptowallet.model.CoinBalance
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.ui.theme.WalletCardBorder
import com.cryptowallet.ui.theme.WalletNegative
import com.cryptowallet.ui.theme.WalletPositive
import com.cryptowallet.ui.theme.WalletTextPrimary
import com.cryptowallet.ui.theme.WalletTextSecondary
import com.cryptowallet.view.components.GradientButton
import com.cryptowallet.view.components.SegmentedTabs
import com.cryptowallet.view.components.walletGlowBrush
import com.cryptowallet.viewmodel.WalletTab
import com.cryptowallet.viewmodel.WalletUiState
import com.cryptowallet.viewmodel.WalletViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

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
        onRetryClick = viewModel::refresh,
    )
}

@Composable
private fun WalletContent(
    uiState: WalletUiState,
    onTabSelected: (WalletTab) -> Unit,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(walletGlowBrush()),
    ) {
        if (uiState.isLoading && uiState.balance == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 34.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = WalletTextPrimary,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Text(
                    text = "Wallet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WalletTextPrimary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tentar novamente",
                    color = WalletTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onRetryClick),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WalletCardBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    uiState.balance?.let { balance ->
                        Text(
                            text = ReaisFormatter.format(balance.totalReais),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
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
                            color = WalletTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientButton(text = "Buy", onClick = onBuyClick, modifier = Modifier.weight(1f))
                GradientButton(text = "Sell", onClick = onSellClick, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            SegmentedTabs(
                items = listOf(WalletTab.YOUR_COINS to "Your coins", WalletTab.ALL_COINS to "All coins"),
                selected = uiState.selectedTab,
                onSelected = onTabSelected,
            )

            Spacer(modifier = Modifier.height(12.dp))

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
}

@Composable
private fun YourCoinRow(balance: CoinBalance) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WalletCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = balance.coin.imageUrl,
                contentDescription = balance.coin.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = balance.coin.symbol.uppercase(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(text = ReaisFormatter.format(balance.valueInReais), fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${String.format(Locale.US, "%.6f", balance.amountOwned)} ${balance.coin.symbol.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WalletTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun AllCoinRow(coin: CoinListItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WalletCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = coin.imageUrl,
                contentDescription = coin.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = coin.name, fontWeight = FontWeight.Bold)
                Text(
                    text = coin.symbol.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = WalletTextSecondary,
                )
            }
            Text(text = ReaisFormatter.format(coin.currentPrice), fontWeight = FontWeight.SemiBold)
        }
    }
}

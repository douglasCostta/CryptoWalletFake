package com.cryptowallet.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.TransactionType
import com.cryptowallet.ui.theme.WalletCardBorder
import com.cryptowallet.ui.theme.WalletTextSecondary
import com.cryptowallet.view.components.GradientButton
import com.cryptowallet.view.components.walletGlowBrush
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
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = WalletCardBorder,
        unfocusedBorderColor = WalletCardBorder,
        focusedLabelColor = WalletTextSecondary,
        unfocusedLabelColor = WalletTextSecondary,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(walletGlowBrush()),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu", modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "Coin", style = MaterialTheme.typography.bodySmall, color = WalletTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WalletCardBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text(text = uiState.selectedCoin?.name ?: "Selecione uma moeda", modifier = Modifier.align(Alignment.CenterStart))
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = WalletTextSecondary,
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
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

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Current Price", style = MaterialTheme.typography.bodySmall, color = WalletTextSecondary)
            Text(text = ReaisFormatter.format(uiState.currentPrice), fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(16.dp))
            uiState.balanceBefore?.let { before ->
                val ownedBefore = before.holdings.find { it.coinId == uiState.selectedCoin?.id }?.amount ?: 0.0
                BalanceInfoCard(
                    label = "Balance before",
                    cash = ReaisFormatter.format(before.cashBalanceReais),
                    symbol = symbol,
                    amount = String.format(Locale.US, "%.6f", ownedBefore),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.amountInReais,
                onValueChange = onAmountInReaisChanged,
                label = { Text("Amount in R$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.amountInCoin,
                onValueChange = onAmountInCoinChanged,
                label = { Text("Amount in $symbol") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            uiState.balanceAfter?.let { after ->
                val ownedAfter = after.holdings.find { it.coinId == uiState.selectedCoin?.id }?.amount ?: 0.0
                BalanceInfoCard(
                    label = "Balance after",
                    cash = ReaisFormatter.format(after.cashBalanceReais),
                    symbol = symbol,
                    amount = String.format(Locale.US, "%.6f", ownedAfter),
                )
            }

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))
            GradientButton(
                text = "Confirm",
                onClick = onConfirmClick,
                enabled = !uiState.isLoading && uiState.selectedCoin != null && uiState.amountInReais.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BalanceInfoCard(label: String, cash: String, symbol: String, amount: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WalletCardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = WalletTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "$cash   $symbol $amount", fontWeight = FontWeight.SemiBold)
        }
    }
}

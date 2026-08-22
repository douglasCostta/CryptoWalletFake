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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptowallet.model.CoinDetails
import com.cryptowallet.model.TransactionResult
import com.cryptowallet.model.TransactionType
import com.cryptowallet.ui.theme.AuthFieldBorder
import com.cryptowallet.ui.theme.AuthLabelMuted
import com.cryptowallet.ui.theme.CryptoDarkOrange
import com.cryptowallet.ui.theme.WalletTextPrimary
import com.cryptowallet.view.component.AppBackground1
import com.cryptowallet.view.component.AppBackground2
import com.cryptowallet.view.component.AppButton
import com.cryptowallet.view.component.AppCard
import com.cryptowallet.view.component.AppPageTitle
import com.cryptowallet.viewmodel.BuySellViewModel
import com.cryptowallet.viewmodel.TradeUiState
import java.text.NumberFormat
import java.util.Locale

private val ReaisFormatter: NumberFormat =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

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
    onCoinSelected: (CoinDetails) -> Unit,
    onAmountInReaisChanged: (String) -> Unit,
    onAmountInCoinChanged: (String) -> Unit,
    onConfirmClick: () -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val title = if (uiState.type == TransactionType.BUY) "Buy" else "Sell"
    val symbol = uiState.selectedCoin?.symbol?.uppercase().orEmpty()

    AppBackground1 {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            AppPageTitle(title)

            Spacer(modifier = Modifier.height(30.dp))

            Text(text = "Coin", style = MaterialTheme.typography.bodyMedium, color = AuthLabelMuted)
            Spacer(modifier = Modifier.height(4.dp))
            var dropdownWidthPx by remember { mutableStateOf(0) }
            val density = LocalDensity.current
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { dropdownWidthPx = it.size.width }
                        .clip(RoundedCornerShape(14.dp))
                        .background(CryptoDarkOrange.copy(alpha = 0.15f))
                        .border(1.dp, AuthFieldBorder, RoundedCornerShape(14.dp))
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = uiState.selectedCoin?.name ?: "Selecione uma moeda",
                        color = WalletTextPrimary,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AuthLabelMuted,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.width(with(density) { dropdownWidthPx.toDp() }),
                ) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "Current Price",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuthLabelMuted
                )
                Text(
                    text = ReaisFormatter.format(uiState.currentPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuthLabelMuted,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            AppCard {
                AppBackground2(
                    modifier = Modifier.fillMaxWidth(),
                    fillParent = false,
                    applyScreenPadding = false,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                    ) {
                        uiState.balanceBefore?.let { before ->
                            val ownedBefore =
                                before.holdings.find { it.coinId == uiState.selectedCoin?.id }?.amount
                                    ?: 0.0
                            BalanceSummaryRow(
                                label = "Balance before",
                                cash = ReaisFormatter.format(before.cashBalanceReais),
                                symbol = symbol,
                                amount = String.format(Locale.US, "%.6f", ownedBefore),
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        AmountRow(
                            label = "Amount in R$",
                            value = uiState.amountInReais,
                            onValueChange = onAmountInReaisChanged,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AmountRow(
                            label = "Amount in $symbol",
                            value = uiState.amountInCoin,
                            onValueChange = onAmountInCoinChanged,
                        )

                        uiState.balanceAfter?.let { after ->
                            val ownedAfter =
                                after.holdings.find { it.coinId == uiState.selectedCoin?.id }?.amount
                                    ?: 0.0
                            Spacer(modifier = Modifier.height(20.dp))
                            BalanceSummaryRow(
                                label = "Balance after",
                                cash = ReaisFormatter.format(after.cashBalanceReais),
                                symbol = symbol,
                                amount = String.format(Locale.US, "%.6f", ownedAfter),
                            )
                        }
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))
            AppButton(
                text = "Confirm",
                onClick = onConfirmClick,
                enabled = !uiState.isLoading && uiState.selectedCoin != null && uiState.amountInReais.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun BalanceSummaryRow(label: String, cash: String, symbol: String, amount: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = AuthLabelMuted)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        ) {
            Text(text = cash, color = AuthLabelMuted, fontWeight = FontWeight.SemiBold)
            Text(
                text = "$symbol $amount",
                color = AuthLabelMuted,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = WalletTextPrimary, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.End,
                color = WalletTextPrimary,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CryptoDarkOrange.copy(alpha = 0.15f),
                unfocusedContainerColor = CryptoDarkOrange.copy(alpha = 0.15f),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = WalletTextPrimary,
            ),
            modifier = Modifier.width(130.dp).height(60.dp),
        )
    }
}

package com.cryptowallet.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cryptowallet.util.formatCompactUsd
import com.cryptowallet.util.formatPercentage
import com.cryptowallet.util.formatPriceChange
import com.cryptowallet.util.formatValueUsd
import com.cryptowallet.view.component.AppBackground1
import com.cryptowallet.view.component.AppButton
import com.cryptowallet.viewmodel.DashboardViewModel
import com.cryptowallet.view.component.PriceChart
import com.cryptowallet.view.component.RangeSelectorRow
import java.util.Locale

@Composable
fun DashboardScreen(
    onBack: () -> Unit = {},
    onBuyClick: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val chartPoints = uiState.graphPoints

    AppBackground1 {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isLoadingCoins) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF7043))
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        AsyncImage(
                            model = uiState.imageUrl,
                            contentDescription = uiState.coinName,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.coinName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.W500,
                            fontFamily = FontFamily.Default,
                            color = Color.White,
                        )
                    }

                    Spacer(modifier = Modifier.size(15.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = formatValueUsd(uiState.currentPrice),
                            fontSize = 27.sp,
                            fontWeight = FontWeight.W500,
                            fontFamily = FontFamily.Default,
                            color = Color.White,
                        )
                        Text(
                            text = "${formatPriceChange(uiState.priceChange24h)} (${formatPercentage(uiState.priceChangePercentage24h)})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W400,
                            fontFamily = FontFamily.Default,
                            color = if (uiState.priceChangePercentage24h < 0) Color(0xFFFF6B6B) else Color(0xFF35C759),
                        )
                    }

                    Spacer(modifier = Modifier.size(15.dp))

                    RangeSelectorRow (
                        selectedRange = uiState.selectedRange,
                        onRangeSelected = { range -> viewModel.selectRange(range) },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (chartPoints.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (uiState.errorMessage != null) {
                                    Text(
                                        text = "No data available for the selected range.",
                                        color = Color(0xFF9A9A9A),
                                        textAlign = TextAlign.Center,
                                    )
                                } else {
                                    CircularProgressIndicator(color = Color(0xFFFF7043))
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                            ) {
                                key(uiState.selectedRange) {
                                    PriceChart(
                                        chartPoints = chartPoints,
                                        range = uiState.selectedRange,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "MARKET DATA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500,
                        color = Color(0xFF9A9A9A),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF3A3A3A))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        MarketDataItem(
                            title = "MARKET CAP",
                            value = formatCompactUsd(uiState.marketCap ?: 0.0),
                        )
                        MarketDataItem(
                            title = "24H VOLUME",
                            value = formatCompactUsd(uiState.totalVolume24h ?: 0.0),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        AsyncImage(
                            model = uiState.imageUrl,
                            contentDescription = uiState.coinName,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.symbol} Wallet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W500,
                            fontFamily = FontFamily.Default,
                            color = Color.White,
                        )

                        Text(
                            text = "${String.format(Locale.US, "%.6f", uiState.amountOwned)} ${uiState.symbol}",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W500,
                            fontFamily = FontFamily.Default,
                            color = Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppButton(
                    text = "Buy ${uiState.symbol}",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBuyClick,
                )

            }
        }
    }

}

@Composable
private fun MarketDataItem(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color(0xFF9A9A9A),
            fontWeight = FontWeight.W400,
        )
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.W500,
            color = Color.White,
        )
    }
}
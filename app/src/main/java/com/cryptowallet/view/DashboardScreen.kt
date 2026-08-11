package com.cryptowallet.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cryptowallet.data.remote.enums.ChartRangeEnum
import com.cryptowallet.model.CoinGraphPoints
import com.cryptowallet.viewmodel.DashboardViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.layer.continuous
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider.Companion.verticalGradient
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val chartPoints = uiState.graphPoints

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .safeDrawingPadding()
            .padding(16.dp),
    ) {
        if (uiState.isLoadingCoins) {
            CircularProgressIndicator()
        } else {
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

            RangeSelectorRow(
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
                        Text(
                            text = "No data available for the selected range.",
                            color = Color(0xFF9A9A9A),
                            textAlign = TextAlign.Center,
                        )
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

@Composable
fun PriceChart(
    chartPoints: List<CoinGraphPoints>,
    range: ChartRangeEnum,
    modifier: Modifier = Modifier,
) {
    if (chartPoints.isEmpty()) return

    val visiblePoints = remember(chartPoints, range) {
        chartPoints
    }

    val minPrice = visiblePoints.minOf { it.close }
    val maxPrice = visiblePoints.maxOf { it.close }
    val priceRange = maxPrice - minPrice

    val padding = when {
                    priceRange > 0.0 ->
                        priceRange * 0.10
                    maxPrice > 0.0 ->
                        maxPrice * 0.01
                    else ->
                        1.0
                }

    val minY = minPrice - padding
    val maxY = maxPrice + padding
    val modelProducer = remember { CartesianChartModelProducer() }

    val lineLayer =
        rememberLineCartesianLayer(
            rangeProvider =
                remember(minY,maxY) {
                    CartesianLayerRangeProvider.fixed(minY = minY, maxY = maxY)
                },
            lineProvider =
                LineCartesianLayer.LineProvider.series(
                    listOf(
                        LineCartesianLayer.rememberLine(
                            fill =
                                LineCartesianLayer.LineFill.single(
                                    fill(
                                        verticalGradient(
                                            Color(0xFFFF7043).toArgb(),
                                            Color(0xFFFF7043).toArgb(),
                                        )
                                    )
                                ),
                            stroke =
                                LineCartesianLayer.LineStroke.continuous(
                                    thickness = 1.dp,
                                    cap = StrokeCap.Round,
                                ),
                            areaFill =
                                LineCartesianLayer.AreaFill.single(
                                    fill(
                                        verticalGradient(
                                            Color(0x66A24F37).toArgb(),
                                            Color(0x334B2A23).toArgb(),
                                            Color(0x001C1C1E).toArgb(),
                                        )
                                    )
                                ),
                        )
                    )
                )
        )

    LaunchedEffect(range, visiblePoints) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = visiblePoints.indices.map { it.toDouble() },
                    y = visiblePoints.map { it.close },
                )
            }
        }
    }

    CartesianChartHost(
        chart =
            rememberCartesianChart(
                lineLayer,
                startAxis = null,
                topAxis = null,
                endAxis =
                    VerticalAxis.rememberEnd(
                        line = null,
                        label = rememberTextComponent(color = Color(0xFF8E8E93),textSize = 10.sp),
                        tick = null,
                        guideline = null,
                        valueFormatter = { _, value, _ -> formatPrice(value) },
                        itemPlacer = remember { VerticalAxis.ItemPlacer.count(count = { 5 }) },
                    ),
                bottomAxis = null,
            ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E))
    )
}

@Composable
private fun RangeSelectorRow(
    selectedRange: ChartRangeEnum,
    onRangeSelected: (ChartRangeEnum) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChartRangeEnum.entries.forEach { range ->
            val isSelected = range == selectedRange
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRangeSelected(range) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = range.label,
                    color = if (isSelected) Color.White else Color(0xFF8A8A8A),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(36.dp)
                        .background(if (isSelected) Color.White else Color.Transparent),
                )
            }
        }
    }
}

private fun formatPrice(value: Double): String {
    return NumberFormat
        .getNumberInstance(Locale.US)
        .apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        .format(value)
}

fun formatPriceChange(priceChange: Double): String {
    val formattedValue = NumberFormat.getCurrencyInstance(Locale.US).format(abs(priceChange))
    return if (priceChange >= 0) "+$formattedValue" else "-$formattedValue"
}

fun formatValueUsd(price: Double): String = NumberFormat.getCurrencyInstance(Locale.US).format(price)

fun formatPercentage(value: Double): String = String.format(Locale.US, "%.2f%%", value)

fun formatCompactUsd(price: Double): String {
    val absValue = abs(price)
    val suffix = when {
        absValue >= 1_000_000_000 -> "B"
        absValue >= 1_000_000 -> "M"
        absValue >= 1_000 -> "K"
        else -> null
    }

    if (suffix == null) return formatValueUsd(price)

    val divisor = when (suffix) {
        "B" -> 1_000_000_000.0
        "M" -> 1_000_000.0
        else -> 1_000.0
    }

    val compact = price / divisor
    return "$${String.format(Locale.US, "%.2f", compact)}$suffix"
}




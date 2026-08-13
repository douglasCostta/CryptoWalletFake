package com.cryptowallet.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.data.remote.enums.ChartRangeEnum
import com.cryptowallet.model.CoinGraphPoints
import com.cryptowallet.util.formatPrice
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
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider.Companion.verticalGradient

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
    )
}
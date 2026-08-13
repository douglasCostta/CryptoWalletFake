package com.cryptowallet.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

fun formatPrice(value: Double): String {
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
        absValue >= 1_000_000_000_000 -> "T"
        absValue >= 1_000_000_000 -> "B"
        absValue >= 1_000_000 -> "M"
        absValue >= 1_000 -> "K"
        else -> null
    }

    if (suffix == null) return formatValueUsd(price)

    val divisor = when (suffix) {
        "T" -> 1_000_000_000_000.0
        "B" -> 1_000_000_000.0
        "M" -> 1_000_000.0
        else -> 1_000.0
    }

    val compact = price / divisor
    return "$${String.format(Locale.US, "%.2f", compact)}$suffix"
}

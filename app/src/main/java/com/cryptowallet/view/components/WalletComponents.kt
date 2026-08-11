package com.cryptowallet.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cryptowallet.ui.theme.WalletBackground
import com.cryptowallet.ui.theme.WalletCardBorder
import com.cryptowallet.ui.theme.WalletGlow
import com.cryptowallet.ui.theme.WalletOrangeEnd
import com.cryptowallet.ui.theme.WalletOrangeStart
import com.cryptowallet.ui.theme.WalletTextPrimary
import com.cryptowallet.ui.theme.WalletTextSecondary

private val OrangeGradient = Brush.horizontalGradient(listOf(WalletOrangeStart, WalletOrangeEnd))

/** Warm radial glow over the dark background, matching the mockup's top-right spotlight. */
fun walletGlowBrush(): Brush = object : ShaderBrush() {
    override fun createShader(size: androidx.compose.ui.geometry.Size): Shader {
        return RadialGradientShader(
            center = Offset(size.width * 0.85f, size.height * 0.05f),
            radius = size.width.coerceAtLeast(1f) * 1.1f,
            colors = listOf(WalletGlow, WalletBackground),
        )
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (enabled) OrangeGradient else Brush.horizontalGradient(listOf(WalletCardBorder, WalletCardBorder)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = WalletTextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun <T> SegmentedTabs(
    items: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
    ) {
        items.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) OrangeGradient else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onSelected(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSelected) WalletTextPrimary else WalletTextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

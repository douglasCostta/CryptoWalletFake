package com.cryptowallet.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cryptowallet.ui.theme.AuthLabelMuted
import com.cryptowallet.ui.theme.CryptoDarkOrange
import com.cryptowallet.ui.theme.CryptoOrange
import com.cryptowallet.ui.theme.WalletTextPrimary

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
            .background(CryptoDarkOrange.copy(0.2f))
    ) {
        items.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) CryptoOrange else Color.Transparent)
                    .clickable { onSelected(value) }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSelected) WalletTextPrimary else AuthLabelMuted,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

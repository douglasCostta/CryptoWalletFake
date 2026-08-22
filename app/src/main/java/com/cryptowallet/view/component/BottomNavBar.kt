package com.cryptowallet.view.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.ui.theme.CryptoDarkOrange
import com.cryptowallet.ui.theme.CryptoOrange

enum class BottomNavItem(val label: String, val icon: ImageVector) {
    WALLET("Wallet", Icons.Filled.AccountBalanceWallet),
    BUY("Buy", Icons.Filled.FileDownload),
    SELL("Sell", Icons.Filled.FileUpload),
    LOGOUT("Logout", Icons.AutoMirrored.Filled.Logout),
}

@Composable
fun BottomNavBar(
    selectedItem: BottomNavItem?,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(CryptoOrange, CryptoDarkOrange),
                ),
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomNavItem.entries.forEach { item ->
            BottomNavBarItem(
                item = item,
                selected = item == selectedItem,
                onClick = { onItemSelected(item) },
            )
        }
    }
}

@Composable
private fun RowScope.BottomNavBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
        label = "bottomNavIndicator",
    )
    val iconTextColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.78f),
        label = "bottomNavIconTextColor",
    )
    val itemScale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        label = "bottomNavItemScale",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .scale(itemScale)
            .clip(RoundedCornerShape(14.dp))
            .background(color = indicatorColor)
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconTextColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = item.label,
            color = iconTextColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

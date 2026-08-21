package com.cryptowallet.view.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(CryptoDarkOrange, CryptoOrange),
                ),
            )
            .navigationBarsPadding()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
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
        targetValue = if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
        label = "bottomNavIndicator",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .selectable(selected = selected, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color = indicatorColor, shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            )
        }
        Text(
            text = item.label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

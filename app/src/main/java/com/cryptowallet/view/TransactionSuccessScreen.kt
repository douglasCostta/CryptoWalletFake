package com.cryptowallet.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cryptowallet.model.TransactionType
import com.cryptowallet.ui.theme.WalletPositive
import com.cryptowallet.ui.theme.WalletTextSecondary
import com.cryptowallet.view.components.GradientButton
import com.cryptowallet.view.components.walletGlowBrush
import java.util.Locale

@Composable
fun TransactionSuccessScreen(
    type: TransactionType,
    coinSymbol: String,
    amount: Double,
    onBackToWallet: () -> Unit,
) {
    val verb = if (type == TransactionType.BUY) "bought" else "sold"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(walletGlowBrush())
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = WalletPositive,
                modifier = Modifier.size(96.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Transaction Completed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You successfully $verb ${String.format(Locale.US, "%.6f", amount)} $coinSymbol",
                color = WalletTextSecondary,
            )
        }

        GradientButton(
            text = "Back to Wallet",
            onClick = onBackToWallet,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        )
    }
}

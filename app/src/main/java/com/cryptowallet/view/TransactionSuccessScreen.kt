package com.cryptowallet.view

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cryptowallet.model.TransactionType
import com.cryptowallet.ui.theme.AuthLabelMuted
import com.cryptowallet.ui.theme.CryptoWalletTheme
import com.cryptowallet.ui.theme.WalletPositive
import com.cryptowallet.ui.theme.WalletTextPrimary
import com.cryptowallet.view.component.AppBackground1
import com.cryptowallet.view.component.AppBackground2
import com.cryptowallet.view.component.AppButton
import com.cryptowallet.view.component.AppCard
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun TransactionSuccessScreen(
    type: TransactionType,
    coinSymbol: String,
    amount: Double,
    onBackToWallet: () -> Unit,
) {
    val verb = if (type == TransactionType.BUY) "bought" else "sold"
    val confettiParties = remember {
        listOf(
            Party(
                speed = 10f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xF2841B, 0xF7C948, 0x35C759, 0xFDFBF9),
                position = Position.Relative(0.5, 0.3),
                emitter = Emitter(duration = 150, TimeUnit.MILLISECONDS).max(150),
            ),
        )
    }

    AppBackground1 {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AppCard() {
                    AppBackground2(
                        modifier = Modifier.fillMaxWidth(),
                        fillParent = false,
                        applyScreenPadding = false,
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth()
                                .height(300.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = WalletPositive,
                                modifier = Modifier.size(96.dp),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Transaction Completed",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = WalletTextPrimary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You successfully $verb ${
                                    String.format(
                                        Locale.US,
                                        "%.6f",
                                        amount
                                    )
                                } $coinSymbol",
                                color = AuthLabelMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.height(100.dp))
            }

            AppButton(
                text = "Back to Wallet",
                onClick = onBackToWallet,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            )

            KonfettiView(modifier = Modifier.fillMaxSize(), parties = confettiParties)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    CryptoWalletTheme {
        TransactionSuccessScreen(
            type = TransactionType.BUY,
            coinSymbol = "ETC",
            amount = 0.01,
            onBackToWallet = {  },
        )
    }
}
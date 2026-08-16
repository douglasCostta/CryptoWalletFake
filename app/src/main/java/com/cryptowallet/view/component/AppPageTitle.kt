package com.cryptowallet.view.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.cryptowallet.ui.theme.WalletTextPrimary

@Composable
fun AppPageTitle(text: String) {
	Text(
		text = text,
		style = MaterialTheme.typography.titleLarge,
		fontWeight = FontWeight.Bold,
		color = WalletTextPrimary,
		textAlign = TextAlign.Center,
		modifier = Modifier.fillMaxWidth()
	)
}
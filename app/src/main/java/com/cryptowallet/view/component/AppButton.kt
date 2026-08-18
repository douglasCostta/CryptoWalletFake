package com.cryptowallet.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.ui.theme.AuthFieldBorder
import com.cryptowallet.ui.theme.CryptoDarkOrange
import com.cryptowallet.ui.theme.CryptoOrange
import com.cryptowallet.ui.theme.WalletTextPrimary

private val OrangeGradient = Brush.horizontalGradient(listOf(CryptoOrange, CryptoDarkOrange))

@Composable
fun AppButton(
	text: String,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	isLoading: Boolean = false,
	onClick: () -> Unit
) {
	Button(
		onClick = onClick,
		contentPadding = PaddingValues(),
		shape = RoundedCornerShape(12.dp),
		modifier = modifier.alpha( if(enabled) 1f else 0.5f),
		enabled = enabled
	) {
		Box(
			modifier = Modifier
				.background(
					brush = OrangeGradient,
				)
				.padding(12.dp)
				.height(30.dp)
				.fillMaxWidth(),
			contentAlignment = Alignment.Center
		) {
			if (isLoading) {
				CircularProgressIndicator(
					modifier = Modifier.size(20.dp),
					color = Color.White,
					strokeWidth = 2.dp,
				)
			} else {
				Text(text, fontSize = 18.sp, color = WalletTextPrimary, fontWeight = FontWeight.Bold)
			}
		}
	}
}
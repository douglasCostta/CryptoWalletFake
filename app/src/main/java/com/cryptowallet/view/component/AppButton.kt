package com.cryptowallet.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppButton(text: String, modifier: Modifier = Modifier, action: () -> Unit) {
	Button(
		onClick = action,
		contentPadding = PaddingValues(),
		shape = RoundedCornerShape(12.dp),
		modifier = modifier
	) {
		Box(
			modifier = Modifier
				.background(
					brush = Brush.horizontalGradient(
						colors = listOf(
							Color(0xFFFFB342),
							Color(0xFFD36014)
						)
					),
				)
				.padding(12.dp)
				.fillMaxWidth(),
			contentAlignment = Alignment.Center
		) {
			Text(text)
		}
	}
}
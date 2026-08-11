package com.cryptowallet.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppBackground1(content: @Composable BoxScope.() -> Unit) {
	Box(
		modifier = Modifier
			.background(
				brush = Brush.verticalGradient(
					colorStops = arrayOf(
						0.75f to Color(0xFF212121),
						1f to Color(0xFF8A481E)
					),
				),
			)
			.padding(12.dp)
			.fillMaxSize(),
	) { content() }
}
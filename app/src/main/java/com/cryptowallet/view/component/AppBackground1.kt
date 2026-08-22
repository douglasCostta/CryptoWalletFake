package com.cryptowallet.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalScaffoldPadding = compositionLocalOf { PaddingValues(0.dp) }

val AppGradientBrush1: Brush = Brush.verticalGradient(
	colorStops = arrayOf(
		0.75f to Color(0xFF212121),
		1f to Color(0xFF8A481E)
	),
)

/** A darker, muted take on [AppGradientBrush1]'s brand colors, spread across the whole height -
 * for smaller surfaces (cards) where the vivid 0.75f/1f stops above would be too bright. */
val CardGradientBrush1: Brush = Brush.verticalGradient(
	colors = listOf(Color(0xFF212121), Color(0xFF4A2A16)),
)

@Composable
fun AppBackground1(content: @Composable BoxScope.() -> Unit) {
	val scaffoldPadding = LocalScaffoldPadding.current
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(brush = AppGradientBrush1),
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(scaffoldPadding)
				.safeDrawingPadding()
				.padding(20.dp),
		) {
			content()
		}
	}
}

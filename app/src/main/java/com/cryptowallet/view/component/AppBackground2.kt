package com.cryptowallet.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppBackground2(
    modifier: Modifier = Modifier,
    fillParent: Boolean = true,
    applyScreenPadding: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val scaffoldPadding = LocalScaffoldPadding.current
    val containerModifier = if (fillParent) modifier.fillMaxSize() else modifier
    val contentModifier = if (applyScreenPadding) {
        Modifier
            .padding(scaffoldPadding)
            .safeDrawingPadding()
    } else {
        Modifier
    }

    Box(
        modifier = containerModifier
            .background(Color(0xFF1E1E1E))
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8A481E).copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(size.width, -size.height * 0.1f),
                        radius = size.width * 0.8f
                    )
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8A481E).copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(0f, size.height * 0.8f),
                        radius = size.width * 0.8f
                    )
                )
            }
    ) {
        Box(
            modifier = contentModifier
                .padding(20.dp),
        ) {
            content()
        }
    }
}

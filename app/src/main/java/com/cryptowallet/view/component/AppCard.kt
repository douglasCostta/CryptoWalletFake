package com.cryptowallet.view.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cryptowallet.ui.theme.AuthFieldBorder

@Composable
fun AppCard(onClick: () -> Unit = {},content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .border(1.dp, AuthFieldBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .fillMaxWidth(),
        content = content
    )
}
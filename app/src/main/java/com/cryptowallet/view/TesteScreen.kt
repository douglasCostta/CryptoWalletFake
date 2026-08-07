package com.cryptowallet.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cryptowallet.model.CoinListItem
import com.cryptowallet.viewmodel.TesteViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TesteScreen(viewModel: TesteViewModel = viewModel()) {

	val uiState = viewModel.uiState.collectAsState().value

	LaunchedEffect(Unit) {
		viewModel.loadCoins()
	}

	when {
		uiState.isLoadingCoins && uiState.coins.isEmpty() -> {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center,
			) {
				CircularProgressIndicator()
			}
		}

		uiState.errorMessage != null && uiState.coins.isEmpty() -> {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(24.dp),
				verticalArrangement = Arrangement.Center,
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				Text(
					text = uiState.errorMessage,
					style = MaterialTheme.typography.bodyLarge,
					textAlign = TextAlign.Center,
				)
				Spacer(modifier = Modifier.height(16.dp))
				Button(onClick = { viewModel.loadCoins() }) {
					Text(text = "Tentar novamente")
				}
			}
		}

		else -> {
			LazyColumn(
				modifier = Modifier
					.fillMaxSize()
					.padding(horizontal = 16.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				item {
					Column(modifier = Modifier.padding(vertical = 16.dp)) {
						Text(
							text = "Moedas",
							style = MaterialTheme.typography.headlineMedium,
							fontWeight = FontWeight.Bold,
						)
						Spacer(modifier = Modifier.height(4.dp))
						Text(
							text = "Teste lista de moedas.",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}

				items(
					items = uiState.coins,
					key = { it.id },
				) { coin ->
					CoinRow(
						coin = coin
					)
				}

				item {
					Spacer(modifier = Modifier.height(16.dp))
				}
			}
		}
	}
}

@Composable
private fun CoinRow( coin: CoinListItem ) {
	val priceFormatter = NumberFormat.getCurrencyInstance(
		Locale.Builder()
			.setLanguage("en")
			.setRegion("US")
			.build(),
	)
	val priceChangeColor = if (coin.priceChangePercentage24h < 0) {
		Color(0xFFFF6B6B)
	} else {
		Color(0xFF35C759)
	}

	Card(
		modifier = Modifier
			.fillMaxWidth(),
		shape = RoundedCornerShape(20.dp),
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp),
		) {
			AsyncImage(
				model = coin.imageUrl,
				contentDescription = coin.name,
				modifier = Modifier
					.size(48.dp)
					.clip(CircleShape)
					.background(MaterialTheme.colorScheme.surface),
				contentScale = ContentScale.Crop,
			)

			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(4.dp),
			) {
				Text(
					text = coin.name,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
				Text(
					text = coin.symbol.uppercase(),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}

			Column(
				horizontalAlignment = Alignment.End,
				verticalArrangement = Arrangement.spacedBy(4.dp),
			) {
				Text(
					text = priceFormatter.format(coin.currentPrice),
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
				)
				Text(
					text = String.format(Locale.US, "%.2f%%", coin.priceChangePercentage24h),
					style = MaterialTheme.typography.bodySmall,
					color = priceChangeColor,
				)
			}
		}
	}
}
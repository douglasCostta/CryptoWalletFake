package com.cryptowallet.view

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cryptowallet.ui.theme.CryptoWalletTheme
import com.cryptowallet.view.component.AppBackground1
import com.cryptowallet.view.component.AppButton
import com.cryptowallet.viewmodel.TransactionViewModel

@Composable
fun TransactionScreen(viewModel: TransactionViewModel = viewModel()) {
	val uiState = viewModel.uiState.collectAsState().value

	AppBackground1 {
		Column() {
			AppButton("Confirm") {
				viewModel.buy()
			}
		}
	}

}

@Preview(showBackground = true)
@Composable
fun Preview() {
	CryptoWalletTheme {
		TransactionScreen()
	}
}
package com.cryptowallet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cryptowallet.data.repository.WalletRepository
import com.cryptowallet.model.TransactionType
import com.cryptowallet.view.BuySellScreen
import com.cryptowallet.view.DashboardScreen
import com.cryptowallet.view.TransactionSuccessScreen
import com.cryptowallet.view.WalletScreen
import com.cryptowallet.viewmodel.BuySellViewModel
import com.cryptowallet.viewmodel.DashboardViewModel
import com.cryptowallet.viewmodel.WalletViewModel

@Composable
fun CryptoWalletNavHost(
    walletRepository: WalletRepository,
    navController: NavHostController = rememberNavController(),
    onLogout: () -> Unit = {},
) {
    NavHost(navController = navController, startDestination = Routes.Wallet) {
        composable<Routes.Wallet> {
            val viewModel: WalletViewModel = viewModel(
                factory = viewModelFactory { initializer { WalletViewModel(walletRepository) } },
            )
            LaunchedEffect(Unit) { viewModel.refresh() }
            WalletScreen(
                viewModel = viewModel,
                onBuyClick = { navController.navigate(Routes.Trade(TransactionType.BUY)) },
                onSellClick = { navController.navigate(Routes.Trade(TransactionType.SELL)) },
                onCoinClick = { coinId -> navController.navigate(Routes.Dashboard(coinId)) },
            )
        }
        composable<Routes.Trade> { backStackEntry ->
            val route = backStackEntry.toRoute<Routes.Trade>()
            val viewModel: BuySellViewModel = viewModel(
                key = "${route.type.name}:${route.preselectedCoinId}",
                factory = viewModelFactory {
                    initializer { BuySellViewModel(walletRepository, route.type, route.preselectedCoinId) }
                },
            )
            BuySellScreen(
                viewModel = viewModel,
                onConfirmed = { result ->
                    navController.navigate(
                        Routes.TransactionSuccess(
                            type = result.type,
                            coinSymbol = result.coinSymbol,
                            amount = result.amountInCoin,
                        ),
                    ) {
                        popUpTo(Routes.Wallet)
                    }
                },
            )
        }
        composable<Routes.TransactionSuccess> { backStackEntry ->
            val route = backStackEntry.toRoute<Routes.TransactionSuccess>()
            TransactionSuccessScreen(
                type = route.type,
                coinSymbol = route.coinSymbol,
                amount = route.amount,
                onBackToWallet = { navController.popBackStack(Routes.Wallet, inclusive = false) },
            )
        }
        composable<Routes.Dashboard> { backStackEntry ->
            val route = backStackEntry.toRoute<Routes.Dashboard>()
            val viewModel: DashboardViewModel = viewModel(
                key = route.coinId,
                factory = viewModelFactory { initializer { DashboardViewModel(route.coinId, walletRepository) } },
            )
            DashboardScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onBuyClick = {
                    navController.navigate(Routes.Trade(TransactionType.BUY, preselectedCoinId = route.coinId))
                },
            )
        }
    }
}

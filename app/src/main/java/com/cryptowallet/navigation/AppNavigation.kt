package com.cryptowallet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cryptowallet.data.local.WalletLocalDataSource
import com.cryptowallet.data.local.walletDataStore
import com.cryptowallet.data.remote.RetrofitInstance
import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.data.repository.CoinGeckoRepository
import com.cryptowallet.data.repository.WalletRepositoryImpl
import com.cryptowallet.view.AuthScreen
import com.google.firebase.auth.FirebaseAuth

object AuthRoutes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
}

@Composable
fun AppNavigation() {
    val isLoggedIn = remember { FirebaseAuth.getInstance().currentUser != null }

    val navController = rememberNavController()
    val startDestination = if (isLoggedIn) AuthRoutes.DASHBOARD else AuthRoutes.LOGIN

    val context = LocalContext.current
    val walletRepository = remember {
        WalletRepositoryImpl(
            coinGeckoRepository = CoinGeckoRepository(CoinGeckoService(RetrofitInstance.api)),
            localDataSource = WalletLocalDataSource(context.applicationContext.walletDataStore),
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(AuthRoutes.LOGIN) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(AuthRoutes.DASHBOARD) {
                        popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(AuthRoutes.DASHBOARD) {
            CryptoWalletNavHost(
                walletRepository = walletRepository,
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(AuthRoutes.LOGIN) {
                        popUpTo(AuthRoutes.DASHBOARD) { inclusive = true }
                    }
                },
            )
        }
    }
}

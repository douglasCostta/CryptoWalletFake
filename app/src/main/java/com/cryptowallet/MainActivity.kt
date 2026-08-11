package com.cryptowallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cryptowallet.data.local.WalletLocalDataSource
import com.cryptowallet.data.local.walletDataStore
import com.cryptowallet.data.remote.RetrofitInstance
import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.data.repository.CoinGeckoRepository
import com.cryptowallet.data.repository.WalletRepository
import com.cryptowallet.data.repository.WalletRepositoryImpl
import com.cryptowallet.navigation.CryptoWalletNavHost
import com.cryptowallet.ui.theme.WalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val walletRepository: WalletRepository = WalletRepositoryImpl(
            coinGeckoRepository = CoinGeckoRepository(CoinGeckoService(RetrofitInstance.api)),
            localDataSource = WalletLocalDataSource(applicationContext.walletDataStore),
        )

        setContent {
            WalletTheme {
                CryptoWalletNavHost(walletRepository = walletRepository)
            }
        }
    }
}

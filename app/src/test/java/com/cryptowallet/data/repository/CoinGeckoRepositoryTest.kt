package com.cryptowallet.data.repository

import com.cryptowallet.data.remote.service.CoinGeckoService
import com.cryptowallet.fakes.FakeCoinGeckoApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CoinGeckoRepositoryTest {

    @Test
    fun `getCoins passes vsCurrency through to the API`() = runTest {
        val api = FakeCoinGeckoApi()
        val repository = CoinGeckoRepository(CoinGeckoService(api))

        repository.getCoins(vsCurrency = "brl")

        assertEquals("brl", api.lastVsCurrency)
    }

    @Test
    fun `getCoinsByIds requests only the given ids and maps them`() = runTest {
        val api = FakeCoinGeckoApi()
        val repository = CoinGeckoRepository(CoinGeckoService(api))

        val result = repository.getCoinsByIds(ids = listOf("bitcoin", "ethereum"), vsCurrency = "brl")

        assertEquals("brl", api.lastVsCurrency)
        assertEquals("bitcoin,ethereum", api.lastIds)
        assertEquals(setOf("bitcoin", "ethereum"), result.map { it.id }.toSet())
    }

    @Test
    fun `getCoinsByIds returns empty list for empty ids without calling the API`() = runTest {
        val api = FakeCoinGeckoApi()
        val repository = CoinGeckoRepository(CoinGeckoService(api))

        val result = repository.getCoinsByIds(ids = emptyList(), vsCurrency = "brl")

        assertEquals(emptyList<String>(), result.map { it.id })
        assertEquals(null, api.lastIds)
    }
}

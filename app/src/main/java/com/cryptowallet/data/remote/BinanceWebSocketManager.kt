package com.cryptowallet.data.remote

import android.util.Log
import com.cryptowallet.data.remote.dto.BinanceKlineEvent
import com.cryptowallet.model.CoinGraphPoints
import com.google.gson.Gson
import okhttp3.*

class BinanceWebSocketManager(
    private val onUpdate: (CoinGraphPoints) -> Unit
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private var webSocket: WebSocket? = null

    fun connect(coinSymbol: String, interval: String = "1h") {
        disconnect()
        
        val symbol = "${coinSymbol.lowercase()}usdt"
        val request = Request.Builder()
            .url("wss://stream.binance.com:9443/ws/$symbol@kline_$interval")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val event = gson.fromJson(text, BinanceKlineEvent::class.java)
                    val kline = event.kline
                    
                    val point = CoinGraphPoints(
                        timestamp = kline.startTime,
                        open = kline.open.toDouble(),
                        high = kline.high.toDouble(),
                        low = kline.low.toDouble(),
                        close = kline.close.toDouble()
                    )
                    
                    onUpdate(point)
                } catch (e: Exception) {
                    Log.e("BinanceWS", "Error parsing message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("BinanceWS", "Connection failed", t)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Closed by manager")
        webSocket = null
    }
}

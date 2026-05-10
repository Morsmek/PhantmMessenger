package com.stagic.phantm.transport

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion

/**
 * OkHttp engine configured for WebSocket + TLS 1.3 (with TLS 1.2 fallback on API < 29).
 * Connection metadata is never logged (AC-M05-6).
 */
actual fun createTransportHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            connectionSpecs(
                listOf(
                    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
                        .build(),
                ),
            )
        }
    }
    install(WebSockets)
}

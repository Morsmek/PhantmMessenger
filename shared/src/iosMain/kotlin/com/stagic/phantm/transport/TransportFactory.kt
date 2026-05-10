package com.stagic.phantm.transport

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets

/** Darwin engine — TLS handled by iOS NSURLSession (TLS 1.3 on iOS 13+). */
actual fun createTransportHttpClient(): HttpClient = HttpClient(Darwin) {
    install(WebSockets)
}

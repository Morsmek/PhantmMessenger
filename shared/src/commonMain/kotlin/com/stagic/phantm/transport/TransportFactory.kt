package com.stagic.phantm.transport

import io.ktor.client.HttpClient

/** Platform-specific [HttpClient] with WebSockets plugin and TLS configured. */
expect fun createTransportHttpClient(): HttpClient

/** Create a [TransportClient] backed by the platform's default HTTP engine. */
fun createTransportClient(): TransportClient = KtorTransportClient(createTransportHttpClient())

/** Create a [TransportClient] using a custom [httpClient] (for testing). */
fun createTransportClient(httpClient: HttpClient): TransportClient = KtorTransportClient(httpClient)

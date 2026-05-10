package com.stagic.phantm.relay

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/**
 * Relay node entry point.
 *
 * TLS (AC-M09-1): set RELAY_TLS_KEYSTORE and RELAY_TLS_PASSWORD env vars to a JKS keystore
 * path/password, then configure Netty's sslConnector — see https://ktor.io/docs/server-ssl.html
 */
fun main() {
    val config = RelayConfig()
    val registry = ConnectionRegistry()
    val store = EnvelopeStore(ttlMs = config.envelopeTtlMs)

    embeddedServer(Netty, port = config.port) {
        configureRelay(registry, store)
    }.start(wait = true)
}

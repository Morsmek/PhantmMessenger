package com.stagic.phantm.relay

/**
 * Runtime configuration for the relay node.
 * TLS (AC-M09-1) is enabled when [tlsKeyStorePath] and [tlsKeyStorePassword] are non-null.
 */
data class RelayConfig(
    val port: Int = 8080,
    val tlsPort: Int = 8443,
    /** TTL for envelopes queued for offline recipients (AC-M09-3). Default: 7 days. */
    val envelopeTtlMs: Long = 7L * 24 * 60 * 60 * 1_000,
    val tlsKeyStorePath: String? = System.getenv("RELAY_TLS_KEYSTORE"),
    val tlsKeyStorePassword: String? = System.getenv("RELAY_TLS_PASSWORD"),
)

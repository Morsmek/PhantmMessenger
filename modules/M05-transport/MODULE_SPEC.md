# M05 — Transport

**Status:** NOT STARTED
**Dependencies:** M07 (Message Protocol), M03 (Local DB)
**Dependents:** M06, M09

---

## Purpose

The sole network I/O layer. All communication with relay nodes goes through this module.
No other module may open a raw network socket. Uses Ktor WebSocket client over TLS 1.3.

---

## Public API

```kotlin
package com.stagic.phantm.transport

interface TransportClient {
    /** Connect to the relay node. */
    suspend fun connect(relayUrl: String): Result<Unit, TransportError>

    /** Send an encrypted envelope. The payload must already be encrypted by M07. */
    suspend fun send(envelope: TransportEnvelope): Result<Unit, TransportError>

    /** Incoming envelopes as a cold flow. */
    val incomingEnvelopes: Flow<TransportEnvelope>

    /** Disconnect gracefully. */
    suspend fun disconnect()

    val connectionState: StateFlow<ConnectionState>
}

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }

sealed class TransportError {
    object ConnectionRefused : TransportError()
    object TlsHandshakeFailed : TransportError()
    data class Unknown(val cause: Throwable) : TransportError()
}
```

---

## Acceptance Criteria

See STATUS.md

package com.stagic.phantm.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The sole network I/O interface for Phantm.
 *
 * All communication with relay nodes MUST go through this interface. No other module
 * may open a raw network socket (AC-M05-5). All data sent via [send] must already be
 * encrypted by M07 — this layer is transport-only with no cryptographic responsibility.
 */
interface TransportClient {

    /** Current connection state. Starts as [ConnectionState.DISCONNECTED]. */
    val connectionState: StateFlow<ConnectionState>

    /** Cold flow of binary envelopes received from the relay. */
    val incomingEnvelopes: Flow<TransportEnvelope>

    /**
     * Connect to the relay node at [relayUrl] (e.g. `wss://relay.example.com/ws`).
     * Returns [TransportError.ConnectionRefused] if the connection cannot be established.
     * After a successful initial connection, reconnection with exponential backoff is
     * automatic on connection loss.
     */
    suspend fun connect(relayUrl: String): TransportResult<Unit>

    /**
     * Enqueue [envelope] for delivery to the relay.
     * The [TransportEnvelope.bytes] must already be M07-encrypted — never plaintext.
     * Returns [TransportError.ConnectionRefused] if not connected.
     */
    suspend fun send(envelope: TransportEnvelope): TransportResult<Unit>

    /** Gracefully disconnect and cancel all background reconnect work. */
    suspend fun disconnect()
}

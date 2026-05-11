package com.stagic.phantm.integration.helpers

import com.stagic.phantm.ok
import com.stagic.phantm.transport.ConnectionState
import com.stagic.phantm.transport.TransportClient
import com.stagic.phantm.transport.TransportEnvelope
import com.stagic.phantm.transport.TransportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [TransportClient] backed by [InMemoryRelay].
 *
 * Used in integration tests to exercise the full Alice→relay→Bob message pipeline
 * without network I/O.
 */
class FakeTransportClient(
    val recipientId: String,
    private val relay: InMemoryRelay,
) : TransportClient {

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val _incoming = MutableSharedFlow<TransportEnvelope>(replay = 64)

    override val connectionState: StateFlow<ConnectionState> = _state.asStateFlow()
    override val incomingEnvelopes: Flow<TransportEnvelope> = _incoming.asSharedFlow()

    override suspend fun connect(relayUrl: String): TransportResult<Unit> {
        relay.register(recipientId, this)
        _state.value = ConnectionState.CONNECTED
        return Unit.ok()
    }

    override suspend fun send(envelope: TransportEnvelope): TransportResult<Unit> {
        relay.deliver(envelope)
        return Unit.ok()
    }

    override suspend fun disconnect() {
        relay.unregister(recipientId)
        _state.value = ConnectionState.DISCONNECTED
    }

    /** Called by [InMemoryRelay] to deliver an incoming envelope to this client. */
    internal suspend fun receive(envelope: TransportEnvelope) {
        _incoming.emit(envelope)
    }
}

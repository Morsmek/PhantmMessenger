@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.transport

import com.stagic.phantm.err
import com.stagic.phantm.ok
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Ktor WebSocket implementation of [TransportClient].
 *
 * All frames sent to the relay are binary protobuf [TransportFrame] — never text.
 * Connection metadata (URL, IP) is never written to logs (AC-M05-6).
 */
internal class KtorTransportClient(
    private val httpClient: HttpClient,
) : TransportClient {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingEnvelopes = MutableSharedFlow<TransportEnvelope>(extraBufferCapacity = 64)
    override val incomingEnvelopes: Flow<TransportEnvelope> = _incomingEnvelopes.asSharedFlow()

    private val outbound = Channel<ByteArray>(capacity = Channel.BUFFERED)
    private var sessionScope: CoroutineScope? = null
    private var sessionJob: Job? = null

    override suspend fun connect(relayUrl: String): TransportResult<Unit> {
        sessionScope?.cancel()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        sessionScope = scope
        _connectionState.value = ConnectionState.CONNECTING

        val firstResult = CompletableDeferred<TransportResult<Unit>>()
        sessionJob = scope.launch { runLoop(relayUrl, firstResult) }
        return firstResult.await()
    }

    private suspend fun runLoop(
        relayUrl: String,
        firstResult: CompletableDeferred<TransportResult<Unit>>,
    ) {
        var attempt = 0
        var firstSignaled = false

        while (true) {
            if (attempt > 0) {
                _connectionState.value = ConnectionState.RECONNECTING
                delay(reconnectDelayMs(attempt - 1))
            }
            try {
                httpClient.webSocket(relayUrl) {
                    _connectionState.value = ConnectionState.CONNECTED
                    attempt = 0
                    if (!firstSignaled) {
                        firstResult.complete(Unit.ok())
                        firstSignaled = true
                    }
                    handleSession()
                }
            } catch (e: CancellationException) {
                if (!firstSignaled) firstResult.complete(TransportError.ConnectionRefused.err())
                _connectionState.value = ConnectionState.DISCONNECTED
                return
            } catch (e: Exception) {
                if (!firstSignaled) {
                    firstResult.complete(TransportError.ConnectionRefused.err())
                    firstSignaled = true
                    _connectionState.value = ConnectionState.DISCONNECTED
                    return // no retry on initial connection failure
                }
            }
            _connectionState.value = ConnectionState.DISCONNECTED
            attempt++
        }
    }

    private suspend fun DefaultClientWebSocketSession.handleSession() {
        val senderJob = launch {
            while (true) {
                val bytes = outbound.receive()
                send(Frame.Binary(true, bytes))
            }
        }
        try {
            for (frame in incoming) {
                if (frame is Frame.Binary) {
                    decodeFrame(frame.readBytes())?.let { _incomingEnvelopes.emit(it) }
                }
            }
        } finally {
            senderJob.cancel()
        }
    }

    override suspend fun send(envelope: TransportEnvelope): TransportResult<Unit> {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return TransportError.ConnectionRefused.err()
        }
        return try {
            outbound.send(encodeFrame(envelope))
            Unit.ok()
        } catch (e: Exception) {
            TransportError.Unknown(e).err()
        }
    }

    override suspend fun disconnect() {
        sessionScope?.cancel()
        sessionScope = null
        sessionJob = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // ── Frame encoding ────────────────────────────────────────────────────────

    private fun encodeFrame(envelope: TransportEnvelope): ByteArray =
        ProtoBuf.encodeToByteArray(
            TransportFrame.serializer(),
            TransportFrame(recipientId = envelope.recipientId, envelopeBytes = envelope.bytes),
        )

    private fun decodeFrame(bytes: ByteArray): TransportEnvelope? = try {
        val frame = ProtoBuf.decodeFromByteArray(TransportFrame.serializer(), bytes)
        TransportEnvelope(recipientId = frame.recipientId, bytes = frame.envelopeBytes)
    } catch (e: Exception) {
        null
    }
}

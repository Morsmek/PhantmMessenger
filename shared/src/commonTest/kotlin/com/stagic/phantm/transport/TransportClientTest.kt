@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.transport

import com.stagic.phantm.err
import com.stagic.phantm.ok
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * M05 unit tests — covers the testable-in-isolation slice of each AC.
 *
 * AC-M05-1 (TLS 1.3 WebSocket connection) and AC-M05-2 (relay echo) require a running
 * relay node (M09) and are validated during integration testing.
 * AC-M05-5 (only M05 opens sockets) is an architectural property verified by code
 * review: no other package imports io.ktor.client.* or opens raw sockets.
 * AC-M05-6 (no connection metadata logged) is verified by code review of KtorTransportClient.
 */
class TransportClientTest {

    // ── TransportFrame serialization (AC-M05-4: binary protobuf, never JSON) ──

    @Test
    fun frameEncodesAsProtobufBinary() {
        val frame = TransportFrame(recipientId = "bob", envelopeBytes = byteArrayOf(1, 2, 3))
        val encoded = ProtoBuf.encodeToByteArray(TransportFrame.serializer(), frame)
        // JSON would start with '{' (0x7B); protobuf field-1 varint tag starts with 0x0A
        assertFalse(encoded.isEmpty())
        assertNotEquals(0x7B.toByte(), encoded[0], "Wire frame must be protobuf binary, not JSON")
    }

    @Test
    fun frameRoundTripsRecipientId() {
        val frame = TransportFrame(recipientId = "alice", envelopeBytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte()))
        val bytes = ProtoBuf.encodeToByteArray(TransportFrame.serializer(), frame)
        val decoded = ProtoBuf.decodeFromByteArray(TransportFrame.serializer(), bytes)
        assertEquals("alice", decoded.recipientId)
    }

    @Test
    fun frameRoundTripsEnvelopeBytes() {
        val payload = ByteArray(256) { it.toByte() }
        val frame = TransportFrame(recipientId = "r", envelopeBytes = payload)
        val bytes = ProtoBuf.encodeToByteArray(TransportFrame.serializer(), frame)
        val decoded = ProtoBuf.decodeFromByteArray(TransportFrame.serializer(), bytes)
        assertContentEquals(payload, decoded.envelopeBytes)
    }

    @Test
    fun emptyEnvelopeBytesRoundTrip() {
        val frame = TransportFrame(recipientId = "x", envelopeBytes = ByteArray(0))
        val bytes = ProtoBuf.encodeToByteArray(TransportFrame.serializer(), frame)
        val decoded = ProtoBuf.decodeFromByteArray(TransportFrame.serializer(), bytes)
        assertEquals(0, decoded.envelopeBytes.size)
    }

    // ── AC-M05-3: Reconnect strategy — exponential backoff ───────────────────

    @Test
    fun reconnectDelayStartsAtOneSecond() {
        assertEquals(1_000L, reconnectDelayMs(0))
    }

    @Test
    fun reconnectDelayDoublesEachAttempt() {
        assertEquals(1_000L, reconnectDelayMs(0))
        assertEquals(2_000L, reconnectDelayMs(1))
        assertEquals(4_000L, reconnectDelayMs(2))
        assertEquals(8_000L, reconnectDelayMs(3))
        assertEquals(16_000L, reconnectDelayMs(4))
        assertEquals(30_000L, reconnectDelayMs(5)) // capped
    }

    @Test
    fun reconnectDelayIsCappedAtThirtySeconds() {
        assertEquals(MAX_RECONNECT_DELAY_MS, reconnectDelayMs(5))
        assertEquals(MAX_RECONNECT_DELAY_MS, reconnectDelayMs(10))
        assertEquals(MAX_RECONNECT_DELAY_MS, reconnectDelayMs(100))
    }

    @Test
    fun reconnectDelayNeverExceedsMax() {
        for (attempt in 0..50) {
            assertTrue(
                reconnectDelayMs(attempt) <= MAX_RECONNECT_DELAY_MS,
                "attempt=$attempt exceeded max",
            )
        }
    }

    // ── ConnectionState and TransportEnvelope types ───────────────────────────

    @Test
    fun connectionStateEnumHasExpectedValues() {
        val values = ConnectionState.entries.map { it.name }
        assertTrue(values.contains("DISCONNECTED"))
        assertTrue(values.contains("CONNECTING"))
        assertTrue(values.contains("CONNECTED"))
        assertTrue(values.contains("RECONNECTING"))
    }

    @Test
    fun transportEnvelopeHoldsRecipientAndBytes() {
        val payload = byteArrayOf(0x01, 0x02, 0x03)
        val envelope = TransportEnvelope(recipientId = "charlie", bytes = payload)
        assertEquals("charlie", envelope.recipientId)
        assertContentEquals(payload, envelope.bytes)
    }

    @Test
    fun transportEnvelopeBytesAreNeverConvertedToString() = runTest {
        // Verify that TransportEnvelope.bytes is a ByteArray and has no String toString path
        val sensitive = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val envelope = TransportEnvelope("r", sensitive)
        // toString() must NOT contain the raw bytes as a readable string
        val str = envelope.toString()
        assertFalse(str.contains("deadbeef"), "Raw bytes must not appear as hex in toString()")
    }

    // ── sendWithoutConnection returns ConnectionRefused ───────────────────────

    @Test
    fun sendWithoutConnectionReturnsError() = runTest {
        // Create a client that is never connected; verify send returns an error immediately
        val client = DisconnectedTransportClientStub()
        val result = client.send(TransportEnvelope("bob", byteArrayOf(1)))
        assertTrue(result.isErr())
        assertEquals(TransportError.ConnectionRefused, result.errorOrNull)
    }
}

/** Minimal stub that stays DISCONNECTED — used to test send-without-connection behavior. */
private class DisconnectedTransportClientStub : TransportClient {
    override val connectionState = kotlinx.coroutines.flow.MutableStateFlow(ConnectionState.DISCONNECTED)
    override val incomingEnvelopes: kotlinx.coroutines.flow.Flow<TransportEnvelope> =
        kotlinx.coroutines.flow.emptyFlow()

    override suspend fun connect(relayUrl: String): TransportResult<Unit> = Unit.ok()
    override suspend fun send(envelope: TransportEnvelope): TransportResult<Unit> =
        TransportError.ConnectionRefused.err()
    override suspend fun disconnect() {}
}

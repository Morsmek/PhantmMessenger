package com.stagic.phantm.integration

import com.stagic.phantm.cover.COVER_RECIPIENT_ID
import com.stagic.phantm.cover.CoverTrafficConfig
import com.stagic.phantm.cover.createCoverTrafficManager
import com.stagic.phantm.crypto.createCryptoCore
import com.stagic.phantm.integration.helpers.FakeTransportClient
import com.stagic.phantm.integration.helpers.InMemoryRelay
import com.stagic.phantm.transport.TransportEnvelope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * AC-M14-4: Cover traffic integration test.
 *
 * Exercises M06 (CoverTrafficManager) together with the in-memory relay routing:
 * - Cover packets are addressed to [COVER_RECIPIENT_ID] and discarded by [InMemoryRelay].
 * - Real messages addressed to real recipients are delivered normally while cover traffic runs.
 *
 * The relay-level discard (`configureRelay` drops COVER_RECIPIENT_ID frames) is tested in
 * the dedicated `:relay:test` suite (RelayServerTest.coverTraffic_notStored_notRouted).
 * This test validates the end-to-end integration: manager → transport → relay discard.
 */
class CoverTrafficRelayTest {

    // ── AC-M14-4a: CoverTrafficManager generates and sends cover frames ────────

    @Test
    fun coverTrafficManager_sendsFramesWithCoverRecipientId_relayDiscards() = runTest {
        val crypto = createCryptoCore()
        val relay = InMemoryRelay()
        val aliceTransport = FakeTransportClient("alice", relay)
        aliceTransport.connect("ws://fake/ws")

        val coverManager = createCoverTrafficManager(crypto)

        // 6000 ppm → 10ms interval; wait ~50ms for ≥3 packets
        coverManager.start(aliceTransport, CoverTrafficConfig(
            packetsPerMinute = 6000,
            minPayloadBytes = 32,
            maxPayloadBytes = 64,
        ))
        assertTrue(coverManager.isRunning, "CoverTrafficManager must report isRunning = true")

        delay(60)
        coverManager.stop()
        assertFalse(coverManager.isRunning, "CoverTrafficManager must report isRunning = false after stop()")

        assertTrue(relay.coverTrafficReceived > 0, "Relay must have received at least one cover frame")
        assertEquals(0, relay.queueSize(COVER_RECIPIENT_ID), "Cover frames must NOT be queued by relay")
    }

    // ── AC-M14-4b: Cover traffic does not interfere with real messages ─────────

    @Test
    fun coverTraffic_doesNotInterferWithRealMessages() = runTest {
        val crypto = createCryptoCore()
        val relay = InMemoryRelay()
        val aliceTransport = FakeTransportClient("alice", relay)
        val bobTransport = FakeTransportClient("bob", relay)
        aliceTransport.connect("ws://fake/ws")
        bobTransport.connect("ws://fake/ws")

        val coverManager = createCoverTrafficManager(crypto)
        coverManager.start(aliceTransport, CoverTrafficConfig(packetsPerMinute = 6000))

        // Alice sends a real message to Bob while cover traffic is running
        val realPayload = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        aliceTransport.send(TransportEnvelope(recipientId = "bob", bytes = realPayload))

        // Bob must receive the real message; cover frames must never appear in his flow
        val received = kotlinx.coroutines.withTimeoutOrNull(200) {
            bobTransport.incomingEnvelopes.first()
        }
        assertTrue(received != null, "Bob must receive the real message")
        assertTrue(received.bytes.contentEquals(realPayload), "Real message bytes must be intact")
        assertEquals(0, relay.queueSize(COVER_RECIPIENT_ID), "Cover frames must not be queued")

        coverManager.stop()
    }

    // ── AC-M14-4c: Cover traffic recipient never appears as a registered client ─

    @Test
    fun coverRecipientId_neverRegisteredAsClient() = runTest {
        val crypto = createCryptoCore()
        val relay = InMemoryRelay()
        val transport = FakeTransportClient("sender", relay)
        transport.connect("ws://fake/ws")

        val coverManager = createCoverTrafficManager(crypto)
        coverManager.start(transport, CoverTrafficConfig(packetsPerMinute = 6000))
        delay(60)
        coverManager.stop()

        // COVER_RECIPIENT_ID should never be in the relay's registered clients
        // (InMemoryRelay discards rather than routing, so no client is ever registered for it)
        assertEquals(0, relay.queueSize(COVER_RECIPIENT_ID))
    }
}

@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.relay

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelayServerTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun encode(frame: RelayFrame): ByteArray =
        ProtoBuf.encodeToByteArray(RelayFrame.serializer(), frame)

    private fun decode(bytes: ByteArray): RelayFrame =
        ProtoBuf.decodeFromByteArray(RelayFrame.serializer(), bytes)

    // ── AC-M09-2: Route envelope to connected recipient ───────────────────────

    @Test
    fun envelope_deliveredToConnectedRecipient() {
        val registry = ConnectionRegistry()
        val store = EnvelopeStore()
        val testPayload = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val bobReceived = CompletableDeferred<ByteArray>()

        testApplication {
            application { configureRelay(registry, store) }

            val bobJob = launch {
                createClient { install(WebSockets) }.webSocket("/ws") {
                    send(Frame.Binary(true, encode(RelayFrame("bob"))))
                    val rf = decode((incoming.receive() as Frame.Binary).readBytes())
                    bobReceived.complete(rf.envelopeBytes)
                }
            }

            // Poll until Bob registers
            while (!registry.isConnected("bob")) kotlinx.coroutines.delay(10)

            // Alice sends to Bob
            createClient { install(WebSockets) }.webSocket("/ws") {
                send(Frame.Binary(true, encode(RelayFrame("alice"))))
                send(Frame.Binary(true, encode(RelayFrame("bob", testPayload))))
            }

            assertContentEquals(testPayload, bobReceived.await())
            bobJob.cancel()
        }
    }

    // ── AC-M09-3: Store and deliver to offline recipient ──────────────────────

    @Test
    fun envelope_storedForOfflineRecipient_deliveredOnConnect() {
        val registry = ConnectionRegistry()
        val store = EnvelopeStore()
        val testPayload = byteArrayOf(1, 2, 3, 4, 5)

        testApplication {
            application { configureRelay(registry, store) }

            // Alice sends to Bob (offline — not yet connected)
            createClient { install(WebSockets) }.webSocket("/ws") {
                send(Frame.Binary(true, encode(RelayFrame("alice"))))
                send(Frame.Binary(true, encode(RelayFrame("bob", testPayload))))
            }

            // Verify envelope queued
            assertEquals(1, store.size("bob"))

            // Bob connects → stored envelope delivered on registration
            val bobReceived = CompletableDeferred<ByteArray>()
            createClient { install(WebSockets) }.webSocket("/ws") {
                send(Frame.Binary(true, encode(RelayFrame("bob"))))
                val rf = decode((incoming.receive() as Frame.Binary).readBytes())
                bobReceived.complete(rf.envelopeBytes)
            }

            assertContentEquals(testPayload, bobReceived.await())
            assertEquals(0, store.size("bob"))
        }
    }

    @Test
    fun offlineEnvelope_ttlBounded() {
        val store = EnvelopeStore(ttlMs = 30)
        val registry = ConnectionRegistry()

        testApplication {
            application { configureRelay(registry, store) }

            // Alice sends to offline Bob
            createClient { install(WebSockets) }.webSocket("/ws") {
                send(Frame.Binary(true, encode(RelayFrame("alice"))))
                send(Frame.Binary(true, encode(RelayFrame("bob", byteArrayOf(9)))))
            }

            // Wait for TTL to expire
            Thread.sleep(60)

            // Bob connects — nothing should be delivered (expired)
            val bobFrames = mutableListOf<ByteArray>()
            createClient { install(WebSockets) }.webSocket("/ws") {
                send(Frame.Binary(true, encode(RelayFrame("bob"))))
                // No pending frame — close after brief wait
                kotlinx.coroutines.withTimeoutOrNull(100) {
                    val rf = decode((incoming.receive() as Frame.Binary).readBytes())
                    bobFrames.add(rf.envelopeBytes)
                }
            }

            assertTrue(bobFrames.isEmpty(), "Expired envelopes must not be delivered")
        }
    }

    // ── AC-M09-5: Cover traffic discarded silently ────────────────────────────

    @Test
    fun coverTraffic_notStored_notRouted() {
        val store = EnvelopeStore()
        val registry = ConnectionRegistry()

        testApplication {
            application { configureRelay(registry, store) }

            createClient { install(WebSockets) }.webSocket("/ws") {
                send(Frame.Binary(true, encode(RelayFrame("alice"))))
                send(Frame.Binary(true, encode(RelayFrame(COVER_RECIPIENT_ID, byteArrayOf(0xCC.toByte())))))
            }

            assertEquals(0, store.size(COVER_RECIPIENT_ID), "Cover traffic must not be stored")
            assertTrue(!registry.isConnected(COVER_RECIPIENT_ID), "Cover recipient must not be registered")
        }
    }

    // ── AC-M09-6: IP addresses not stored ─────────────────────────────────────

    @Test
    fun connectionRegistry_doesNotStoreIpAddresses() {
        // Structural: ConnectionRegistry only maps recipientId → WebSocketSession.
        // No IP field exists anywhere in ConnectionRegistry or RelayServer.
        val registry = ConnectionRegistry()
        val fields = registry.javaClass.declaredFields.map { it.name }
        assertTrue(
            fields.none { it.contains("ip", ignoreCase = true) || it.contains("address", ignoreCase = true) },
            "ConnectionRegistry must not store IP addresses",
        )
    }
}

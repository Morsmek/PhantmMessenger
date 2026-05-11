package com.stagic.phantm.integration

import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.createCryptoCore
import com.stagic.phantm.identity.PlatformContext
import com.stagic.phantm.identity.createIdentityManager
import com.stagic.phantm.integration.helpers.FakeTransportClient
import com.stagic.phantm.integration.helpers.InMemoryRelay
import com.stagic.phantm.protocol.MessagePayload
import com.stagic.phantm.protocol.MessageType
import com.stagic.phantm.protocol.createMessageProtocol
import com.stagic.phantm.transport.TransportEnvelope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * AC-M14-1: Full Alice→Bob message send/receive integration test.
 *
 * Exercises the complete pipeline across M01 (crypto), M02 (identity),
 * M05 (transport), M07 (message protocol), and the in-memory relay routing:
 *   Alice creates identity → encrypts M07 envelope for Bob →
 *   sends via FakeTransportClient → InMemoryRelay routes → Bob receives →
 *   Bob decrypts → plaintext matches.
 */
class AliceToBobIntegrationTest {

    @Test
    fun aliceSendsMessage_bobReceivesAndDecrypts() = runTest {
        val relay = InMemoryRelay()

        // Each participant has an isolated crypto core (simulates separate devices)
        val aliceCrypto = createCryptoCore()
        val bobCrypto = createCryptoCore()

        val aliceManager = createIdentityManager(aliceCrypto, PlatformContext())
        val bobManager = createIdentityManager(bobCrypto, PlatformContext())

        val aliceId = (aliceManager.createIdentity() as PhantmResult.Ok).value
        val bobId = (bobManager.createIdentity() as PhantmResult.Ok).value
        val alicePrivKeys = (aliceManager.loadPrivateKeys() as PhantmResult.Ok).value
        val bobPrivKeys = (bobManager.loadPrivateKeys() as PhantmResult.Ok).value

        // Wire up transports
        val aliceTransport = FakeTransportClient(aliceId.id, relay)
        val bobTransport = FakeTransportClient(bobId.id, relay)
        aliceTransport.connect("ws://localhost/ws")
        bobTransport.connect("ws://localhost/ws")

        val aliceProtocol = createMessageProtocol(aliceCrypto)
        val bobProtocol = createMessageProtocol(bobCrypto)

        // Alice encrypts a message for Bob
        val plaintext = "Hello Bob — this is an end-to-end encrypted message!"
        val envelopeBytes = (aliceProtocol.encrypt(
            payload = MessagePayload(type = MessageType.TEXT, text = plaintext),
            senderId = aliceId.id,
            recipientId = bobId.id,
            senderEd25519PrivKey = alicePrivKeys.ed25519PrivKey,
            recipientX25519Pub = bobId.x25519PublicKey,
            recipientMlKemPub = bobId.mlKem768PublicKey,
        ) as PhantmResult.Ok).value

        // Alice sends via transport → relay routes to Bob
        aliceTransport.send(TransportEnvelope(recipientId = bobId.id, bytes = envelopeBytes))

        // Bob receives via incoming flow
        val received = bobTransport.incomingEnvelopes.first()
        assertContentEquals(envelopeBytes, received.bytes, "Received bytes must match sent bytes exactly")

        // Bob decrypts — plaintext must match
        val decrypted = bobProtocol.decrypt(
            envelopeBytes = received.bytes,
            recipientId = bobId.id,
            recipientX25519PrivKey = bobPrivKeys.x25519PrivKey,
            recipientMlKemPrivKey = bobPrivKeys.mlKemPrivKey,
            senderEd25519Pub = aliceId.ed25519PublicKey,
        )
        assertIs<PhantmResult.Ok<MessagePayload>>(decrypted)
        assertEquals(plaintext, decrypted.value.text)
        assertEquals(MessageType.TEXT, decrypted.value.type)

        alicePrivKeys.zero()
        bobPrivKeys.zero()
    }

    @Test
    fun offlineRecipient_envelopeQueued_deliveredOnConnect() = runTest {
        val relay = InMemoryRelay()
        val aliceCrypto = createCryptoCore()
        val bobCrypto = createCryptoCore()

        val aliceManager = createIdentityManager(aliceCrypto, PlatformContext())
        val bobManager = createIdentityManager(bobCrypto, PlatformContext())
        val aliceId = (aliceManager.createIdentity() as PhantmResult.Ok).value
        val bobId = (bobManager.createIdentity() as PhantmResult.Ok).value
        val alicePrivKeys = (aliceManager.loadPrivateKeys() as PhantmResult.Ok).value

        // Only Alice connects — Bob is offline
        val aliceTransport = FakeTransportClient(aliceId.id, relay)
        aliceTransport.connect("ws://localhost/ws")

        val aliceProtocol = createMessageProtocol(aliceCrypto)
        val envelopeBytes = (aliceProtocol.encrypt(
            payload = MessagePayload(type = MessageType.TEXT, text = "Are you there?"),
            senderId = aliceId.id,
            recipientId = bobId.id,
            senderEd25519PrivKey = alicePrivKeys.ed25519PrivKey,
            recipientX25519Pub = bobId.x25519PublicKey,
            recipientMlKemPub = bobId.mlKem768PublicKey,
        ) as PhantmResult.Ok).value

        aliceTransport.send(TransportEnvelope(bobId.id, envelopeBytes))
        assertEquals(1, relay.queueSize(bobId.id), "Offline envelope must be queued in relay")

        // Bob comes online — queued envelope is flushed on connect
        val bobTransport = FakeTransportClient(bobId.id, relay)
        val deliveredEnvelope = async { bobTransport.incomingEnvelopes.first() }
        bobTransport.connect("ws://localhost/ws")

        assertContentEquals(envelopeBytes, deliveredEnvelope.await().bytes)
        assertEquals(0, relay.queueSize(bobId.id), "Queue must be empty after delivery")

        alicePrivKeys.zero()
    }
}

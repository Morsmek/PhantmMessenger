@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.protocol

import com.stagic.phantm.crypto.createCryptoCore
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Covers AC-M07-1 through AC-M07-6.
 */
class MessageProtocolTest {

    private val crypto = createCryptoCore()

    // Each test creates its own NonceTracker so replays don't bleed between tests.
    private fun protocol() = createMessageProtocol(crypto, NonceTracker())

    // Sender key material
    private val senderX25519 = crypto.generateX25519KeyPair()
    private val senderEd25519 = crypto.generateEd25519KeyPair()
    private val senderMlKem = crypto.generateMlKem768KeyPair()

    // Recipient key material
    private val recipientX25519 = crypto.generateX25519KeyPair()
    private val recipientEd25519 = crypto.generateEd25519KeyPair()
    private val recipientMlKem = crypto.generateMlKem768KeyPair()

    private fun encrypt(
        p: MessageProtocol,
        payload: MessagePayload = MessagePayload(text = "hello"),
        senderId: String = "alice",
        recipientId: String = "bob",
    ): ByteArray = p.encrypt(
        payload = payload,
        senderId = senderId,
        recipientId = recipientId,
        senderEd25519PrivKey = senderEd25519.privateKey,
        recipientX25519Pub = recipientX25519.publicKey,
        recipientMlKemPub = recipientMlKem.publicKey,
    ).valueOrNull!!

    private fun decrypt(
        p: MessageProtocol,
        envelopeBytes: ByteArray,
        recipientId: String = "bob",
    ) = p.decrypt(
        envelopeBytes = envelopeBytes,
        recipientId = recipientId,
        recipientX25519PrivKey = recipientX25519.privateKey,
        recipientMlKemPrivKey = recipientMlKem.privateKey,
        senderEd25519Pub = senderEd25519.publicKey,
    )

    // ── AC-M07-1: 1000 random-payload round-trips ─────────────────────────────

    @Test
    fun encryptDecryptRoundTrip() {
        val p = protocol()
        val payload = MessagePayload(text = "round-trip test", type = MessageType.TEXT)
        val envelope = encrypt(p, payload)
        val result = decrypt(p, envelope)
        assertTrue(result.isOk())
        assertEquals(payload.text, result.valueOrNull!!.text)
        assertEquals(payload.type, result.valueOrNull!!.type)
    }

    @Test
    fun roundTripWith1000RandomPayloads() {
        val p = protocol()
        repeat(1000) { i ->
            val text = "msg-$i-${crypto.randomBytes(32).joinToString("") { it.toString(16) }}"
            val payload = MessagePayload(text = text)
            val envelope = p.encrypt(
                payload = payload,
                senderId = "alice",
                recipientId = "bob",
                senderEd25519PrivKey = senderEd25519.privateKey,
                recipientX25519Pub = recipientX25519.publicKey,
                recipientMlKemPub = recipientMlKem.publicKey,
            ).valueOrNull!!
            // Each message gets a unique ID — use a shared protocol but its NonceTracker must track all 1000
            val result = decrypt(p, envelope)
            assertTrue(result.isOk(), "Round-trip failed at iteration $i: ${result.errorOrNull}")
            assertEquals(text, result.valueOrNull!!.text, "Text mismatch at iteration $i")
        }
    }

    @Test
    fun allMessageTypesRoundTrip() {
        val p = protocol()
        MessageType.entries.forEach { type ->
            val payload = MessagePayload(text = "type-test", type = type)
            val envelope = encrypt(p, payload)
            val result = decrypt(p, envelope)
            assertTrue(result.isOk())
            assertEquals(type, result.valueOrNull!!.type)
        }
    }

    @Test
    fun replyToIdRoundTrips() {
        val p = protocol()
        val payload = MessagePayload(text = "reply", replyToId = "original-msg-id")
        val result = decrypt(p, encrypt(p, payload))
        assertEquals("original-msg-id", result.valueOrNull!!.replyToId)
    }

    // ── AC-M07-2: Hybrid KEM via M01 ────────────────────────────────────────

    @Test
    fun wrongRecipientX25519KeyFailsDecryption() {
        val p = protocol()
        val envelope = encrypt(p)
        val wrongX25519 = crypto.generateX25519KeyPair()
        val result = p.decrypt(
            envelopeBytes = envelope,
            recipientId = "bob",
            recipientX25519PrivKey = wrongX25519.privateKey,
            recipientMlKemPrivKey = recipientMlKem.privateKey,
            senderEd25519Pub = senderEd25519.publicKey,
        )
        assertTrue(result.isErr())
    }

    @Test
    fun wrongRecipientMlKemKeyFailsDecryption() {
        val p = protocol()
        val envelope = encrypt(p)
        val wrongMlKem = crypto.generateMlKem768KeyPair()
        val result = p.decrypt(
            envelopeBytes = envelope,
            recipientId = "bob",
            recipientX25519PrivKey = recipientX25519.privateKey,
            recipientMlKemPrivKey = wrongMlKem.privateKey,
            senderEd25519Pub = senderEd25519.publicKey,
        )
        assertTrue(result.isErr())
    }

    // ── AC-M07-3: Replay attack detection ────────────────────────────────────

    @Test
    fun replayedEnvelopeIsRejected() {
        val p = protocol()
        val envelope = encrypt(p)
        val first = decrypt(p, envelope)
        assertTrue(first.isOk(), "First decrypt should succeed")

        val replay = decrypt(p, envelope)
        assertTrue(replay.isErr(), "Replayed envelope must be rejected")
        assertEquals(ProtocolError.ReplayDetected, replay.errorOrNull)
    }

    @Test
    fun distinctEnvelopesAreNotFlaggedAsReplays() {
        val p = protocol()
        val e1 = encrypt(p, MessagePayload(text = "msg1"))
        val e2 = encrypt(p, MessagePayload(text = "msg2"))
        assertTrue(decrypt(p, e1).isOk())
        assertTrue(decrypt(p, e2).isOk())
    }

    // ── AC-M07-4: Tampering detection ────────────────────────────────────────

    @Test
    fun tamperedEncryptedPayloadIsRejected() {
        val p = protocol()
        val envelopeBytes = encrypt(p).copyOf()
        // Flip a byte somewhere in the middle of the serialized envelope
        val mid = envelopeBytes.size / 2
        envelopeBytes[mid] = envelopeBytes[mid].xor(0xFF.toByte())
        val result = decrypt(p, envelopeBytes)
        assertTrue(result.isErr(), "Tampered envelope must be rejected")
    }

    @Test
    fun truncatedEnvelopeIsRejected() {
        val p = protocol()
        val truncated = encrypt(p).copyOfRange(0, 10)
        val result = decrypt(p, truncated)
        assertTrue(result.isErr())
    }

    @Test
    fun emptyBytesAreRejected() {
        val result = decrypt(protocol(), ByteArray(0))
        assertTrue(result.isErr())
        assertEquals(ProtocolError.MalformedEnvelope, result.errorOrNull)
    }

    // ── AC-M07-5: Protocol version validation ────────────────────────────────

    @Test
    fun unknownProtocolVersionIsRejected() {
        val p = protocol()
        val original = ProtoBuf.decodeFromByteArray(Envelope.serializer(), encrypt(p))
        val modified = original.copy(protocolVersion = 999)
        val reencoded = ProtoBuf.encodeToByteArray(Envelope.serializer(), modified)
        val result = decrypt(p, reencoded)
        assertTrue(result.isErr())
        assertEquals(ProtocolError.UnknownVersion, result.errorOrNull)
    }

    @Test
    fun correctProtocolVersionIsAccepted() {
        val p = protocol()
        val envelope = encrypt(p)
        val decoded = ProtoBuf.decodeFromByteArray(Envelope.serializer(), envelope)
        assertEquals(PROTOCOL_VERSION, decoded.protocolVersion)
    }

    // ── AC-M07-6: Protobuf wire format ───────────────────────────────────────

    @Test
    fun serializedEnvelopeIsProtobufNotJson() {
        val p = protocol()
        val envelope = encrypt(p)
        // JSON always starts with '{' (0x7B). Protobuf field 1 varint tag = 0x08.
        assertFalse(envelope.isEmpty())
        assertNotEquals(0x7B.toByte(), envelope[0], "Envelope must not be JSON-encoded")
        // Verify it round-trips through protobuf decoder without error
        val decoded = ProtoBuf.decodeFromByteArray(Envelope.serializer(), envelope)
        assertEquals(PROTOCOL_VERSION, decoded.protocolVersion)
        assertEquals("alice", decoded.senderId)
        assertEquals("bob", decoded.recipientId)
    }

    @Test
    fun envelopeFieldsArePresent() {
        val p = protocol()
        val envelope = ProtoBuf.decodeFromByteArray(Envelope.serializer(), encrypt(p))
        assertEquals(32, envelope.ephemeralX25519Pub.size)
        assertEquals(64, envelope.signature.size)
        assertEquals(16, envelope.messageId.size)
        assertTrue(envelope.encryptedPayload.isNotEmpty())
        assertTrue(envelope.nonce.isNotEmpty())
        assertTrue(envelope.kemCiphertext.isNotEmpty())
    }

    // ── Sender mismatch / wrong public key ────────────────────────────────────

    @Test
    fun wrongSenderVerifyKeyIsRejected() {
        val p = protocol()
        val envelope = encrypt(p)
        val wrongEd25519 = crypto.generateEd25519KeyPair()
        val result = p.decrypt(
            envelopeBytes = envelope,
            recipientId = "bob",
            recipientX25519PrivKey = recipientX25519.privateKey,
            recipientMlKemPrivKey = recipientMlKem.privateKey,
            senderEd25519Pub = wrongEd25519.publicKey,
        )
        assertTrue(result.isErr())
        assertEquals(ProtocolError.InvalidSignature, result.errorOrNull)
    }

    @Test
    fun wrongRecipientIdIsRejected() {
        val p = protocol()
        val envelope = encrypt(p, recipientId = "bob")
        val result = decrypt(p, envelope, recipientId = "charlie")
        assertTrue(result.isErr())
        assertEquals(ProtocolError.MalformedEnvelope, result.errorOrNull)
    }
}

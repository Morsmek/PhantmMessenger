@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.protocol

import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.crypto.EncryptedData
import com.stagic.phantm.crypto.HybridKemCiphertext
import com.stagic.phantm.crypto.PrivateKey
import com.stagic.phantm.crypto.PublicKey
import com.stagic.phantm.crypto.SecretKey
import com.stagic.phantm.crypto.Signature
import com.stagic.phantm.err
import com.stagic.phantm.ok
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Encrypts and decrypts Phantm message envelopes.
 *
 * Encryption: hybrid KEM (X25519 + ML-KEM-768) → XSalsa20-Poly1305 → Ed25519 signature.
 * Decryption: version check → replay check → signature verify → KEM decap → secretbox open.
 */
interface MessageProtocol {

    /**
     * Encrypt [payload] for a recipient and return serialized [Envelope] bytes.
     *
     * @param senderEd25519PrivKey used to sign the envelope fields 1–7.
     * @param recipientX25519Pub   used for hybrid KEM encapsulation.
     * @param recipientMlKemPub    used for hybrid KEM encapsulation.
     */
    fun encrypt(
        payload: MessagePayload,
        senderId: String,
        recipientId: String,
        senderEd25519PrivKey: PrivateKey,
        recipientX25519Pub: PublicKey,
        recipientMlKemPub: PublicKey,
    ): ProtocolResult<ByteArray>

    /**
     * Decrypt and verify a serialized [Envelope].
     *
     * @param recipientId          must match the envelope's recipient_id or [ProtocolError.MalformedEnvelope] is returned.
     * @param recipientX25519PrivKey used for hybrid KEM decapsulation.
     * @param recipientMlKemPrivKey  used for hybrid KEM decapsulation.
     * @param senderEd25519Pub     used to verify the envelope signature.
     */
    fun decrypt(
        envelopeBytes: ByteArray,
        recipientId: String,
        recipientX25519PrivKey: PrivateKey,
        recipientMlKemPrivKey: PrivateKey,
        senderEd25519Pub: PublicKey,
    ): ProtocolResult<MessagePayload>
}

fun createMessageProtocol(
    crypto: CryptoCore,
    nonceTracker: NonceTracker = NonceTracker(),
): MessageProtocol = MessageProtocolImpl(crypto, nonceTracker)

// ── Implementation ────────────────────────────────────────────────────────────

private class MessageProtocolImpl(
    private val crypto: CryptoCore,
    private val nonceTracker: NonceTracker,
) : MessageProtocol {

    private val protoBuf = ProtoBuf

    override fun encrypt(
        payload: MessagePayload,
        senderId: String,
        recipientId: String,
        senderEd25519PrivKey: PrivateKey,
        recipientX25519Pub: PublicKey,
        recipientMlKemPub: PublicKey,
    ): ProtocolResult<ByteArray> {
        return try {
            val kemResult = crypto.hybridEncapsulate(recipientX25519Pub, recipientMlKemPub)
            val messageKey = SecretKey(kemResult.sharedSecret.bytes)

            val plaintextBytes = protoBuf.encodeToByteArray(MessagePayload.serializer(), payload)
            val encrypted = crypto.secretBox(plaintextBytes, messageKey)

            val messageId = crypto.randomBytes(16)

            val signingInput = EnvelopeSigningInput(
                protocolVersion = PROTOCOL_VERSION,
                senderId = senderId,
                recipientId = recipientId,
                ephemeralX25519Pub = kemResult.ciphertext.x25519Ciphertext,
                kemCiphertext = kemResult.ciphertext.mlKemCiphertext,
                encryptedPayload = encrypted.ciphertext,
                nonce = encrypted.nonce,
            )
            val signature = crypto.sign(
                protoBuf.encodeToByteArray(EnvelopeSigningInput.serializer(), signingInput),
                senderEd25519PrivKey,
            )

            val envelope = Envelope(
                protocolVersion = PROTOCOL_VERSION,
                senderId = senderId,
                recipientId = recipientId,
                ephemeralX25519Pub = kemResult.ciphertext.x25519Ciphertext,
                kemCiphertext = kemResult.ciphertext.mlKemCiphertext,
                encryptedPayload = encrypted.ciphertext,
                nonce = encrypted.nonce,
                signature = signature.bytes,
                timestampMs = currentTimeMs(),
                messageId = messageId,
            )

            protoBuf.encodeToByteArray(Envelope.serializer(), envelope).ok()
        } catch (e: Exception) {
            ProtocolError.Unknown(e).err()
        }
    }

    override fun decrypt(
        envelopeBytes: ByteArray,
        recipientId: String,
        recipientX25519PrivKey: PrivateKey,
        recipientMlKemPrivKey: PrivateKey,
        senderEd25519Pub: PublicKey,
    ): ProtocolResult<MessagePayload> {
        val envelope = try {
            protoBuf.decodeFromByteArray(Envelope.serializer(), envelopeBytes)
        } catch (e: Exception) {
            return ProtocolError.MalformedEnvelope.err()
        }

        if (envelope.protocolVersion != PROTOCOL_VERSION) {
            return ProtocolError.UnknownVersion.err()
        }

        if (envelope.recipientId != recipientId) {
            return ProtocolError.MalformedEnvelope.err()
        }

        if (!nonceTracker.checkAndRecord(envelope.messageId)) {
            return ProtocolError.ReplayDetected.err()
        }

        val signingInput = EnvelopeSigningInput(
            protocolVersion = envelope.protocolVersion,
            senderId = envelope.senderId,
            recipientId = envelope.recipientId,
            ephemeralX25519Pub = envelope.ephemeralX25519Pub,
            kemCiphertext = envelope.kemCiphertext,
            encryptedPayload = envelope.encryptedPayload,
            nonce = envelope.nonce,
        )
        val verifyResult = crypto.verify(
            protoBuf.encodeToByteArray(EnvelopeSigningInput.serializer(), signingInput),
            Signature(envelope.signature),
            senderEd25519Pub,
        )
        if (verifyResult.isErr()) return ProtocolError.InvalidSignature.err()

        val kemCiphertext = HybridKemCiphertext(
            x25519Ciphertext = envelope.ephemeralX25519Pub,
            mlKemCiphertext = envelope.kemCiphertext,
        )
        val sharedSecretResult = crypto.hybridDecapsulate(
            ciphertext = kemCiphertext,
            x25519PrivKey = recipientX25519PrivKey,
            mlKemPrivKey = recipientMlKemPrivKey,
        )
        if (sharedSecretResult.isErr()) return ProtocolError.DecryptionFailed.err()
        val messageKey = SecretKey(sharedSecretResult.valueOrNull!!.bytes)

        val decryptResult = crypto.secretBoxOpen(
            encrypted = EncryptedData(nonce = envelope.nonce, ciphertext = envelope.encryptedPayload),
            key = messageKey,
        )
        if (decryptResult.isErr()) return ProtocolError.DecryptionFailed.err()

        return try {
            protoBuf.decodeFromByteArray(
                MessagePayload.serializer(),
                decryptResult.valueOrNull!!,
            ).ok()
        } catch (e: Exception) {
            ProtocolError.MalformedEnvelope.err()
        }
    }
}

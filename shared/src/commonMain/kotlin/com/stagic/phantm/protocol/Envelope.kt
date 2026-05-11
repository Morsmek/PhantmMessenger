@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.protocol

import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.Serializable

internal const val PROTOCOL_VERSION = 1

/** Wire-format envelope. All fields beyond (8) signature are unauthenticated metadata. */
@Suppress("ArrayInDataClass")
@Serializable
internal data class Envelope(
    @ProtoNumber(1) val protocolVersion: Int,
    @ProtoNumber(2) val senderId: String,
    @ProtoNumber(3) val recipientId: String,
    @ProtoNumber(4) val ephemeralX25519Pub: ByteArray,   // 32 bytes
    @ProtoNumber(5) val kemCiphertext: ByteArray,         // ML-KEM-768 ciphertext
    @ProtoNumber(6) val encryptedPayload: ByteArray,      // XSalsa20-Poly1305 ciphertext
    @ProtoNumber(7) val nonce: ByteArray,                 // 24-byte secretbox nonce
    @ProtoNumber(8) val signature: ByteArray,             // Ed25519 over SigningInput(fields 1-7)
    @ProtoNumber(9) val timestampMs: Long,
    @ProtoNumber(10) val messageId: ByteArray,            // 16 bytes random
)

/** Canonical protobuf encoding signed by the sender's Ed25519 key. Covers envelope fields 1–7. */
@Suppress("ArrayInDataClass")
@Serializable
internal data class EnvelopeSigningInput(
    @ProtoNumber(1) val protocolVersion: Int,
    @ProtoNumber(2) val senderId: String,
    @ProtoNumber(3) val recipientId: String,
    @ProtoNumber(4) val ephemeralX25519Pub: ByteArray,
    @ProtoNumber(5) val kemCiphertext: ByteArray,
    @ProtoNumber(6) val encryptedPayload: ByteArray,
    @ProtoNumber(7) val nonce: ByteArray,
)

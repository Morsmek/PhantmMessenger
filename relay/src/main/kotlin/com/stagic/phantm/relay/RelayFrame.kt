@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.relay

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Binary protobuf frame exchanged over the WebSocket.
 * Byte-compatible with shared TransportFrame (same ProtoNumber assignments).
 *
 * Registration: [envelopeBytes] is empty; [recipientId] identifies the connecting client.
 * Envelope:     [recipientId] is the intended recipient; [envelopeBytes] is the M07 ciphertext.
 */
@Suppress("ArrayInDataClass")
@Serializable
internal data class RelayFrame(
    @ProtoNumber(1) val recipientId: String = "",
    @ProtoNumber(2) val envelopeBytes: ByteArray = ByteArray(0),
)

/** Sentinel recipient ID used by M06 cover traffic — relay discards these silently (AC-M09-5). */
const val COVER_RECIPIENT_ID = "__phantm_cover__"

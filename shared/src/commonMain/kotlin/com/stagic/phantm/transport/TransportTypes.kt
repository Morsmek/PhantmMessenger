@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.transport

import com.stagic.phantm.PhantmResult
import kotlinx.serialization.ProtoNumber
import kotlinx.serialization.Serializable

/**
 * An already-encrypted M07 envelope ready to be sent over the wire.
 * The [bytes] field is the serialized [com.stagic.phantm.protocol.Envelope] from M07 —
 * never plaintext.
 */
@Suppress("ArrayInDataClass")
data class TransportEnvelope(
    val recipientId: String,
    val bytes: ByteArray,
)

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }

sealed class TransportError {
    data object ConnectionRefused : TransportError()
    data object TlsHandshakeFailed : TransportError()
    data class Unknown(val cause: Throwable) : TransportError()
}

typealias TransportResult<T> = PhantmResult<T, TransportError>

/** Binary protobuf frame exchanged between client and relay over the WebSocket. */
@Suppress("ArrayInDataClass")
@Serializable
internal data class TransportFrame(
    @ProtoNumber(1) val recipientId: String = "",
    @ProtoNumber(2) val envelopeBytes: ByteArray = ByteArray(0),
)

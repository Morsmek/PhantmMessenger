package com.stagic.phantm.protocol

import kotlinx.serialization.ProtoNumber
import kotlinx.serialization.Serializable

/** Plaintext message content. Serialized with protobuf and then encrypted inside [Envelope]. */
@Serializable
data class MessagePayload(
    @ProtoNumber(1) val text: String = "",
    @ProtoNumber(2) val attachments: List<Attachment> = emptyList(),
    @ProtoNumber(3) val replyToId: String = "",
    @ProtoNumber(4) val type: MessageType = MessageType.TEXT,
)

@Suppress("ArrayInDataClass")
@Serializable
data class Attachment(
    @ProtoNumber(1) val mimeType: String = "",
    @ProtoNumber(2) val encryptedBytes: ByteArray = ByteArray(0),
    @ProtoNumber(3) val filename: String = "",
)

@Serializable
enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    REACTION,
    DELETION,
}

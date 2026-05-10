package com.stagic.phantm.db

import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.PublicKey

data class LocalMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    /** Still-encrypted ciphertext blob; decrypted only by M07 Message Protocol. */
    val encryptedPayload: ByteArray,
    val timestampMs: Long,
    val deliveryStatus: DeliveryStatus,
    val localOnly: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalMessage) return false
        return id == other.id &&
            conversationId == other.conversationId &&
            senderId == other.senderId &&
            encryptedPayload.contentEquals(other.encryptedPayload) &&
            timestampMs == other.timestampMs &&
            deliveryStatus == other.deliveryStatus &&
            localOnly == other.localOnly
    }

    override fun hashCode(): Int = id.hashCode()
}

data class LocalContact(
    val id: String,
    val displayName: String,
    val x25519PublicKey: PublicKey,
    val mlKemPublicKey: PublicKey,
    val ed25519PublicKey: PublicKey,
    val fingerprint: String,
    val verified: Boolean,
    val createdAtMs: Long,
)

data class LocalSession(
    val id: String,
    val contactId: String,
    val ephemeralPublicKey: PublicKey,
    /** Ratchet chain key — sensitive; never log this field. */
    val chainKey: ByteArray,
    val messageIndex: Long,
    val createdAtMs: Long,
) {
    override fun toString(): String =
        "LocalSession(id=$id, contactId=$contactId, messageIndex=$messageIndex, chainKey=[redacted])"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalSession) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class DeliveryStatus { PENDING, SENT, DELIVERED, READ, FAILED }

sealed class DbError {
    data object NotFound : DbError()
    data object ConflictingId : DbError()
    data class Unknown(val cause: Throwable) : DbError()
}

typealias DbResult<T> = PhantmResult<T, DbError>

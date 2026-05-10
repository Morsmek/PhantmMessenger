package com.stagic.phantm.groups

/** Group metadata stored in the local DB. */
data class LocalGroup(
    val id: String,
    val name: String,
    val keyVersion: Long,
    /** Symmetric key for group message encryption — sensitive; never log this field. */
    val groupKey: ByteArray,
    val createdAtMs: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalGroup) return false
        return id == other.id && keyVersion == other.keyVersion
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "LocalGroup(id=$id, name=$name, keyVersion=$keyVersion, groupKey=[redacted])"
}

/**
 * A key distribution envelope addressed to one group member.
 * The bytes are a serialised M07 [MessageProtocol] envelope.
 * The caller is responsible for routing it to [recipientId].
 */
data class GroupKeyEnvelope(
    val recipientId: String,
    val envelopeBytes: ByteArray,
)

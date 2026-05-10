package com.stagic.phantm.groups

import com.stagic.phantm.crypto.PrivateKey
import com.stagic.phantm.crypto.PublicKey
import com.stagic.phantm.db.LocalContact
import com.stagic.phantm.protocol.MessagePayload

/**
 * Manages encrypted group messaging with forward-secure key rotation.
 *
 * Group keys are symmetric (32-byte XSalsa20 keys). Key distribution uses
 * M07 [com.stagic.phantm.protocol.MessageProtocol] for per-member delivery.
 * Key rotation (AC-M08-2, AC-M08-3) is triggered on every membership change.
 */
interface GroupManager {

    /**
     * Create a new group, persist it in the DB, and produce key distribution
     * envelopes for each member in [members] (AC-M08-1).
     * The creator's own key is stored but no envelope is produced for them.
     */
    suspend fun createGroup(
        groupId: String,
        name: String,
        creatorId: String,
        creatorEd25519PrivKey: PrivateKey,
        members: List<LocalContact>,
    ): GroupResult<List<GroupKeyEnvelope>>

    /**
     * Receive and process a key distribution envelope sent via M07.
     * Stores or updates the group key in the DB.
     */
    suspend fun receiveGroupKeyEnvelope(
        envelopeBytes: ByteArray,
        recipientId: String,
        recipientX25519PrivKey: PrivateKey,
        recipientMlKemPrivKey: PrivateKey,
        senderEd25519Pub: PublicKey,
    ): GroupResult<Unit>

    /**
     * Add a new member to a group, rotate the group key, and return new key
     * distribution envelopes for ALL current members (AC-M08-2).
     */
    suspend fun addMember(
        groupId: String,
        newMember: LocalContact,
        senderId: String,
        senderEd25519PrivKey: PrivateKey,
        allMembers: List<LocalContact>,
    ): GroupResult<List<GroupKeyEnvelope>>

    /**
     * Remove a member from a group, rotate the group key, and return new key
     * distribution envelopes for the remaining members only (AC-M08-3).
     * The removed member's old key cannot decrypt messages encrypted with the new key.
     */
    suspend fun removeMember(
        groupId: String,
        memberIdToRemove: String,
        senderId: String,
        senderEd25519PrivKey: PrivateKey,
        remainingMembers: List<LocalContact>,
    ): GroupResult<List<GroupKeyEnvelope>>

    /** Encrypt [payload] with the stored group key for [groupId]. Returns nonce + ciphertext. */
    suspend fun encryptGroupMessage(
        groupId: String,
        payload: MessagePayload,
    ): GroupResult<ByteArray>

    /** Decrypt bytes previously produced by [encryptGroupMessage] for the same [groupId]. */
    suspend fun decryptGroupMessage(
        groupId: String,
        encryptedBytes: ByteArray,
    ): GroupResult<MessagePayload>
}

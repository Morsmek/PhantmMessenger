@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.groups

import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.crypto.EncryptedData
import com.stagic.phantm.crypto.PrivateKey
import com.stagic.phantm.crypto.PublicKey
import com.stagic.phantm.crypto.SecretKey
import com.stagic.phantm.db.LocalContact
import com.stagic.phantm.err
import com.stagic.phantm.ok
import com.stagic.phantm.protocol.Attachment
import com.stagic.phantm.protocol.MessagePayload
import com.stagic.phantm.protocol.MessageProtocol
import com.stagic.phantm.protocol.MessageType
import com.stagic.phantm.protocol.currentTimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.protobuf.ProtoBuf

internal class GroupManagerImpl(
    private val crypto: CryptoCore,
    private val messageProtocol: MessageProtocol,
    internal val groupDao: GroupDao,
) : GroupManager {

    override suspend fun createGroup(
        groupId: String,
        name: String,
        creatorId: String,
        creatorEd25519PrivKey: PrivateKey,
        members: List<LocalContact>,
    ): GroupResult<List<GroupKeyEnvelope>> = withContext(Dispatchers.Default) {
        runCatching {
            val groupKey = crypto.randomBytes(GROUP_KEY_BYTES)
            val nowMs = currentTimeMs()

            groupDao.insertGroup(LocalGroup(id = groupId, name = name, keyVersion = 1L, groupKey = groupKey, createdAtMs = nowMs))
            groupDao.insertMember(groupId, creatorId, nowMs)
            members.forEach { groupDao.insertMember(groupId, it.id, nowMs) }

            buildKeyEnvelopes(
                groupId = groupId,
                groupName = name,
                groupKey = groupKey,
                keyVersion = 1L,
                senderId = creatorId,
                senderEd25519PrivKey = creatorEd25519PrivKey,
                recipients = members,
            )
        }.fold(onSuccess = { it.ok() }, onFailure = { GroupError.Unknown(it).err() })
    }

    override suspend fun receiveGroupKeyEnvelope(
        envelopeBytes: ByteArray,
        recipientId: String,
        recipientX25519PrivKey: PrivateKey,
        recipientMlKemPrivKey: PrivateKey,
        senderEd25519Pub: PublicKey,
    ): GroupResult<Unit> = withContext(Dispatchers.Default) {
        val payload = messageProtocol.decrypt(
            envelopeBytes = envelopeBytes,
            recipientId = recipientId,
            recipientX25519PrivKey = recipientX25519PrivKey,
            recipientMlKemPrivKey = recipientMlKemPrivKey,
            senderEd25519Pub = senderEd25519Pub,
        ).valueOrNull ?: return@withContext GroupError.DecryptionFailed.err()

        val attachment = payload.attachments.firstOrNull { it.mimeType == GROUP_KEY_MIME_TYPE }
            ?: return@withContext GroupError.DecryptionFailed.err()

        val groupId = payload.text
        val groupName = attachment.filename
        val keyVersion = payload.replyToId.toLongOrNull() ?: 1L
        val groupKey = attachment.encryptedBytes

        val existing = groupDao.getGroupById(groupId)
        if (existing.isOk()) {
            groupDao.updateGroupKey(groupId, groupKey)
        } else {
            val nowMs = currentTimeMs()
            groupDao.insertGroup(LocalGroup(groupId, groupName, keyVersion, groupKey, nowMs))
        }
        groupDao.insertMember(groupId, recipientId, currentTimeMs())
        Unit.ok()
    }

    override suspend fun addMember(
        groupId: String,
        newMember: LocalContact,
        senderId: String,
        senderEd25519PrivKey: PrivateKey,
        allMembers: List<LocalContact>,
    ): GroupResult<List<GroupKeyEnvelope>> = withContext(Dispatchers.Default) {
        runCatching {
            val group = groupDao.getGroupById(groupId).valueOrNull
                ?: return@withContext GroupError.GroupNotFound.err()

            val newKey = crypto.randomBytes(GROUP_KEY_BYTES)
            groupDao.insertMember(groupId, newMember.id, currentTimeMs())
            groupDao.updateGroupKey(groupId, newKey)
            val newVersion = group.keyVersion + 1

            buildKeyEnvelopes(
                groupId = groupId,
                groupName = group.name,
                groupKey = newKey,
                keyVersion = newVersion,
                senderId = senderId,
                senderEd25519PrivKey = senderEd25519PrivKey,
                recipients = allMembers + newMember,
            )
        }.fold(onSuccess = { it.ok() }, onFailure = { GroupError.Unknown(it).err() })
    }

    override suspend fun removeMember(
        groupId: String,
        memberIdToRemove: String,
        senderId: String,
        senderEd25519PrivKey: PrivateKey,
        remainingMembers: List<LocalContact>,
    ): GroupResult<List<GroupKeyEnvelope>> = withContext(Dispatchers.Default) {
        runCatching {
            val group = groupDao.getGroupById(groupId).valueOrNull
                ?: return@withContext GroupError.GroupNotFound.err()

            groupDao.removeMember(groupId, memberIdToRemove)
            val newKey = crypto.randomBytes(GROUP_KEY_BYTES)
            groupDao.updateGroupKey(groupId, newKey)
            val newVersion = group.keyVersion + 1

            buildKeyEnvelopes(
                groupId = groupId,
                groupName = group.name,
                groupKey = newKey,
                keyVersion = newVersion,
                senderId = senderId,
                senderEd25519PrivKey = senderEd25519PrivKey,
                recipients = remainingMembers,
            )
        }.fold(onSuccess = { it.ok() }, onFailure = { GroupError.Unknown(it).err() })
    }

    override suspend fun encryptGroupMessage(
        groupId: String,
        payload: MessagePayload,
    ): GroupResult<ByteArray> = withContext(Dispatchers.Default) {
        val group = groupDao.getGroupById(groupId).valueOrNull
            ?: return@withContext GroupError.GroupNotFound.err()
        runCatching {
            val bytes = ProtoBuf.encodeToByteArray(MessagePayload.serializer(), payload)
            val encrypted = crypto.secretBox(bytes, SecretKey(group.groupKey))
            encrypted.nonce + encrypted.ciphertext
        }.fold(onSuccess = { it.ok() }, onFailure = { GroupError.EncryptionFailed(it.message ?: "Unknown").err() })
    }

    override suspend fun decryptGroupMessage(
        groupId: String,
        encryptedBytes: ByteArray,
    ): GroupResult<MessagePayload> = withContext(Dispatchers.Default) {
        val group = groupDao.getGroupById(groupId).valueOrNull
            ?: return@withContext GroupError.GroupNotFound.err()
        runCatching {
            val nonce = encryptedBytes.copyOf(NONCE_BYTES)
            val ciphertext = encryptedBytes.copyOfRange(NONCE_BYTES, encryptedBytes.size)
            val decrypted = crypto.secretBoxOpen(EncryptedData(nonce, ciphertext), SecretKey(group.groupKey))
                .valueOrNull ?: return@withContext GroupError.DecryptionFailed.err()
            ProtoBuf.decodeFromByteArray(MessagePayload.serializer(), decrypted)
        }.fold(onSuccess = { it.ok() }, onFailure = { GroupError.DecryptionFailed.err() })
    }

    private fun buildKeyEnvelopes(
        groupId: String,
        groupName: String,
        groupKey: ByteArray,
        keyVersion: Long,
        senderId: String,
        senderEd25519PrivKey: PrivateKey,
        recipients: List<LocalContact>,
    ): List<GroupKeyEnvelope> = recipients.mapNotNull { member ->
        val keyPayload = MessagePayload(
            text = groupId,
            replyToId = keyVersion.toString(),
            attachments = listOf(
                Attachment(
                    mimeType = GROUP_KEY_MIME_TYPE,
                    encryptedBytes = groupKey,
                    filename = groupName,
                ),
            ),
            type = MessageType.FILE,
        )
        messageProtocol.encrypt(
            payload = keyPayload,
            senderId = senderId,
            recipientId = member.id,
            senderEd25519PrivKey = senderEd25519PrivKey,
            recipientX25519Pub = member.x25519PublicKey,
            recipientMlKemPub = member.mlKemPublicKey,
        ).valueOrNull?.let { envelopeBytes ->
            GroupKeyEnvelope(recipientId = member.id, envelopeBytes = envelopeBytes)
        }
    }

    private companion object {
        const val GROUP_KEY_BYTES = 32
        const val NONCE_BYTES = 24
        const val GROUP_KEY_MIME_TYPE = "application/x-phantm-groupkey"
    }
}

fun createGroupManager(
    crypto: CryptoCore,
    messageProtocol: MessageProtocol,
    groupDao: GroupDao,
): GroupManager = GroupManagerImpl(crypto, messageProtocol, groupDao)

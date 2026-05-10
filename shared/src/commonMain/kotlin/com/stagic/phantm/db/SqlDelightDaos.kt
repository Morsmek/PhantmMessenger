package com.stagic.phantm.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.stagic.phantm.crypto.PublicKey
import com.stagic.phantm.err
import com.stagic.phantm.groups.GroupDao
import com.stagic.phantm.groups.LocalGroup
import com.stagic.phantm.ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// ── MessageDao ───────────────────────────────────────────────────────────────

internal class SqlDelightMessageDao(private val db: PhantmDatabase) : MessageDao {

    override suspend fun insert(message: LocalMessage): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                db.messagesQueries.insert(
                    id = message.id,
                    conversation_id = message.conversationId,
                    sender_id = message.senderId,
                    encrypted_payload = message.encryptedPayload,
                    timestamp_ms = message.timestampMs,
                    delivery_status = message.deliveryStatus.name,
                    local_only = if (message.localOnly) 1L else 0L,
                )
            }.fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun getById(id: String): DbResult<LocalMessage> =
        withContext(Dispatchers.Default) {
            val row = db.messagesQueries.selectById(id).executeAsOneOrNull()
                ?: return@withContext DbError.NotFound.err()
            row.toLocalMessage().ok()
        }

    override fun getByConversation(conversationId: String): Flow<List<LocalMessage>> =
        db.messagesQueries.selectByConversation(conversationId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toLocalMessage() } }

    override suspend fun updateDeliveryStatus(id: String, status: DeliveryStatus): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                db.messagesQueries.updateDeliveryStatus(
                    delivery_status = status.name,
                    id = id,
                )
            }.fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun delete(id: String): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching { db.messagesQueries.deleteById(id) }
                .fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun deleteAllForConversation(conversationId: String): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching { db.messagesQueries.deleteByConversation(conversationId) }
                .fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }
}

// ── ContactDao ───────────────────────────────────────────────────────────────

internal class SqlDelightContactDao(private val db: PhantmDatabase) : ContactDao {

    override suspend fun insert(contact: LocalContact): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                db.contactsQueries.insert(
                    id = contact.id,
                    display_name = contact.displayName,
                    x25519_public_key = contact.x25519PublicKey.bytes,
                    ml_kem_public_key = contact.mlKemPublicKey.bytes,
                    ed25519_public_key = contact.ed25519PublicKey.bytes,
                    fingerprint = contact.fingerprint,
                    verified = if (contact.verified) 1L else 0L,
                    created_at_ms = contact.createdAtMs,
                )
            }.fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun getById(id: String): DbResult<LocalContact> =
        withContext(Dispatchers.Default) {
            val row = db.contactsQueries.selectById(id).executeAsOneOrNull()
                ?: return@withContext DbError.NotFound.err()
            row.toLocalContact().ok()
        }

    override fun getAll(): Flow<List<LocalContact>> =
        db.contactsQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toLocalContact() } }

    override suspend fun update(contact: LocalContact): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                db.contactsQueries.update(
                    display_name = contact.displayName,
                    x25519_public_key = contact.x25519PublicKey.bytes,
                    ml_kem_public_key = contact.mlKemPublicKey.bytes,
                    ed25519_public_key = contact.ed25519PublicKey.bytes,
                    fingerprint = contact.fingerprint,
                    verified = if (contact.verified) 1L else 0L,
                    id = contact.id,
                )
            }.fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun delete(id: String): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching { db.contactsQueries.deleteById(id) }
                .fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }
}

// ── SessionDao ───────────────────────────────────────────────────────────────

internal class SqlDelightSessionDao(private val db: PhantmDatabase) : SessionDao {

    override suspend fun insert(session: LocalSession): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                db.sessionsQueries.insert(
                    id = session.id,
                    contact_id = session.contactId,
                    ephemeral_public_key = session.ephemeralPublicKey.bytes,
                    chain_key = session.chainKey,
                    message_index = session.messageIndex,
                    created_at_ms = session.createdAtMs,
                )
            }.fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun getById(id: String): DbResult<LocalSession> =
        withContext(Dispatchers.Default) {
            val row = db.sessionsQueries.selectById(id).executeAsOneOrNull()
                ?: return@withContext DbError.NotFound.err()
            row.toLocalSession().ok()
        }

    override suspend fun getByContact(contactId: String): DbResult<List<LocalSession>> =
        withContext(Dispatchers.Default) {
            runCatching {
                db.sessionsQueries.selectByContact(contactId)
                    .executeAsList()
                    .map { it.toLocalSession() }
            }.fold(onSuccess = { it.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun updateChainKey(id: String, chainKey: ByteArray, messageIndex: Long): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                db.sessionsQueries.updateChainKey(
                    chain_key = chainKey,
                    message_index = messageIndex,
                    id = id,
                )
            }.fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun delete(id: String): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching { db.sessionsQueries.deleteById(id) }
                .fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun deleteByContact(contactId: String): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching { db.sessionsQueries.deleteByContact(contactId) }
                .fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }
}

// ── Row mappers ───────────────────────────────────────────────────────────────

private fun Messages.toLocalMessage() = LocalMessage(
    id = id,
    conversationId = conversation_id,
    senderId = sender_id,
    encryptedPayload = encrypted_payload,
    timestampMs = timestamp_ms,
    deliveryStatus = DeliveryStatus.valueOf(delivery_status),
    localOnly = local_only != 0L,
)

private fun Contacts.toLocalContact() = LocalContact(
    id = id,
    displayName = display_name,
    x25519PublicKey = PublicKey(x25519_public_key),
    mlKemPublicKey = PublicKey(ml_kem_public_key),
    ed25519PublicKey = PublicKey(ed25519_public_key),
    fingerprint = fingerprint,
    verified = verified != 0L,
    createdAtMs = created_at_ms,
)

private fun Sessions.toLocalSession() = LocalSession(
    id = id,
    contactId = contact_id,
    ephemeralPublicKey = PublicKey(ephemeral_public_key),
    chainKey = chain_key,
    messageIndex = message_index,
    createdAtMs = created_at_ms,
)

// ── GroupDao ──────────────────────────────────────────────────────────────────

internal class SqlDelightGroupDao(private val db: PhantmDatabase) : GroupDao {

    override suspend fun insertGroup(group: LocalGroup): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                db.groupsQueries.insertGroup(
                    id = group.id,
                    name = group.name,
                    key_version = group.keyVersion,
                    group_key = group.groupKey,
                    created_at_ms = group.createdAtMs,
                )
            }.fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun getGroupById(id: String): DbResult<LocalGroup> =
        withContext(Dispatchers.Default) {
            val row = db.groupsQueries.selectGroupById(id).executeAsOneOrNull()
                ?: return@withContext DbError.NotFound.err()
            row.toLocalGroup().ok()
        }

    override suspend fun updateGroupKey(groupId: String, newKey: ByteArray): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                db.groupsQueries.updateGroupKey(group_key = newKey, id = groupId)
            }.fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun deleteGroup(id: String): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching { db.groupsQueries.deleteGroup(id) }
                .fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun insertMember(groupId: String, memberId: String, joinedAtMs: Long): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                db.groupsQueries.insertMember(group_id = groupId, member_id = memberId, joined_at_ms = joinedAtMs)
            }.fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun getMemberIds(groupId: String): DbResult<List<String>> =
        withContext(Dispatchers.Default) {
            runCatching {
                db.groupsQueries.selectMembersByGroup(groupId).executeAsList()
            }.fold(onSuccess = { it.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun removeMember(groupId: String, memberId: String): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching { db.groupsQueries.deleteMember(group_id = groupId, member_id = memberId) }
                .fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }

    override suspend fun removeAllMembers(groupId: String): DbResult<Unit> =
        withContext(Dispatchers.Default) {
            runCatching { db.groupsQueries.deleteAllMembers(groupId) }
                .fold(onSuccess = { Unit.ok() }, onFailure = { DbError.Unknown(it).err() })
        }
}

private fun Groups.toLocalGroup() = LocalGroup(
    id = id,
    name = name,
    keyVersion = key_version,
    groupKey = group_key,
    createdAtMs = created_at_ms,
)

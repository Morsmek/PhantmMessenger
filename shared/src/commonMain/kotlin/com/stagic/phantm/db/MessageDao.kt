package com.stagic.phantm.db

import kotlinx.coroutines.flow.Flow

/**
 * Data access object for encrypted message storage.
 * The [LocalMessage.encryptedPayload] is stored as-is — decryption is M07's responsibility.
 */
interface MessageDao {
    suspend fun insert(message: LocalMessage): DbResult<Unit>
    suspend fun getById(id: String): DbResult<LocalMessage>
    /** Emits a new list whenever any message in the conversation changes. */
    fun getByConversation(conversationId: String): Flow<List<LocalMessage>>
    suspend fun updateDeliveryStatus(id: String, status: DeliveryStatus): DbResult<Unit>
    suspend fun delete(id: String): DbResult<Unit>
    suspend fun deleteAllForConversation(conversationId: String): DbResult<Unit>
}

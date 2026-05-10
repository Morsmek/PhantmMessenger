package com.stagic.phantm.db

/** Data access object for double-ratchet session state (used by M07). */
interface SessionDao {
    suspend fun insert(session: LocalSession): DbResult<Unit>
    suspend fun getById(id: String): DbResult<LocalSession>
    suspend fun getByContact(contactId: String): DbResult<List<LocalSession>>
    suspend fun updateChainKey(id: String, chainKey: ByteArray, messageIndex: Long): DbResult<Unit>
    suspend fun delete(id: String): DbResult<Unit>
    suspend fun deleteByContact(contactId: String): DbResult<Unit>
}

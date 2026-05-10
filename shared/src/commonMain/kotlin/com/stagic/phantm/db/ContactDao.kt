package com.stagic.phantm.db

import kotlinx.coroutines.flow.Flow

/** Data access object for contact book operations. */
interface ContactDao {
    suspend fun insert(contact: LocalContact): DbResult<Unit>
    suspend fun getById(id: String): DbResult<LocalContact>
    /** Emits a new list whenever the contact book changes. */
    fun getAll(): Flow<List<LocalContact>>
    suspend fun update(contact: LocalContact): DbResult<Unit>
    suspend fun delete(id: String): DbResult<Unit>
}

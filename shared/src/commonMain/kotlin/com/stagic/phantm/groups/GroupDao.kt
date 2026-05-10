package com.stagic.phantm.groups

import com.stagic.phantm.db.DbResult

/** Persists group metadata and membership records (AC-M08-5). */
interface GroupDao {
    suspend fun insertGroup(group: LocalGroup): DbResult<Unit>
    suspend fun getGroupById(id: String): DbResult<LocalGroup>
    suspend fun updateGroupKey(groupId: String, newKey: ByteArray): DbResult<Unit>
    suspend fun deleteGroup(id: String): DbResult<Unit>
    suspend fun insertMember(groupId: String, memberId: String, joinedAtMs: Long): DbResult<Unit>
    suspend fun getMemberIds(groupId: String): DbResult<List<String>>
    suspend fun removeMember(groupId: String, memberId: String): DbResult<Unit>
    suspend fun removeAllMembers(groupId: String): DbResult<Unit>
}

package com.stagic.phantm.crdt

/**
 * Last-write-wins register. Merge picks the higher timestamp; nodeId breaks ties
 * deterministically so all nodes converge to the same value.
 */
data class LwwRegister<T>(
    val value: T,
    val timestampMs: Long,
    val nodeId: String,
) {
    fun set(value: T, timestampMs: Long, nodeId: String): LwwRegister<T> =
        LwwRegister(value, timestampMs, nodeId)

    fun merge(other: LwwRegister<T>): LwwRegister<T> = when {
        other.timestampMs > timestampMs -> other
        other.timestampMs < timestampMs -> this
        other.nodeId > nodeId -> other
        else -> this
    }
}

package com.stagic.phantm.crdt

/** Grow-only counter. Each node tracks its own count; value is the sum across all nodes. */
data class GCounter(val counts: Map<String, Long> = emptyMap()) {

    fun increment(nodeId: String, by: Long = 1L): GCounter {
        require(by > 0) { "Increment amount must be positive" }
        return GCounter(counts + (nodeId to (counts[nodeId] ?: 0L) + by))
    }

    fun value(): Long = counts.values.fold(0L, Long::plus)

    fun merge(other: GCounter): GCounter {
        val allKeys = counts.keys + other.counts.keys
        return GCounter(allKeys.associateWith { key ->
            maxOf(counts[key] ?: 0L, other.counts[key] ?: 0L)
        })
    }
}

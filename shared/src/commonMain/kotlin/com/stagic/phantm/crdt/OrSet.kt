package com.stagic.phantm.crdt

/**
 * Observed-Remove Set. Each element is associated with a set of unique add-tags.
 * Remove clears locally known tags; concurrent adds with unseen tags survive through merge.
 * Merge unions the tag sets per element — add wins over concurrent remove.
 */
data class OrSet<T>(val entries: Map<T, Set<String>> = emptyMap()) {

    fun add(element: T, tag: String): OrSet<T> {
        val existing = entries[element] ?: emptySet()
        return OrSet(entries + (element to existing + tag))
    }

    /** Removes all currently observed tags for [element]. Concurrent unseen adds will survive merge. */
    fun remove(element: T): OrSet<T> {
        if (!entries.containsKey(element)) return this
        return OrSet(entries + (element to emptySet()))
    }

    fun contains(element: T): Boolean = (entries[element]?.isNotEmpty()) == true

    fun toSet(): Set<T> = entries.filter { it.value.isNotEmpty() }.keys.toSet()

    fun merge(other: OrSet<T>): OrSet<T> {
        val allKeys = entries.keys + other.entries.keys
        return OrSet(allKeys.associateWith { key ->
            (entries[key] ?: emptySet()) + (other.entries[key] ?: emptySet())
        })
    }
}

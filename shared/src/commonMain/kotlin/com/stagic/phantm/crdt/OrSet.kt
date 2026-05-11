package com.stagic.phantm.crdt

/**
 * Observed-Remove Set (OR-Set) with proper tombstone tracking.
 *
 * Each element has two tag sets: [addTags] (all ever-added tags) and [removeTags] (tombstones).
 * An element is present iff (addTags - removeTags) is non-empty.
 *
 * Semantics:
 * - Concurrent add wins over concurrent remove (add-tag not yet seen by remover survives).
 * - Observed remove is effective: tags seen by the removing replica go into tombstones and are
 *   cancelled out even after merge.
 */
data class OrSet<T>(
    val addTags: Map<T, Set<String>> = emptyMap(),
    val removeTags: Map<T, Set<String>> = emptyMap(),
) {

    fun add(element: T, tag: String): OrSet<T> {
        val existing = addTags[element] ?: emptySet()
        return OrSet(addTags + (element to existing + tag), removeTags)
    }

    /** Tombstones all currently observed add-tags for [element]. Unseen concurrent adds survive. */
    fun remove(element: T): OrSet<T> {
        val observed = addTags[element] ?: return this
        if (observed.isEmpty()) return this
        val existing = removeTags[element] ?: emptySet()
        return OrSet(addTags, removeTags + (element to existing + observed))
    }

    fun contains(element: T): Boolean {
        val added = addTags[element] ?: return false
        val removed = removeTags[element] ?: emptySet()
        return (added - removed).isNotEmpty()
    }

    fun toSet(): Set<T> = (addTags.keys + removeTags.keys).filter { contains(it) }.toSet()

    fun merge(other: OrSet<T>): OrSet<T> {
        val allKeys = addTags.keys + other.addTags.keys + removeTags.keys + other.removeTags.keys
        val newAdd = allKeys.associateWith { key ->
            (addTags[key] ?: emptySet()) + (other.addTags[key] ?: emptySet())
        }.filterValues { it.isNotEmpty() }
        val newRemove = allKeys.associateWith { key ->
            (removeTags[key] ?: emptySet()) + (other.removeTags[key] ?: emptySet())
        }.filterValues { it.isNotEmpty() }
        return OrSet(newAdd, newRemove)
    }
}

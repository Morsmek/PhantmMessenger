package com.stagic.phantm.relay

import java.util.concurrent.ConcurrentHashMap

/**
 * TTL-bounded in-memory store for envelopes destined for offline recipients (AC-M09-3).
 *
 * Envelopes older than [ttlMs] are evicted on the next [pop] call. Thread-safe.
 * In a production deployment this would be backed by a persistent SQLite store;
 * in-memory suffices for the relay's JVM test environment.
 */
class EnvelopeStore(private val ttlMs: Long = DEFAULT_TTL_MS) {

    private data class Stored(val frameBytes: ByteArray, val storedAtMs: Long)

    private val store = ConcurrentHashMap<String, ArrayDeque<Stored>>()

    /** Queue [frameBytes] for [recipientId]. Called when the recipient is offline. */
    fun put(recipientId: String, frameBytes: ByteArray) {
        store.getOrPut(recipientId) { ArrayDeque() }
            .addLast(Stored(frameBytes, System.currentTimeMillis()))
    }

    /**
     * Drain and return all non-expired envelopes for [recipientId].
     * Clears the queue and removes the recipient's entry from the store.
     */
    fun pop(recipientId: String): List<ByteArray> {
        val queue = store.remove(recipientId) ?: return emptyList()
        val cutoff = System.currentTimeMillis() - ttlMs
        return queue.filter { it.storedAtMs >= cutoff }.map { it.frameBytes }
    }

    /** Number of queued envelopes for [recipientId] (including possibly expired ones). */
    fun size(recipientId: String): Int = store[recipientId]?.size ?: 0

    private companion object {
        const val DEFAULT_TTL_MS = 7L * 24 * 60 * 60 * 1_000
    }
}

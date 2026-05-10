package com.stagic.phantm.protocol

/**
 * In-memory replay-attack detector. Tracks seen message IDs by their byte content.
 * Evicts the oldest entry when [maxSize] is reached to bound memory growth.
 * Production deployments should persist seen IDs to survive restarts.
 */
class NonceTracker(private val maxSize: Int = 100_000) {

    private val seen = LinkedHashSet<List<Byte>>()

    /**
     * Returns `true` if [messageId] has NOT been seen before and records it.
     * Returns `false` (replay detected) if [messageId] was already recorded.
     */
    fun checkAndRecord(messageId: ByteArray): Boolean {
        val key = messageId.toList()
        if (key in seen) return false
        if (seen.size >= maxSize) seen.iterator().apply { next(); remove() }
        seen.add(key)
        return true
    }
}

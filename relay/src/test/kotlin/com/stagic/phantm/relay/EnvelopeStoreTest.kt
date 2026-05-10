package com.stagic.phantm.relay

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvelopeStoreTest {

    @Test
    fun put_then_pop_returnsStoredBytes() {
        val store = EnvelopeStore()
        val payload = byteArrayOf(1, 2, 3, 4)
        store.put("bob", payload)
        val result = store.pop("bob")
        assertEquals(1, result.size)
        assertContentEquals(payload, result[0])
    }

    @Test
    fun pop_clearsQueue() {
        val store = EnvelopeStore()
        store.put("bob", byteArrayOf(1))
        store.pop("bob")
        assertEquals(0, store.size("bob"))
        assertTrue(store.pop("bob").isEmpty())
    }

    @Test
    fun pop_unknownRecipient_returnsEmpty() {
        assertTrue(EnvelopeStore().pop("nobody").isEmpty())
    }

    @Test
    fun multipleEnvelopes_returnedInOrder() {
        val store = EnvelopeStore()
        store.put("bob", byteArrayOf(1))
        store.put("bob", byteArrayOf(2))
        store.put("bob", byteArrayOf(3))
        val result = store.pop("bob")
        assertEquals(3, result.size)
        assertContentEquals(byteArrayOf(1), result[0])
        assertContentEquals(byteArrayOf(2), result[1])
        assertContentEquals(byteArrayOf(3), result[2])
    }

    @Test
    fun expiredEnvelopes_evictedOnPop() {
        val store = EnvelopeStore(ttlMs = 30)
        store.put("bob", byteArrayOf(1, 2, 3))
        Thread.sleep(60)
        val result = store.pop("bob")
        assertTrue(result.isEmpty(), "Envelopes past TTL must be evicted on pop")
    }

    @Test
    fun freshEnvelopes_notEvicted() {
        val store = EnvelopeStore(ttlMs = 60_000)
        store.put("bob", byteArrayOf(1, 2, 3))
        val result = store.pop("bob")
        assertEquals(1, result.size)
    }

    @Test
    fun size_reflectsQueueDepth() {
        val store = EnvelopeStore()
        assertEquals(0, store.size("bob"))
        store.put("bob", byteArrayOf(1))
        store.put("bob", byteArrayOf(2))
        assertEquals(2, store.size("bob"))
    }

    @Test
    fun differentRecipients_isolatedQueues() {
        val store = EnvelopeStore()
        store.put("alice", byteArrayOf(0xAA.toByte()))
        store.put("bob", byteArrayOf(0xBB.toByte()))
        assertContentEquals(byteArrayOf(0xBB.toByte()), store.pop("bob")[0])
        assertContentEquals(byteArrayOf(0xAA.toByte()), store.pop("alice")[0])
    }
}

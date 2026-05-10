package com.stagic.phantm.crdt

import kotlin.test.Test
import kotlin.test.assertEquals

/** AC-M04-1: PN-Counter commutativity, idempotency, associativity. AC-M04-3: merge idempotency. */
class PNCounterTest {

    // ── basic operations ──────────────────────────────────────────────────────

    @Test
    fun incrementDecrementValue() {
        val c = PNCounter()
            .increment("n1", 5)
            .decrement("n1", 2)
            .increment("n2", 3)
        assertEquals(6L, c.value())
    }

    @Test
    fun valueCanGoNegative() {
        val c = PNCounter().decrement("n1", 10).increment("n1", 3)
        assertEquals(-7L, c.value())
    }

    // ── merge properties ──────────────────────────────────────────────────────

    @Test
    fun mergeIsCommutative() {
        val a = PNCounter().increment("n1", 4).decrement("n2", 1)
        val b = PNCounter().increment("n2", 6).decrement("n1", 2)
        assertEquals(a.merge(b).value(), b.merge(a).value())
    }

    @Test
    fun mergeIsIdempotent() {
        val a = PNCounter().increment("n1", 3)
        val b = PNCounter().decrement("n2", 1)
        val merged = a.merge(b)
        assertEquals(merged.value(), merged.merge(b).value())
        assertEquals(merged.value(), a.merge(merged).value())
    }

    @Test
    fun mergeIsAssociative() {
        val a = PNCounter().increment("n1", 1)
        val b = PNCounter().decrement("n2", 2)
        val c = PNCounter().increment("n3", 3)
        assertEquals(a.merge(b).merge(c).value(), a.merge(b.merge(c)).value())
    }

    @Test
    fun concurrentIncrementsMergeCorrectly() {
        val a = PNCounter().increment("n1", 10)
        val b = PNCounter().increment("n2", 5)
        assertEquals(15L, a.merge(b).value())
    }
}

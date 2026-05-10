package com.stagic.phantm.crdt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** AC-M04-1: G-Counter commutativity, idempotency, associativity. AC-M04-3: merge idempotency. */
class GCounterTest {

    // ── basic operations ──────────────────────────────────────────────────────

    @Test
    fun incrementIncreasesValue() {
        val c = GCounter().increment("n1").increment("n1").increment("n2")
        assertEquals(3L, c.value())
    }

    @Test
    fun separateNodesAccumulate() {
        val c = GCounter().increment("a", 5).increment("b", 3)
        assertEquals(8L, c.value())
    }

    // ── merge properties ──────────────────────────────────────────────────────

    @Test
    fun mergeIsCommutative() {
        val a = GCounter(mapOf("n1" to 3L, "n2" to 1L))
        val b = GCounter(mapOf("n1" to 1L, "n3" to 5L))
        assertEquals(a.merge(b), b.merge(a))
    }

    @Test
    fun mergeIsIdempotent() {
        val a = GCounter(mapOf("n1" to 4L))
        val b = GCounter(mapOf("n2" to 2L))
        val merged = a.merge(b)
        assertEquals(merged, merged.merge(b))
        assertEquals(merged, a.merge(merged))
    }

    @Test
    fun mergeIsAssociative() {
        val a = GCounter(mapOf("n1" to 1L))
        val b = GCounter(mapOf("n2" to 2L))
        val c = GCounter(mapOf("n3" to 3L))
        assertEquals(a.merge(b).merge(c), a.merge(b.merge(c)))
    }

    @Test
    fun mergeTakesMaxPerNode() {
        val a = GCounter(mapOf("n1" to 5L))
        val b = GCounter(mapOf("n1" to 3L))
        assertEquals(5L, a.merge(b).value())
        assertEquals(5L, b.merge(a).value())
    }

    @Test
    fun mergeOfDisjointCountersUnions() {
        val a = GCounter(mapOf("n1" to 2L))
        val b = GCounter(mapOf("n2" to 7L))
        assertEquals(9L, a.merge(b).value())
    }

    @Test
    fun emptyMergeIsIdentity() {
        val a = GCounter(mapOf("n1" to 3L))
        assertEquals(a, a.merge(GCounter()))
        assertEquals(a, GCounter().merge(a))
    }
}

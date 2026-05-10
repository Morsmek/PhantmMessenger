package com.stagic.phantm.crdt

import kotlin.test.Test
import kotlin.test.assertEquals

/** AC-M04-1: LWW-Register commutativity and tie-break determinism. AC-M04-3: merge idempotency. */
class LwwRegisterTest {

    // ── merge: higher timestamp wins ──────────────────────────────────────────

    @Test
    fun higherTimestampWins() {
        val a = LwwRegister("old", 1000L, "n1")
        val b = LwwRegister("new", 2000L, "n2")
        assertEquals("new", a.merge(b).value)
        assertEquals("new", b.merge(a).value)
    }

    @Test
    fun mergeIsCommutative() {
        val a = LwwRegister("alpha", 5000L, "n1")
        val b = LwwRegister("beta", 3000L, "n2")
        assertEquals(a.merge(b), b.merge(a))
    }

    @Test
    fun mergeIsIdempotent() {
        val a = LwwRegister("x", 1000L, "n1")
        val b = LwwRegister("y", 2000L, "n2")
        val merged = a.merge(b)
        assertEquals(merged, merged.merge(b))
        assertEquals(merged, a.merge(merged))
    }

    // ── tie-break: higher nodeId wins ────────────────────────────────────────

    @Test
    fun tieBreakByNodeIdIsCommutative() {
        val a = LwwRegister("a-val", 1000L, "node-a")
        val b = LwwRegister("b-val", 1000L, "node-b")
        // "node-b" > "node-a" lexicographically → b wins
        assertEquals("b-val", a.merge(b).value)
        assertEquals("b-val", b.merge(a).value)
    }

    @Test
    fun tieBreakIsStable() {
        val r = LwwRegister(42, 9000L, "n1")
        assertEquals(r, r.merge(r))
    }

    // ── set ───────────────────────────────────────────────────────────────────

    @Test
    fun setReplacesValue() {
        val r = LwwRegister("initial", 1000L, "n1")
        val updated = r.set("updated", 2000L, "n1")
        assertEquals("updated", updated.value)
        assertEquals(2000L, updated.timestampMs)
    }
}

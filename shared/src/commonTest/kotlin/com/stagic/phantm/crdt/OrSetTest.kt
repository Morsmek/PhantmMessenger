package com.stagic.phantm.crdt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** AC-M04-1: OR-Set commutativity, idempotency, and observed-remove semantics. AC-M04-3: merge idempotency. */
class OrSetTest {

    // ── basic operations ──────────────────────────────────────────────────────

    @Test
    fun addMakesElementPresent() {
        val s = OrSet<String>().add("apple", "t1")
        assertTrue(s.contains("apple"))
    }

    @Test
    fun removeMakesElementAbsent() {
        val s = OrSet<String>().add("apple", "t1").remove("apple")
        assertFalse(s.contains("apple"))
    }

    @Test
    fun toSetReflectsCurrentElements() {
        val s = OrSet<String>()
            .add("a", "t1")
            .add("b", "t2")
            .remove("a")
        assertEquals(setOf("b"), s.toSet())
    }

    @Test
    fun removeOfAbsentElementIsNoOp() {
        val s = OrSet<String>()
        val afterRemove = s.remove("ghost")
        assertEquals(s, afterRemove)
    }

    // ── observed-remove semantics ─────────────────────────────────────────────

    @Test
    fun concurrentAddWinsOverRemove() {
        // Node A adds "apple" with tag t1
        val a = OrSet<String>().add("apple", "t1")
        // Node B removes "apple" (only knew about empty state before A's add)
        val b = OrSet<String>().remove("apple")
        // After merge: A's unseen add-tag survives — "apple" is present
        val merged = a.merge(b)
        assertTrue(merged.contains("apple"))
    }

    @Test
    fun removeOfObservedAddIsEffective() {
        // Node A adds "apple" with t1; Node B observes then removes it
        val base = OrSet<String>().add("apple", "t1")
        val aFinal = base  // A keeps its state
        val bFinal = base.remove("apple")  // B removes after observing
        // Merge: B removed all tags it saw — apple is gone
        val merged = aFinal.merge(bFinal)
        assertFalse(merged.contains("apple"))
    }

    @Test
    fun newAddAfterRemoveRestoresElement() {
        val s = OrSet<String>().add("x", "t1").remove("x").add("x", "t2")
        assertTrue(s.contains("x"))
    }

    // ── merge properties ──────────────────────────────────────────────────────

    @Test
    fun mergeIsCommutative() {
        val a = OrSet<String>().add("a", "t1").add("b", "t2")
        val b = OrSet<String>().add("b", "t3").add("c", "t4").remove("a")
        assertEquals(a.merge(b).toSet(), b.merge(a).toSet())
    }

    @Test
    fun mergeIsIdempotent() {
        val a = OrSet<String>().add("x", "t1")
        val b = OrSet<String>().add("y", "t2").remove("x")
        val merged = a.merge(b)
        assertEquals(merged.toSet(), merged.merge(b).toSet())
        assertEquals(merged.toSet(), a.merge(merged).toSet())
    }

    @Test
    fun mergeIsAssociative() {
        val a = OrSet<String>().add("p", "t1")
        val b = OrSet<String>().add("q", "t2")
        val c = OrSet<String>().add("r", "t3").remove("p")
        assertEquals(
            a.merge(b).merge(c).toSet(),
            a.merge(b.merge(c)).toSet(),
        )
    }

    @Test
    fun emptyMergeIsIdentity() {
        val s = OrSet<String>().add("a", "t1")
        assertEquals(s.toSet(), s.merge(OrSet()).toSet())
        assertEquals(s.toSet(), OrSet<String>().merge(s).toSet())
    }
}

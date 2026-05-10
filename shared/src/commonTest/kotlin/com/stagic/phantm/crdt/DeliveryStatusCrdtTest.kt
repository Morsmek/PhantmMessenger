package com.stagic.phantm.crdt

import com.stagic.phantm.db.DeliveryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

/**
 * Covers:
 * AC-M04-2: 3-node convergence under concurrent delivery-status updates.
 * AC-M04-3: Merge idempotency.
 * AC-M04-4: SyncDelta contains no message content — only metadata.
 */
class DeliveryStatusCrdtTest {

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialStatusIsPending() {
        val crdt = DeliveryStatusCrdt.initial("msg-1", "node-a", 1000L)
        assertEquals(DeliveryStatus.PENDING, crdt.status)
    }

    // ── update: monotonic advancement ─────────────────────────────────────────

    @Test
    fun updateAdvancesStatus() {
        val crdt = DeliveryStatusCrdt.initial("msg-1", "node-a", 1000L)
            .update(DeliveryStatus.SENT, 2000L, "node-a")
            .update(DeliveryStatus.DELIVERED, 3000L, "node-a")
        assertEquals(DeliveryStatus.DELIVERED, crdt.status)
    }

    @Test
    fun updateDoesNotRollBackStatus() {
        val crdt = DeliveryStatusCrdt.initial("msg-1", "node-a", 1000L)
            .update(DeliveryStatus.READ, 2000L, "node-a")
            .update(DeliveryStatus.PENDING, 3000L, "node-a")
        assertEquals(DeliveryStatus.READ, crdt.status)
    }

    // ── AC-M04-2: 3-node convergence ─────────────────────────────────────────

    @Test
    fun threeNodesConcurrentUpdatesConverge() {
        // Three nodes start from PENDING and each apply different statuses
        val nodeA = DeliveryStatusCrdt.initial("msg-1", "node-a", 1000L)
            .update(DeliveryStatus.SENT, 1100L, "node-a")
        val nodeB = DeliveryStatusCrdt.initial("msg-1", "node-b", 1000L)
            .update(DeliveryStatus.DELIVERED, 1200L, "node-b")
        val nodeC = DeliveryStatusCrdt.initial("msg-1", "node-c", 1000L)
            .update(DeliveryStatus.READ, 1300L, "node-c")

        // All merge orderings must converge to READ (highest ordinal wins)
        val abc = nodeA.merge(nodeB).merge(nodeC)
        val bca = nodeB.merge(nodeC).merge(nodeA)
        val cab = nodeC.merge(nodeA).merge(nodeB)
        val cba = nodeC.merge(nodeB).merge(nodeA)

        assertEquals(DeliveryStatus.READ, abc.status)
        assertEquals(DeliveryStatus.READ, bca.status)
        assertEquals(DeliveryStatus.READ, cab.status)
        assertEquals(DeliveryStatus.READ, cba.status)
    }

    @Test
    fun twoNodesWithSameStatusUseLwwTieBreak() {
        val nodeA = DeliveryStatusCrdt.initial("msg-2", "node-a", 1000L)
            .update(DeliveryStatus.DELIVERED, 2000L, "node-a")
        val nodeB = DeliveryStatusCrdt.initial("msg-2", "node-b", 1000L)
            .update(DeliveryStatus.DELIVERED, 2000L, "node-b")

        // Same status + same timestamp → nodeId tie-break; must be commutative
        assertEquals(nodeA.merge(nodeB).status, nodeB.merge(nodeA).status)
    }

    @Test
    fun allDeliveryStatusValuesCanBeReached() {
        var crdt = DeliveryStatusCrdt.initial("msg-3", "node-a", 0L)
        DeliveryStatus.entries.forEachIndexed { i, status ->
            crdt = crdt.update(status, i * 1000L, "node-a")
            assertEquals(status, crdt.status)
        }
    }

    // ── AC-M04-3: merge idempotency ───────────────────────────────────────────

    @Test
    fun mergeIsIdempotent() {
        val a = DeliveryStatusCrdt.initial("msg-4", "node-a", 1000L)
            .update(DeliveryStatus.SENT, 1100L, "node-a")
        val b = DeliveryStatusCrdt.initial("msg-4", "node-b", 1000L)
            .update(DeliveryStatus.READ, 1200L, "node-b")
        val merged = a.merge(b)
        assertEquals(merged.status, merged.merge(b).status)
        assertEquals(merged.status, a.merge(merged).status)
    }

    @Test
    fun mergeIsCommutative() {
        val a = DeliveryStatusCrdt.initial("msg-5", "node-a", 1000L)
            .update(DeliveryStatus.DELIVERED, 2000L, "node-a")
        val b = DeliveryStatusCrdt.initial("msg-5", "node-b", 1000L)
            .update(DeliveryStatus.READ, 1500L, "node-b")
        assertEquals(a.merge(b).status, b.merge(a).status)
    }

    @Test
    fun mergeOfDifferentMessageIdsThrows() {
        val a = DeliveryStatusCrdt.initial("msg-A", "node-a", 1000L)
        val b = DeliveryStatusCrdt.initial("msg-B", "node-b", 1000L)
        assertFails { a.merge(b) }
    }

    // ── AC-M04-4: delta contains no message content ───────────────────────────

    @Test
    fun deliveryStatusDeltaContainsOnlyMetadata() {
        val crdt = DeliveryStatusCrdt.initial("msg-6", "node-a", 1000L)
            .update(DeliveryStatus.DELIVERED, 2000L, "node-a")
        val delta = crdt.toDelta("node-a")

        assertTrue(delta is SyncDelta.DeliveryStatusDelta)
        // Delta carries only the message ID, status, timestamp, and node — no payload
        assertEquals("msg-6", delta.messageId)
        assertEquals(DeliveryStatus.DELIVERED, delta.status)
        assertEquals(2000L, delta.timestampMs)
        assertEquals("node-a", delta.nodeId)
        // Compile-time guarantee: DeliveryStatusDelta has no encryptedPayload field
    }

    @Test
    fun reactionDeltaContainsOnlyMetadata() {
        val delta = SyncDelta.ReactionDelta(
            messageId = "msg-7",
            reaction = "thumbsup",
            userId = "alice",
            tag = "unique-add-tag-abc",
            operation = ReactionOperation.ADD,
            timestampMs = 5000L,
        )
        assertEquals("msg-7", delta.messageId)
        assertEquals("thumbsup", delta.reaction)
        assertEquals(ReactionOperation.ADD, delta.operation)
        // No message body, no encrypted payload — compile-time guarantee via sealed class fields
    }
}

package com.stagic.phantm.crdt

import com.stagic.phantm.db.DeliveryStatus

/**
 * Wire-format delta types for CRDT sync. Deliberately contain only metadata identifiers —
 * no encrypted payload, no message body — satisfying AC-M04-4.
 */
sealed class SyncDelta {

    /** Delivery-status update for a single message. */
    data class DeliveryStatusDelta(
        val messageId: String,
        val status: DeliveryStatus,
        val timestampMs: Long,
        val nodeId: String,
    ) : SyncDelta()

    /** Reaction add or remove for a single message. */
    data class ReactionDelta(
        val messageId: String,
        val reaction: String,
        val userId: String,
        val tag: String,
        val operation: ReactionOperation,
        val timestampMs: Long,
    ) : SyncDelta()
}

enum class ReactionOperation { ADD, REMOVE }

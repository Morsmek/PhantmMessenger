package com.stagic.phantm.crdt

import com.stagic.phantm.db.DeliveryStatus

/**
 * Per-message delivery-status CRDT.
 *
 * Status advances monotonically (PENDING < SENT < DELIVERED < READ < FAILED by ordinal).
 * When two nodes hold the same ordinal, LWW by timestamp + nodeId breaks the tie.
 * Merge always converges to the highest-ordinal status.
 */
data class DeliveryStatusCrdt(
    val messageId: String,
    val register: LwwRegister<DeliveryStatus>,
) {
    val status: DeliveryStatus get() = register.value

    fun update(newStatus: DeliveryStatus, timestampMs: Long, nodeId: String): DeliveryStatusCrdt {
        val candidate = LwwRegister(newStatus, timestampMs, nodeId)
        val next = when {
            newStatus.ordinal > register.value.ordinal -> candidate
            newStatus.ordinal == register.value.ordinal -> register.merge(candidate)
            else -> register
        }
        return copy(register = next)
    }

    fun merge(other: DeliveryStatusCrdt): DeliveryStatusCrdt {
        require(other.messageId == messageId) { "Cannot merge CRDTs for different messages" }
        val merged = when {
            other.register.value.ordinal > register.value.ordinal -> other.register
            other.register.value.ordinal < register.value.ordinal -> register
            else -> register.merge(other.register)
        }
        return DeliveryStatusCrdt(messageId, merged)
    }

    fun toDelta(nodeId: String): SyncDelta.DeliveryStatusDelta = SyncDelta.DeliveryStatusDelta(
        messageId = messageId,
        status = status,
        timestampMs = register.timestampMs,
        nodeId = nodeId,
    )

    companion object {
        fun initial(messageId: String, nodeId: String, timestampMs: Long): DeliveryStatusCrdt =
            DeliveryStatusCrdt(messageId, LwwRegister(DeliveryStatus.PENDING, timestampMs, nodeId))
    }
}

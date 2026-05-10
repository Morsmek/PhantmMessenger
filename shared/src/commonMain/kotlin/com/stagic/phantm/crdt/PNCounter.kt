package com.stagic.phantm.crdt

/** Positive-negative counter built from two G-Counters. Value may go negative. */
data class PNCounter(
    val increments: GCounter = GCounter(),
    val decrements: GCounter = GCounter(),
) {
    fun increment(nodeId: String, by: Long = 1L): PNCounter =
        copy(increments = increments.increment(nodeId, by))

    fun decrement(nodeId: String, by: Long = 1L): PNCounter =
        copy(decrements = decrements.increment(nodeId, by))

    fun value(): Long = increments.value() - decrements.value()

    fun merge(other: PNCounter): PNCounter = PNCounter(
        increments = increments.merge(other.increments),
        decrements = decrements.merge(other.decrements),
    )
}

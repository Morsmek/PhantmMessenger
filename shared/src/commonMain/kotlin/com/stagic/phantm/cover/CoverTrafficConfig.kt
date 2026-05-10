package com.stagic.phantm.cover

/**
 * Configuration for the cover traffic generator.
 *
 * @param packetsPerMinute How many cover packets to send per minute while connected.
 * @param minPayloadBytes  Minimum encrypted payload size in bytes.
 * @param maxPayloadBytes  Maximum encrypted payload size in bytes.
 *                         Sizes are drawn uniformly from [minPayloadBytes, maxPayloadBytes].
 *                         Set both to the same value for a fixed packet size.
 */
data class CoverTrafficConfig(
    val packetsPerMinute: Int = 10,
    val minPayloadBytes: Int = 256,
    val maxPayloadBytes: Int = 1024,
) {
    init {
        require(packetsPerMinute > 0) { "packetsPerMinute must be positive" }
        require(minPayloadBytes > 0) { "minPayloadBytes must be positive" }
        require(maxPayloadBytes >= minPayloadBytes) { "maxPayloadBytes must be >= minPayloadBytes" }
    }

    /** Inter-packet interval in milliseconds. */
    internal val intervalMs: Long get() = 60_000L / packetsPerMinute
}

/** Sentinel recipient ID that tells the relay to silently discard the packet. */
const val COVER_RECIPIENT_ID = "__phantm_cover__"

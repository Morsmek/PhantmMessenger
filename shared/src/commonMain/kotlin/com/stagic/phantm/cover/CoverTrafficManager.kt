package com.stagic.phantm.cover

import com.stagic.phantm.transport.TransportClient

/**
 * Generates and transmits dummy cover packets at a configured rate to resist
 * traffic analysis. Cover packets are encrypted random payloads addressed to
 * [COVER_RECIPIENT_ID]; the relay discards them silently (AC-M06-3).
 *
 * Cover packet sizes are drawn uniformly from the configured range, designed to
 * match the distribution of real M07 envelopes (AC-M06-1).
 */
interface CoverTrafficManager {

    /** `true` while the background generation coroutine is active. */
    val isRunning: Boolean

    /**
     * Start emitting cover packets via [transport] at [config] rate.
     * Packets are only sent when [transport] is in the CONNECTED state.
     * Calling [start] while already running replaces the previous configuration.
     */
    fun start(transport: TransportClient, config: CoverTrafficConfig = CoverTrafficConfig())

    /** Stop cover traffic generation and cancel the background coroutine. */
    fun stop()
}

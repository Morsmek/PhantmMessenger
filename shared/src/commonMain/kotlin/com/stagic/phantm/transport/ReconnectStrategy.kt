package com.stagic.phantm.transport

internal const val INITIAL_RECONNECT_DELAY_MS = 1_000L
internal const val MAX_RECONNECT_DELAY_MS = 30_000L

/**
 * Exponential backoff delay for reconnect attempt [attempt] (0-based).
 * Doubles each attempt, capped at [MAX_RECONNECT_DELAY_MS].
 *
 * attempt=0 → 1s, attempt=1 → 2s, attempt=2 → 4s, …, attempt≥5 → 30s
 */
internal fun reconnectDelayMs(attempt: Int): Long =
    (INITIAL_RECONNECT_DELAY_MS * (1L shl attempt.coerceAtMost(5)))
        .coerceAtMost(MAX_RECONNECT_DELAY_MS)

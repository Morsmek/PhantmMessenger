package com.stagic.phantm.relay

import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks live WebSocket sessions by recipient ID (AC-M09-2).
 * Thread-safe via [ConcurrentHashMap]. IP addresses are never stored (AC-M09-6).
 */
class ConnectionRegistry {

    private val sessions = ConcurrentHashMap<String, DefaultWebSocketSession>()

    fun register(recipientId: String, session: DefaultWebSocketSession) {
        sessions[recipientId] = session
    }

    fun unregister(recipientId: String) {
        sessions.remove(recipientId)
    }

    fun isConnected(recipientId: String): Boolean = sessions.containsKey(recipientId)

    /**
     * Send [frameBytes] to [recipientId]'s live session.
     * Returns `true` on success, `false` if the recipient is not connected or the send fails.
     */
    suspend fun send(recipientId: String, frameBytes: ByteArray): Boolean {
        val session = sessions[recipientId] ?: return false
        return runCatching { session.send(Frame.Binary(true, frameBytes)); true }
            .getOrDefault(false)
    }
}

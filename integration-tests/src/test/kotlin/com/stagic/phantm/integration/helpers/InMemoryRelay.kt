package com.stagic.phantm.integration.helpers

import com.stagic.phantm.cover.COVER_RECIPIENT_ID
import com.stagic.phantm.transport.TransportEnvelope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory relay that routes [TransportEnvelope] frames between [FakeTransportClient] instances.
 *
 * Semantics mirror M09 [RelayServer]:
 * - Cover traffic (`COVER_RECIPIENT_ID`) is discarded silently.
 * - Envelopes for offline recipients are queued; delivered when the recipient connects.
 * - No plaintext inspection; [TransportEnvelope.bytes] are opaque.
 */
class InMemoryRelay {

    private val clients = ConcurrentHashMap<String, FakeTransportClient>()
    private val offlineQueue = ConcurrentHashMap<String, CopyOnWriteArrayList<TransportEnvelope>>()
    /** Count of cover-traffic frames received — for test assertions only. */
    var coverTrafficReceived = 0
        private set

    fun register(recipientId: String, client: FakeTransportClient) {
        clients[recipientId] = client
        // Flush offline queue
        offlineQueue.remove(recipientId)?.let { queued ->
            // deliver in a new coroutine — caller's scope handles this via async test runner
            queued.forEach { envelope ->
                kotlinx.coroutines.runBlocking { client.receive(envelope) }
            }
        }
    }

    fun unregister(recipientId: String) {
        clients.remove(recipientId)
    }

    suspend fun deliver(envelope: TransportEnvelope) {
        if (envelope.recipientId == COVER_RECIPIENT_ID) {
            coverTrafficReceived++
            return // discard silently
        }
        val client = clients[envelope.recipientId]
        if (client != null) {
            client.receive(envelope)
        } else {
            offlineQueue.getOrPut(envelope.recipientId) { CopyOnWriteArrayList() }.add(envelope)
        }
    }

    /** Number of envelopes queued for offline recipient [recipientId]. */
    fun queueSize(recipientId: String): Int = offlineQueue[recipientId]?.size ?: 0
}

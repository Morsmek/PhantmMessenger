@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.relay

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Installs the relay WebSocket endpoint.
 *
 * Protocol (binary protobuf [RelayFrame] frames):
 *  - Registration:  first frame has empty [RelayFrame.envelopeBytes]; [RelayFrame.recipientId]
 *                   identifies the client. Client IP is never read or logged (AC-M09-6).
 *  - Envelope:      [RelayFrame.recipientId] is the intended recipient; routed to the live
 *                   session or stored for later delivery (AC-M09-2, AC-M09-3).
 *  - Cover traffic: [RelayFrame.recipientId] == [COVER_RECIPIENT_ID] → discarded, no log (AC-M09-5).
 *  - Payload bytes  are never inspected or decrypted (AC-M09-4).
 */
fun Application.configureRelay(
    registry: ConnectionRegistry = ConnectionRegistry(),
    store: EnvelopeStore = EnvelopeStore(),
) {
    install(WebSockets)

    routing {
        webSocket("/ws") {
            var myId: String? = null
            try {
                for (frame in incoming) {
                    if (frame !is Frame.Binary) continue
                    val frameBytes = frame.readBytes()
                    val rf = runCatching {
                        ProtoBuf.decodeFromByteArray(RelayFrame.serializer(), frameBytes)
                    }.getOrNull() ?: continue

                    if (rf.envelopeBytes.isEmpty()) {
                        // Registration message
                        myId = rf.recipientId
                        registry.register(myId, this)
                        store.pop(myId).forEach { queued -> send(Frame.Binary(true, queued)) }
                        continue
                    }

                    // Cover traffic silently discarded — no storage, no logging (AC-M09-5)
                    if (rf.recipientId == COVER_RECIPIENT_ID) continue

                    // Route to live session or queue for offline recipient (AC-M09-2, AC-M09-3)
                    if (!registry.send(rf.recipientId, frameBytes)) {
                        store.put(rf.recipientId, frameBytes)
                    }
                }
            } finally {
                myId?.let { registry.unregister(it) }
            }
        }
    }
}

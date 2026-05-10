@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.mesh

import com.stagic.phantm.err
import com.stagic.phantm.ok
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * 2-hop mesh router. Sits on top of [MeshNetwork] and:
 * - encodes outbound M07 envelopes into [MeshFrame] protobuf frames,
 * - routes frames to the correct direct peer (or relay),
 * - delivers inbound frames addressed to [localId] to a registered handler,
 * - forwards in-transit frames one additional hop (hopCount ≤ [MAX_HOPS]).
 *
 * No plaintext content ever passes through this class — [envelopeBytes] are treated
 * as opaque binary blobs (AC-M10-4).
 */
class MeshRouter(
    val localId: String,
    private val transport: MeshNetwork,
) {
    private var envelopeHandler: (suspend (fromId: String, envelopeBytes: ByteArray) -> Unit)? = null

    init {
        transport.onFrameReceived { fromPeerId, frameBytes ->
            val frame = runCatching {
                ProtoBuf.decodeFromByteArray(MeshFrame.serializer(), frameBytes)
            }.getOrNull() ?: return@onFrameReceived

            if (frame.finalRecipientId == localId) {
                envelopeHandler?.invoke(fromPeerId, frame.envelopeBytes)
            } else if (frame.hopCount < MAX_HOPS) {
                forwardFrame(frame.copy(hopCount = frame.hopCount + 1))
            }
            // frames exceeding MAX_HOPS are silently dropped
        }
    }

    /** Register the callback invoked when a mesh frame addressed to [localId] arrives. */
    fun onEnvelopeReceived(handler: suspend (fromId: String, envelopeBytes: ByteArray) -> Unit) {
        envelopeHandler = handler
    }

    /**
     * Send an already-encrypted M07 [envelopeBytes] to [finalRecipientId].
     *
     * Routing:
     * 1. If [finalRecipientId] is a direct peer → send directly.
     * 2. Else find a direct peer whose [MeshPeer.reachableIds] contain [finalRecipientId] → relay.
     * 3. Else return [MeshError.NoRoute].
     */
    suspend fun sendEnvelope(finalRecipientId: String, envelopeBytes: ByteArray): MeshResult<Unit> {
        if (!transport.isRunning) return MeshError.NotRunning().err()

        val frame = MeshFrame(finalRecipientId = finalRecipientId, hopCount = 0, envelopeBytes = envelopeBytes)
        val frameBytes = ProtoBuf.encodeToByteArray(MeshFrame.serializer(), frame)

        val peers = transport.directPeers
        val direct = peers.firstOrNull { it.id == finalRecipientId }
        if (direct != null) return transport.send(finalRecipientId, frameBytes)

        val relay = peers.firstOrNull { finalRecipientId in it.reachableIds }
            ?: return MeshError.NoRoute(finalRecipientId).err()
        return transport.send(relay.id, frameBytes)
    }

    private suspend fun forwardFrame(frame: MeshFrame) {
        val frameBytes = ProtoBuf.encodeToByteArray(MeshFrame.serializer(), frame)
        val peers = transport.directPeers
        val direct = peers.firstOrNull { it.id == frame.finalRecipientId }
        if (direct != null) {
            transport.send(frame.finalRecipientId, frameBytes)
            return
        }
        val relay = peers.firstOrNull { frame.finalRecipientId in it.reachableIds } ?: return
        transport.send(relay.id, frameBytes)
    }

    companion object {
        const val MAX_HOPS = 2
    }
}

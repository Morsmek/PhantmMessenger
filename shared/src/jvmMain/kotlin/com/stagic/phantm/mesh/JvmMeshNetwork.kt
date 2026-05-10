package com.stagic.phantm.mesh

import com.stagic.phantm.err
import com.stagic.phantm.ok

/**
 * In-memory [MeshNetwork] for JVM unit tests.
 *
 * Nodes are connected via [connectTo]; frames are delivered synchronously within the
 * calling coroutine, which is sufficient for deterministic in-process tests.
 *
 * Not thread-safe beyond what the test harness provides.
 */
class JvmMeshNetwork(val nodeId: String) : MeshNetwork {

    private val connections = LinkedHashMap<String, JvmMeshNetwork>()
    private var frameHandler: (suspend (String, ByteArray) -> Unit)? = null

    override val isRunning: Boolean = true

    override val directPeers: List<MeshPeer>
        get() = connections.map { (id, node) ->
            MeshPeer(id, reachableIds = node.connections.keys.filter { it != nodeId })
        }

    override fun start(): MeshResult<Unit> = Unit.ok()
    override fun stop() {}

    /**
     * Establishes a bidirectional direct connection between this node and [other].
     * Idempotent — connecting twice is a no-op.
     */
    fun connectTo(other: JvmMeshNetwork) {
        connections[other.nodeId] = other
        other.connections[nodeId] = this
    }

    override suspend fun send(peerId: String, frameBytes: ByteArray): MeshResult<Unit> {
        val peer = connections[peerId] ?: return MeshError.PeerNotFound(peerId).err()
        peer.deliver(nodeId, frameBytes)
        return Unit.ok()
    }

    override fun onFrameReceived(handler: suspend (String, ByteArray) -> Unit) {
        frameHandler = handler
    }

    /** Called by a connected peer to deliver an inbound frame to this node. */
    internal suspend fun deliver(fromId: String, frameBytes: ByteArray) {
        frameHandler?.invoke(fromId, frameBytes)
    }
}

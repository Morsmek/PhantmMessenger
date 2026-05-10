package com.stagic.phantm.mesh

/**
 * Transport abstraction over BLE / WiFi Direct / in-memory (test).
 *
 * Implementations handle peer discovery and raw frame delivery.
 * Routing logic lives in [MeshRouter], which sits on top of this interface.
 */
interface MeshNetwork {
    val isRunning: Boolean

    /** Directly reachable peers, each advertising their own set of reachable IDs. */
    val directPeers: List<MeshPeer>

    /** Start peer discovery and advertising. */
    fun start(): MeshResult<Unit>

    /** Stop all discovery, advertising, and connections. */
    fun stop()

    /**
     * Send [frameBytes] to a specific directly-connected peer.
     * [peerId] must appear in [directPeers]; returns [MeshError.PeerNotFound] otherwise.
     */
    suspend fun send(peerId: String, frameBytes: ByteArray): MeshResult<Unit>

    /**
     * Register a callback invoked for every inbound frame from a direct peer.
     * [fromPeerId] is the ID of the direct peer that sent this frame (not necessarily the originator).
     */
    fun onFrameReceived(handler: suspend (fromPeerId: String, frameBytes: ByteArray) -> Unit)
}

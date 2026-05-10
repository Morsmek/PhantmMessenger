package com.stagic.phantm.mesh

import com.stagic.phantm.err
import com.stagic.phantm.ok

/** iOS stub — full implementation requires CoreBluetooth + MultipeerConnectivity via Swift bridge. */
internal class IosMeshNetwork(private val localId: String) : MeshNetwork {

    override val isRunning: Boolean = false
    override val directPeers: List<MeshPeer> = emptyList()

    override fun start(): MeshResult<Unit> =
        MeshError.NotRunning("iOS mesh requires CoreBluetooth/MultipeerConnectivity bridge").err()

    override fun stop() {}

    override suspend fun send(peerId: String, frameBytes: ByteArray): MeshResult<Unit> =
        MeshError.NotRunning("iOS mesh not implemented").err()

    override fun onFrameReceived(handler: suspend (String, ByteArray) -> Unit) {}
}

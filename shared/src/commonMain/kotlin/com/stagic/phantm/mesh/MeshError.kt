package com.stagic.phantm.mesh

import com.stagic.phantm.PhantmResult

sealed class MeshError {
    data class NoRoute(val recipientId: String) : MeshError()
    data class PeerNotFound(val peerId: String) : MeshError()
    data class SendFailed(val reason: String) : MeshError()
    data class NotRunning(val reason: String = "Mesh network not started") : MeshError()
}

typealias MeshResult<T> = PhantmResult<T, MeshError>

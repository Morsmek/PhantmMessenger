package com.stagic.phantm.mesh

actual fun createMeshNetwork(localId: String): MeshNetwork = JvmMeshNetwork(localId)

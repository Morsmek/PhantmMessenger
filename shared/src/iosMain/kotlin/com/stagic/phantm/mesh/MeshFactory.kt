package com.stagic.phantm.mesh

actual fun createMeshNetwork(localId: String): MeshNetwork = IosMeshNetwork(localId)

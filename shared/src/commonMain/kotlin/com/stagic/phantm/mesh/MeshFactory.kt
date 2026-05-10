package com.stagic.phantm.mesh

/** Returns a platform-specific [MeshNetwork] implementation for [localId]. */
expect fun createMeshNetwork(localId: String): MeshNetwork

package com.stagic.phantm.mesh

/**
 * A directly reachable mesh peer.
 *
 * @param id          Unique identifier for this peer (matches M07 recipientId convention).
 * @param reachableIds IDs that this peer can relay to in one more hop (its own direct peers).
 */
data class MeshPeer(val id: String, val reachableIds: List<String> = emptyList())

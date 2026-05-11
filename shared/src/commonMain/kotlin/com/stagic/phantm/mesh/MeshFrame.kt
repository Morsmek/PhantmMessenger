@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.stagic.phantm.mesh

import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.Serializable

/**
 * Wire frame carried over each mesh hop.
 *
 * [finalRecipientId] is routing metadata (like an IP destination address); it is not
 * sensitive — the encrypted M07 envelope hides the actual message content.
 * [hopCount] is incremented on each relay; frames exceeding [MeshRouter.MAX_HOPS] are dropped.
 */
@Suppress("ArrayInDataClass")
@Serializable
internal data class MeshFrame(
    @ProtoNumber(1) val finalRecipientId: String = "",
    @ProtoNumber(2) val hopCount: Int = 0,
    @ProtoNumber(3) val envelopeBytes: ByteArray = ByteArray(0),
)

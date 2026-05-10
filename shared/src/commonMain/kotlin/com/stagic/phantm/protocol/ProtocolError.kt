package com.stagic.phantm.protocol

import com.stagic.phantm.PhantmResult

sealed class ProtocolError {
    data object UnknownVersion : ProtocolError()
    data object InvalidSignature : ProtocolError()
    data object DecryptionFailed : ProtocolError()
    data object ReplayDetected : ProtocolError()
    data object MalformedEnvelope : ProtocolError()
    data class Unknown(val cause: Throwable) : ProtocolError()
}

typealias ProtocolResult<T> = PhantmResult<T, ProtocolError>

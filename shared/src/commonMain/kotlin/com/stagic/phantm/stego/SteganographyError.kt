package com.stagic.phantm.stego

import com.stagic.phantm.PhantmResult

sealed class SteganographyError {
    data class InvalidCarrier(val reason: String) : SteganographyError()
    data class InsufficientCapacity(val reason: String) : SteganographyError()
    data object DecryptionFailed : SteganographyError()
    data class Unknown(val cause: Throwable) : SteganographyError()
}

typealias SteganographyResult<T> = PhantmResult<T, SteganographyError>

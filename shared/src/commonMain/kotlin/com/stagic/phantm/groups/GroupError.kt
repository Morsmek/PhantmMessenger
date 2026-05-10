package com.stagic.phantm.groups

import com.stagic.phantm.PhantmResult

sealed class GroupError {
    data class EncryptionFailed(val reason: String) : GroupError()
    data object DecryptionFailed : GroupError()
    data object GroupNotFound : GroupError()
    data class StorageFailed(val reason: String) : GroupError()
    data class Unknown(val cause: Throwable) : GroupError()
}

typealias GroupResult<T> = PhantmResult<T, GroupError>

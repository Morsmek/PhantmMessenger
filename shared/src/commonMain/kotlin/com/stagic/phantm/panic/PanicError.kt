package com.stagic.phantm.panic

sealed class PanicError {
    data class WipeFailed(val reason: String) : PanicError()
    data class ConfigError(val reason: String) : PanicError()
}

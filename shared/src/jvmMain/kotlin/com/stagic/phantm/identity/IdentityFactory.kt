package com.stagic.phantm.identity

import com.stagic.phantm.crypto.CryptoCore

/** Empty platform context — JVM unit tests pass `PlatformContext()`. */
actual class PlatformContext

actual fun createIdentityManager(crypto: CryptoCore, context: PlatformContext): IdentityManager =
    JvmIdentityManager(crypto)

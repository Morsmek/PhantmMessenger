package com.stagic.phantm.identity

import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.err

/** Empty placeholder — iOS requires SecureEnclave cinterop bindings before use. */
actual class PlatformContext

actual fun createIdentityManager(crypto: CryptoCore, context: PlatformContext): IdentityManager =
    object : IdentityManager {
        private fun notImplemented(): Nothing =
            throw NotImplementedError("M02 iOS implementation requires Secure Enclave cinterop — not yet wired")

        override suspend fun createIdentity(): IdentityResult<Identity> = notImplemented()
        override suspend fun loadIdentity(): IdentityResult<Identity> = notImplemented()
        override suspend fun hasIdentity(): Boolean = notImplemented()
        override suspend fun destroyIdentity(): IdentityResult<Unit> = notImplemented()
        override suspend fun loadPrivateKeys(): IdentityResult<IdentityPrivateKeys> = notImplemented()
    }

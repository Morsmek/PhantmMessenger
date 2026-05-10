package com.stagic.phantm.identity

import com.stagic.phantm.crypto.CryptoCore

actual typealias PlatformContext = android.content.Context

actual fun createIdentityManager(crypto: CryptoCore, context: PlatformContext): IdentityManager =
    AndroidIdentityManager(crypto, context)

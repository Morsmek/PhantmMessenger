package com.stagic.phantm.crypto

/** Returns the platform-specific [CryptoCore] implementation. */
expect fun createCryptoCore(): CryptoCore

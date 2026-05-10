package com.stagic.phantm.identity

import com.stagic.phantm.crypto.CryptoCore

/**
 * Platform context used to wire up the IdentityManager.
 * - Android: typealias to android.content.Context
 * - JVM (tests): empty placeholder class
 * - iOS: empty placeholder class
 */
expect class PlatformContext

/** Returns the platform-specific [IdentityManager]. */
expect fun createIdentityManager(crypto: CryptoCore, context: PlatformContext): IdentityManager

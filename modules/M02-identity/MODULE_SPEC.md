# M02 — Identity

**Status:** NOT STARTED
**Dependencies:** M01 (Crypto Core)
**Dependents:** M07, M12

---

## Purpose

Manages the user's long-term cryptographic identity. Creates, stores, and retrieves keypairs
using OS-native secure storage (Android Keystore on Android, Secure Enclave on iOS).
Private keys MUST never leave the secure hardware element.

---

## Public API

```kotlin
package com.stagic.phantm.identity

interface IdentityManager {
    /** Create a new identity, storing private keys in the secure element. */
    suspend fun createIdentity(): Result<Identity, IdentityError>

    /** Load the existing identity. Returns error if no identity exists. */
    suspend fun loadIdentity(): Result<Identity, IdentityError>

    /** True if an identity has been initialized on this device. */
    suspend fun hasIdentity(): Boolean

    /** Permanently destroy the identity and all associated key material. */
    suspend fun destroyIdentity(): Result<Unit, IdentityError>
}

data class Identity(
    val id: String,
    val x25519PublicKey: PublicKey,
    val mlKem768PublicKey: PublicKey,
    val ed25519PublicKey: PublicKey,
    val fingerprint: String              // Hex-encoded BLAKE3(ed25519PublicKey)
)

sealed class IdentityError {
    object NoIdentityFound : IdentityError()
    object SecureElementUnavailable : IdentityError()
    object KeyExportForbidden : IdentityError()
    data class Unknown(val cause: Throwable) : IdentityError()
}
```

---

## Acceptance Criteria

See STATUS.md

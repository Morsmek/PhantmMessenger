package com.stagic.phantm.identity

import com.stagic.phantm.crypto.PrivateKey

/**
 * Manages the user's long-term cryptographic identity.
 *
 * Private keys are never exposed through this interface except via [loadPrivateKeys],
 * which is provided solely for M07 (Message Protocol) crypto operations.
 * Callers MUST zero private key bytes immediately after use.
 */
interface IdentityManager {

    /** Create a new identity, storing private keys in the platform secure element. */
    suspend fun createIdentity(): IdentityResult<Identity>

    /** Load the existing public identity. Returns [IdentityError.NoIdentityFound] if none exists. */
    suspend fun loadIdentity(): IdentityResult<Identity>

    /** True if an identity has been initialised on this device. */
    suspend fun hasIdentity(): Boolean

    /** Permanently destroy all identity key material. This operation is irreversible. */
    suspend fun destroyIdentity(): IdentityResult<Unit>

    /**
     * Decrypt and return private key material for in-memory crypto operations.
     * The caller is responsible for zeroing [IdentityPrivateKeys] bytes immediately after use.
     * Used exclusively by M07 (Message Protocol) — do not call from UI or other modules.
     */
    suspend fun loadPrivateKeys(): IdentityResult<IdentityPrivateKeys>
}

/**
 * Transient private key material. MUST be zeroed after use by calling [zero].
 * Never log, serialize, or persist any field of this class.
 */
data class IdentityPrivateKeys(
    val x25519PrivKey: PrivateKey,
    val ed25519PrivKey: PrivateKey,
    val mlKemPrivKey: PrivateKey,
) {
    /** Overwrite all private key bytes with zeros. Call after crypto operation completes. */
    fun zero() {
        x25519PrivKey.bytes.fill(0)
        ed25519PrivKey.bytes.fill(0)
        mlKemPrivKey.bytes.fill(0)
    }

    override fun toString(): String = "IdentityPrivateKeys([redacted])"
}

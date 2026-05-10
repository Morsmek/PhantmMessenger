package com.stagic.phantm.identity

import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.crypto.PublicKey

/**
 * A user's long-term cryptographic identity.
 * Contains only public key material — private keys remain in the secure element.
 */
data class Identity(
    val id: String,
    val x25519PublicKey: PublicKey,
    val mlKem768PublicKey: PublicKey,
    val ed25519PublicKey: PublicKey,
    /** Hex-encoded BLAKE2b-256 of the Ed25519 public key, stable across sessions. */
    val fingerprint: String,
)

sealed class IdentityError {
    data object NoIdentityFound : IdentityError()
    data object SecureElementUnavailable : IdentityError()
    data object KeyExportForbidden : IdentityError()
    data class Unknown(val cause: Throwable) : IdentityError()
}

typealias IdentityResult<T> = PhantmResult<T, IdentityError>

/**
 * Compute the stable identity fingerprint: hex(BLAKE2b-256(ed25519PublicKey)).
 * Uses M01's BLAKE2b-HKDF with a fixed zero salt and domain-separated info.
 */
internal fun computeIdentityFingerprint(crypto: CryptoCore, ed25519Pub: PublicKey): String {
    val hash = crypto.deriveKey(
        ikm = ed25519Pub.bytes,
        salt = ByteArray(32),
        info = "phantm-identity-fingerprint-v1".encodeToByteArray(),
        outputLen = 32,
    )
    return hash.bytes.toHexString()
}

internal fun ByteArray.toHexString(): String =
    joinToString("") { it.toInt().and(0xFF).toString(16).padStart(2, '0') }

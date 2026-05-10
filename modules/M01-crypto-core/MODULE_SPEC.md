# M01 — Crypto Core

**Status:** NOT STARTED
**Dependencies:** None
**Dependents:** M02, M07

---

## Purpose

Provides all cryptographic primitives for the Phantm system. All other modules
MUST obtain crypto operations exclusively through this module's public API.
Direct use of cryptographic libraries outside M01 is prohibited.

---

## Public API

```kotlin
package com.stagic.phantm.crypto

/**
 * All cryptographic operations for Phantm.
 * Backed by libsodium (via KMP binding) and liboqs (ML-KEM-768).
 */
interface CryptoCore {

    // ── Key Generation ──────────────────────────────────────────────────────

    /** Generate an X25519 keypair for ECDH key exchange. */
    fun generateX25519KeyPair(): KeyPair

    /** Generate an Ed25519 keypair for signing. */
    fun generateEd25519KeyPair(): KeyPair

    /** Generate an ML-KEM-768 keypair for post-quantum KEM. */
    fun generateMlKem768KeyPair(): KeyPair

    // ── Hybrid KEM ───────────────────────────────────────────────────────────

    /**
     * Perform X25519 + ML-KEM-768 hybrid key encapsulation.
     * Returns the shared secret and ciphertext to send to the recipient.
     */
    fun hybridEncapsulate(recipientX25519Pub: PublicKey, recipientMlKemPub: PublicKey): HybridKemResult

    /** Decapsulate a hybrid KEM ciphertext to recover the shared secret. */
    fun hybridDecapsulate(
        ciphertext: HybridKemCiphertext,
        x25519PrivKey: PrivateKey,
        mlKemPrivKey: PrivateKey
    ): Result<SharedSecret, CryptoError>

    // ── Symmetric Encryption ─────────────────────────────────────────────────

    /** Encrypt with XSalsa20-Poly1305. Returns nonce + ciphertext. */
    fun secretBox(plaintext: ByteArray, key: SecretKey): EncryptedData

    /** Decrypt with XSalsa20-Poly1305. */
    fun secretBoxOpen(encrypted: EncryptedData, key: SecretKey): Result<ByteArray, CryptoError>

    // ── KDF ──────────────────────────────────────────────────────────────────

    /** Derive a symmetric key from input key material using HKDF-BLAKE3. */
    fun deriveKey(ikm: ByteArray, salt: ByteArray, info: ByteArray, outputLen: Int): SecretKey

    // ── Signing ───────────────────────────────────────────────────────────────

    fun sign(message: ByteArray, signingKey: PrivateKey): Signature
    fun verify(message: ByteArray, signature: Signature, verifyKey: PublicKey): Result<Unit, CryptoError>

    // ── Utilities ─────────────────────────────────────────────────────────────

    /** Generate cryptographically secure random bytes. */
    fun randomBytes(size: Int): ByteArray
}
```

---

## Data Types

```kotlin
@JvmInline value class PublicKey(val bytes: ByteArray)
@JvmInline value class PrivateKey(val bytes: ByteArray)
@JvmInline value class SecretKey(val bytes: ByteArray)
@JvmInline value class Signature(val bytes: ByteArray)
@JvmInline value class SharedSecret(val bytes: ByteArray)

data class KeyPair(val publicKey: PublicKey, val privateKey: PrivateKey)
data class EncryptedData(val nonce: ByteArray, val ciphertext: ByteArray)
data class HybridKemResult(val sharedSecret: SharedSecret, val ciphertext: HybridKemCiphertext)
data class HybridKemCiphertext(val x25519Ciphertext: ByteArray, val mlKemCiphertext: ByteArray)

sealed class CryptoError {
    object DecryptionFailed : CryptoError()
    object InvalidKey : CryptoError()
    object InvalidSignature : CryptoError()
    data class Unknown(val cause: Throwable) : CryptoError()
}
```

---

## Implementation Notes

- Use `lazysodium-android` / `lazysodium-java` for JVM/Android targets
- Use `libsodium.xcframework` (pre-built) for iOS via cinterop
- Use `liboqs` pre-built binaries for ML-KEM-768 on both targets
- All `PrivateKey` values MUST be zeroed from memory after use (use `sodium_memzero`)
- Nonces for `secretBox` MUST be randomly generated (never reused)
- Key sizes: X25519 = 32 bytes, Ed25519 public = 32 bytes / private = 64 bytes, ML-KEM-768 public = 1184 bytes / private = 2400 bytes

---

## Acceptance Criteria

See STATUS.md

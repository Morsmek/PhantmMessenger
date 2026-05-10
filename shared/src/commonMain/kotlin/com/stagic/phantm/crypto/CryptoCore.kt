package com.stagic.phantm.crypto

/**
 * All cryptographic primitives for Phantm.
 *
 * Backed by libsodium (X25519 ECDH, Ed25519, XSalsa20-Poly1305, BLAKE2b-KDF)
 * and Bouncy Castle (ML-KEM-768 per NIST FIPS 203).
 *
 * No other module may call cryptographic libraries directly — all operations
 * MUST flow through this interface.
 */
interface CryptoCore {

    // ── Key Generation ───────────────────────────────────────────────────────

    /** Generate an X25519 keypair for ECDH key exchange. */
    fun generateX25519KeyPair(): KeyPair

    /** Generate an Ed25519 keypair for message signing. */
    fun generateEd25519KeyPair(): KeyPair

    /** Generate an ML-KEM-768 keypair for post-quantum KEM (NIST FIPS 203). */
    fun generateMlKem768KeyPair(): KeyPair

    // ── Hybrid Post-Quantum KEM ──────────────────────────────────────────────

    /**
     * X25519 + ML-KEM-768 hybrid key encapsulation against a recipient's public keys.
     *
     * The shared secret is BLAKE2b-HKDF over concat(x25519_dh_secret, mlkem_secret).
     * The returned [HybridKemCiphertext] must be transmitted to the recipient for decapsulation.
     */
    fun hybridEncapsulate(
        recipientX25519Pub: PublicKey,
        recipientMlKemPub: PublicKey,
    ): HybridKemResult

    /**
     * Decapsulate a hybrid KEM ciphertext to recover the shared secret.
     * Both the recipient's X25519 and ML-KEM-768 private keys are required.
     */
    fun hybridDecapsulate(
        ciphertext: HybridKemCiphertext,
        x25519PrivKey: PrivateKey,
        mlKemPrivKey: PrivateKey,
    ): CryptoResult<SharedSecret>

    // ── Symmetric Encryption ─────────────────────────────────────────────────

    /**
     * Encrypt with XSalsa20-Poly1305 (libsodium secretbox).
     * A random nonce is generated internally; the nonce is included in [EncryptedData].
     */
    fun secretBox(plaintext: ByteArray, key: SecretKey): EncryptedData

    /**
     * Decrypt XSalsa20-Poly1305 ciphertext.
     * Returns [CryptoError.DecryptionFailed] if authentication verification fails.
     */
    fun secretBoxOpen(encrypted: EncryptedData, key: SecretKey): CryptoResult<ByteArray>

    // ── Key Derivation ────────────────────────────────────────────────────────

    /**
     * Derive a symmetric key using BLAKE2b-HKDF (Extract+Expand).
     * [outputLen] must be in 16..64. Defaults to 32 bytes.
     */
    fun deriveKey(
        ikm: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        outputLen: Int = 32,
    ): SecretKey

    // ── Signing ───────────────────────────────────────────────────────────────

    /** Sign [message] with an Ed25519 private key. Returns a 64-byte detached signature. */
    fun sign(message: ByteArray, signingKey: PrivateKey): Signature

    /**
     * Verify an Ed25519 [signature] over [message] using [verifyKey].
     * Returns [CryptoError.InvalidSignature] if verification fails.
     */
    fun verify(message: ByteArray, signature: Signature, verifyKey: PublicKey): CryptoResult<Unit>

    // ── Utilities ─────────────────────────────────────────────────────────────

    /** Generate [size] cryptographically secure random bytes. */
    fun randomBytes(size: Int): ByteArray
}

package com.stagic.phantm.crypto

import com.stagic.phantm.PhantmResult

/** Opaque X25519 or ML-KEM-768 public key bytes. toString is redacted to prevent logging. */
@JvmInline
value class PublicKey(val bytes: ByteArray) {
    override fun toString(): String = "[PublicKey redacted]"
}

/** Opaque private key bytes. toString is redacted to prevent accidental logging. */
@JvmInline
value class PrivateKey(val bytes: ByteArray) {
    override fun toString(): String = "[PrivateKey redacted]"
}

/** Opaque symmetric key bytes. toString is redacted to prevent accidental logging. */
@JvmInline
value class SecretKey(val bytes: ByteArray) {
    override fun toString(): String = "[SecretKey redacted]"
}

@JvmInline
value class Signature(val bytes: ByteArray)

/** Opaque shared secret. toString is redacted to prevent accidental logging. */
@JvmInline
value class SharedSecret(val bytes: ByteArray) {
    override fun toString(): String = "[SharedSecret redacted]"
}

data class KeyPair(
    val publicKey: PublicKey,
    val privateKey: PrivateKey,
) {
    override fun toString(): String = "KeyPair(publicKey=$publicKey, privateKey=[redacted])"
}

data class EncryptedData(
    val nonce: ByteArray,
    val ciphertext: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedData) return false
        return nonce.contentEquals(other.nonce) && ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int = 31 * nonce.contentHashCode() + ciphertext.contentHashCode()
}

/**
 * Hybrid KEM ciphertext: sender's ephemeral X25519 public key (32 bytes)
 * plus ML-KEM-768 ciphertext (1088 bytes).
 */
data class HybridKemCiphertext(
    val x25519Ciphertext: ByteArray,
    val mlKemCiphertext: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HybridKemCiphertext) return false
        return x25519Ciphertext.contentEquals(other.x25519Ciphertext) &&
            mlKemCiphertext.contentEquals(other.mlKemCiphertext)
    }

    override fun hashCode(): Int =
        31 * x25519Ciphertext.contentHashCode() + mlKemCiphertext.contentHashCode()
}

data class HybridKemResult(
    val sharedSecret: SharedSecret,
    val ciphertext: HybridKemCiphertext,
)

sealed class CryptoError {
    data object DecryptionFailed : CryptoError()
    data object InvalidKey : CryptoError()
    data object InvalidSignature : CryptoError()
    data class Unknown(val cause: Throwable) : CryptoError()
}

typealias CryptoResult<T> = PhantmResult<T, CryptoError>

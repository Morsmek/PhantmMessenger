package com.stagic.phantm.crypto

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.GenericHash
import com.goterl.lazysodium.interfaces.DiffieHellman
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.interfaces.Sign
import com.stagic.phantm.err
import com.stagic.phantm.ok
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters
import java.security.SecureRandom

internal class CryptoCoreImpl : CryptoCore {

    private val sodium = LazySodiumJava(SodiumJava())
    private val secureRandom = SecureRandom()

    // ── Key Generation ───────────────────────────────────────────────────────

    override fun generateX25519KeyPair(): KeyPair {
        val pub = ByteArray(Box.PUBLICKEYBYTES)
        val priv = ByteArray(Box.SECRETKEYBYTES)
        check(sodium.cryptoBoxKeypair(pub, priv)) { "X25519 keypair generation failed" }
        return KeyPair(PublicKey(pub), PrivateKey(priv))
    }

    override fun generateEd25519KeyPair(): KeyPair {
        val pub = ByteArray(Sign.PUBLICKEYBYTES)
        val priv = ByteArray(Sign.SECRETKEYBYTES)
        check(sodium.cryptoSignKeypair(pub, priv)) { "Ed25519 keypair generation failed" }
        return KeyPair(PublicKey(pub), PrivateKey(priv))
    }

    override fun generateMlKem768KeyPair(): KeyPair {
        val keyGen = MLKEMKeyPairGenerator()
        keyGen.init(MLKEMKeyGenerationParameters(secureRandom, MLKEMParameters.ml_kem_768))
        val kp = keyGen.generateKeyPair()
        val pub = (kp.public as MLKEMPublicKeyParameters).encoded
        val priv = (kp.private as MLKEMPrivateKeyParameters).encoded
        return KeyPair(PublicKey(pub), PrivateKey(priv))
    }

    // ── Hybrid Post-Quantum KEM ──────────────────────────────────────────────

    override fun hybridEncapsulate(
        recipientX25519Pub: PublicKey,
        recipientMlKemPub: PublicKey,
    ): HybridKemResult {
        // X25519 half: ephemeral DH
        val ephKp = generateX25519KeyPair()
        val x25519Secret = ByteArray(DiffieHellman.SCALARMULT_BYTES)
        check(sodium.cryptoScalarMult(x25519Secret, ephKp.privateKey.bytes, recipientX25519Pub.bytes)) {
            "X25519 scalar mult failed"
        }

        // ML-KEM-768 half
        val mlKemPub = MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, recipientMlKemPub.bytes)
        val kemGen = MLKEMGenerator(secureRandom)
        val encapsulated = kemGen.generateEncapsulated(mlKemPub)
        val mlKemSecret = encapsulated.secret
        val mlKemCt = encapsulated.encapsulation

        // Combine via BLAKE2b-HKDF; zero salt is valid when IKM carries full entropy
        val sharedKey = combineSecrets(x25519Secret, mlKemSecret)

        x25519Secret.fill(0)
        mlKemSecret.fill(0)

        return HybridKemResult(
            sharedSecret = SharedSecret(sharedKey.bytes),
            ciphertext = HybridKemCiphertext(
                x25519Ciphertext = ephKp.publicKey.bytes,
                mlKemCiphertext = mlKemCt,
            ),
        )
    }

    override fun hybridDecapsulate(
        ciphertext: HybridKemCiphertext,
        x25519PrivKey: PrivateKey,
        mlKemPrivKey: PrivateKey,
    ): CryptoResult<SharedSecret> = runCatching {
        // X25519 half
        val x25519Secret = ByteArray(DiffieHellman.SCALARMULT_BYTES)
        check(sodium.cryptoScalarMult(x25519Secret, x25519PrivKey.bytes, ciphertext.x25519Ciphertext)) {
            "X25519 scalar mult failed during decapsulation"
        }

        // ML-KEM-768 half
        val mlKemPriv = MLKEMPrivateKeyParameters(MLKEMParameters.ml_kem_768, mlKemPrivKey.bytes)
        val mlKemSecret = MLKEMExtractor(mlKemPriv).extractSecret(ciphertext.mlKemCiphertext)

        val sharedKey = combineSecrets(x25519Secret, mlKemSecret)

        x25519Secret.fill(0)
        mlKemSecret.fill(0)

        SharedSecret(sharedKey.bytes)
    }.fold(onSuccess = { it.ok() }, onFailure = { CryptoError.Unknown(it).err() })

    /** BLAKE2b-HKDF over concat(x25519Secret, mlKemSecret) with domain-separation info. */
    private fun combineSecrets(x25519Secret: ByteArray, mlKemSecret: ByteArray): SecretKey {
        val ikm = x25519Secret + mlKemSecret
        val zeroSalt = ByteArray(GenericHash.KEYBYTES)
        return deriveKey(ikm, zeroSalt, HYBRID_KEM_INFO)
    }

    // ── Symmetric Encryption ─────────────────────────────────────────────────

    override fun secretBox(plaintext: ByteArray, key: SecretKey): EncryptedData {
        val nonce = randomBytes(SecretBox.NONCEBYTES)
        val ciphertext = ByteArray(SecretBox.MACBYTES + plaintext.size)
        check(
            sodium.cryptoSecretBoxEasy(ciphertext, plaintext, plaintext.size.toLong(), nonce, key.bytes)
        ) { "secretBox encryption failed" }
        return EncryptedData(nonce, ciphertext)
    }

    override fun secretBoxOpen(encrypted: EncryptedData, key: SecretKey): CryptoResult<ByteArray> {
        val plaintext = ByteArray(encrypted.ciphertext.size - SecretBox.MACBYTES)
        val ok = sodium.cryptoSecretBoxOpenEasy(
            plaintext,
            encrypted.ciphertext,
            encrypted.ciphertext.size.toLong(),
            encrypted.nonce,
            key.bytes,
        )
        return if (ok) plaintext.ok() else CryptoError.DecryptionFailed.err()
    }

    // ── Key Derivation ────────────────────────────────────────────────────────

    override fun deriveKey(ikm: ByteArray, salt: ByteArray, info: ByteArray, outputLen: Int): SecretKey {
        require(outputLen in 16..GenericHash.BYTES_MAX) {
            "outputLen must be 16..${GenericHash.BYTES_MAX}, got $outputLen"
        }

        // Normalise salt to a valid BLAKE2b key size (16..64 bytes)
        val normSalt = normaliseSalt(salt)

        // HKDF-Extract: PRK = BLAKE2b-MAC(key=salt, data=ikm)
        val prk = ByteArray(GenericHash.KEYBYTES)
        check(sodium.cryptoGenericHash(prk, prk.size, ikm, ikm.size.toLong(), normSalt, normSalt.size)) {
            "BLAKE2b extract step failed"
        }

        // HKDF-Expand T(1) = BLAKE2b-MAC(key=prk, data=info || 0x01)
        val expandInput = info + byteArrayOf(0x01)
        val okm = ByteArray(outputLen)
        check(sodium.cryptoGenericHash(okm, outputLen, expandInput, expandInput.size.toLong(), prk, prk.size)) {
            "BLAKE2b expand step failed"
        }

        prk.fill(0)
        return SecretKey(okm)
    }

    // ── Signing ───────────────────────────────────────────────────────────────

    override fun sign(message: ByteArray, signingKey: PrivateKey): Signature {
        val sig = ByteArray(Sign.BYTES)
        check(sodium.cryptoSignDetached(sig, message, message.size.toLong(), signingKey.bytes)) {
            "Ed25519 sign failed"
        }
        return Signature(sig)
    }

    override fun verify(message: ByteArray, signature: Signature, verifyKey: PublicKey): CryptoResult<Unit> {
        val valid = sodium.cryptoSignVerifyDetached(
            signature.bytes,
            message,
            message.size,
            verifyKey.bytes,
        )
        return if (valid) Unit.ok() else CryptoError.InvalidSignature.err()
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    override fun randomBytes(size: Int): ByteArray = sodium.randomBytesBuf(size)

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun normaliseSalt(salt: ByteArray): ByteArray {
        if (salt.size in GenericHash.BLAKE2B_KEYBYTES_MIN..GenericHash.KEYBYTES_MAX) return salt
        val out = ByteArray(GenericHash.KEYBYTES)
        salt.copyInto(out, 0, 0, minOf(salt.size, GenericHash.KEYBYTES))
        return out
    }

    private companion object {
        val HYBRID_KEM_INFO = "phantm-hybrid-kem-v1".encodeToByteArray()
    }
}

package com.stagic.phantm.crypto

import com.stagic.phantm.PhantmResult
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers AC-M01-1 through AC-M01-7.
 * Runs on the JVM target via ./gradlew :shared:jvmTest --tests "phantm.M01.*"
 * (package mapped in build config to "phantm.M01").
 */
class CryptoCoreTest {

    private val crypto = createCryptoCore()

    // ── AC-M01-2: Distinct keypairs ──────────────────────────────────────────

    @Test
    fun x25519KeyPairsAreDistinct() {
        val keys = (1..100).map { crypto.generateX25519KeyPair().publicKey.bytes }
        val unique = keys.map { it.toList() }.toSet()
        assertEquals(100, unique.size, "All 100 X25519 public keys must be distinct")
    }

    @Test
    fun ed25519KeyPairsAreDistinct() {
        val keys = (1..100).map { crypto.generateEd25519KeyPair().publicKey.bytes }
        val unique = keys.map { it.toList() }.toSet()
        assertEquals(100, unique.size, "All 100 Ed25519 public keys must be distinct")
    }

    @Test
    fun mlKem768KeyPairsAreDistinct() {
        val keys = (1..10).map { crypto.generateMlKem768KeyPair().publicKey.bytes }
        val unique = keys.map { it.toList() }.toSet()
        assertEquals(10, unique.size, "All ML-KEM-768 public keys must be distinct")
    }

    @Test
    fun x25519KeySizesAreCorrect() {
        val kp = crypto.generateX25519KeyPair()
        assertEquals(32, kp.publicKey.bytes.size, "X25519 public key must be 32 bytes")
        assertEquals(32, kp.privateKey.bytes.size, "X25519 private key must be 32 bytes")
    }

    @Test
    fun ed25519KeySizesAreCorrect() {
        val kp = crypto.generateEd25519KeyPair()
        assertEquals(32, kp.publicKey.bytes.size, "Ed25519 public key must be 32 bytes")
        assertEquals(64, kp.privateKey.bytes.size, "Ed25519 private key must be 64 bytes")
    }

    @Test
    fun mlKem768KeySizesAreCorrect() {
        val kp = crypto.generateMlKem768KeyPair()
        assertEquals(1184, kp.publicKey.bytes.size, "ML-KEM-768 public key must be 1184 bytes")
        // Private key encoding size varies by BC version; assert it is non-trivial
        assertTrue(kp.privateKey.bytes.size >= 64, "ML-KEM-768 private key must be at least 64 bytes")
    }

    // ── AC-M01-3: secretBox / secretBoxOpen round-trip (1000 iterations) ────

    @Test
    fun secretBoxRoundTrip1000Iterations() {
        repeat(1000) { i ->
            val key = SecretKey(crypto.randomBytes(32))
            val plaintext = crypto.randomBytes(Random.nextInt(1, 512))
            val encrypted = crypto.secretBox(plaintext, key)
            val result = crypto.secretBoxOpen(encrypted, key)
            assertTrue(result.isOk(), "Decryption must succeed (iteration $i)")
            assertContentEquals(plaintext, result.valueOrNull!!, "Decrypted content must match (iteration $i)")
        }
    }

    @Test
    fun secretBoxNoncesAreDistinct() {
        val key = SecretKey(crypto.randomBytes(32))
        val plaintext = "test".encodeToByteArray()
        val nonces = (1..100).map { crypto.secretBox(plaintext, key).nonce.toList() }.toSet()
        assertEquals(100, nonces.size, "Each secretBox call must produce a distinct nonce")
    }

    @Test
    fun secretBoxRejectsWrongKey() {
        val key1 = SecretKey(crypto.randomBytes(32))
        val key2 = SecretKey(crypto.randomBytes(32))
        val encrypted = crypto.secretBox("secret".encodeToByteArray(), key1)
        val result = crypto.secretBoxOpen(encrypted, key2)
        assertTrue(result.isErr(), "Opening with wrong key must fail")
        assertEquals(CryptoError.DecryptionFailed, result.errorOrNull)
    }

    @Test
    fun secretBoxRejectsTamperedCiphertext() {
        val key = SecretKey(crypto.randomBytes(32))
        val encrypted = crypto.secretBox("secret".encodeToByteArray(), key)
        val tampered = encrypted.ciphertext.copyOf().also { it[0] = (it[0].toInt() xor 0xFF).toByte() }
        val result = crypto.secretBoxOpen(EncryptedData(encrypted.nonce, tampered), key)
        assertTrue(result.isErr(), "Tampered ciphertext must fail authentication")
    }

    // ── AC-M01-4: ML-KEM-768 encapsulate/decapsulate round-trip ─────────────

    @Test
    fun hybridKemRoundTrip() {
        val aliceX25519 = crypto.generateX25519KeyPair()
        val aliceMlKem = crypto.generateMlKem768KeyPair()

        val kemResult = crypto.hybridEncapsulate(aliceX25519.publicKey, aliceMlKem.publicKey)
        val decapResult = crypto.hybridDecapsulate(
            kemResult.ciphertext,
            aliceX25519.privateKey,
            aliceMlKem.privateKey,
        )

        assertTrue(decapResult.isOk(), "Hybrid decapsulation must succeed: ${decapResult.errorOrNull}")
        assertContentEquals(
            kemResult.sharedSecret.bytes,
            decapResult.valueOrNull!!.bytes,
            "Encapsulated and decapsulated shared secrets must match",
        )
    }

    @Test
    fun hybridKemCiphertextSizesAreCorrect() {
        val x25519Kp = crypto.generateX25519KeyPair()
        val mlKemKp = crypto.generateMlKem768KeyPair()
        val result = crypto.hybridEncapsulate(x25519Kp.publicKey, mlKemKp.publicKey)
        assertEquals(32, result.ciphertext.x25519Ciphertext.size, "X25519 part must be 32 bytes")
        assertEquals(1088, result.ciphertext.mlKemCiphertext.size, "ML-KEM-768 ciphertext must be 1088 bytes")
    }

    @Test
    fun hybridKemFailsWithWrongPrivateKey() {
        val aliceX25519 = crypto.generateX25519KeyPair()
        val aliceMlKem = crypto.generateMlKem768KeyPair()
        val eveX25519 = crypto.generateX25519KeyPair()
        val eveMlKem = crypto.generateMlKem768KeyPair()

        val kemResult = crypto.hybridEncapsulate(aliceX25519.publicKey, aliceMlKem.publicKey)

        // Decapsulate with Eve's keys — either fails or produces wrong secret
        val decapResult = crypto.hybridDecapsulate(
            kemResult.ciphertext,
            eveX25519.privateKey,
            eveMlKem.privateKey,
        )
        if (decapResult.isOk()) {
            // ML-KEM is implicit-rejection: returns a random value, not the real secret
            assertFalse(
                decapResult.valueOrNull!!.bytes.contentEquals(kemResult.sharedSecret.bytes),
                "Wrong keys must produce a different shared secret",
            )
        }
        // decapResult.isErr() is also acceptable (explicit failure)
    }

    // ── AC-M01-5: Ed25519 sign/verify ────────────────────────────────────────

    @Test
    fun ed25519SignVerifyRoundTrip() {
        val kp = crypto.generateEd25519KeyPair()
        val message = "Hello, Phantm!".encodeToByteArray()
        val sig = crypto.sign(message, kp.privateKey)
        val result = crypto.verify(message, sig, kp.publicKey)
        assertTrue(result.isOk(), "Valid signature must verify successfully")
    }

    @Test
    fun ed25519SignatureIs64Bytes() {
        val kp = crypto.generateEd25519KeyPair()
        val sig = crypto.sign("test".encodeToByteArray(), kp.privateKey)
        assertEquals(64, sig.bytes.size, "Ed25519 detached signature must be 64 bytes")
    }

    @Test
    fun ed25519RejectsModifiedMessage() {
        val kp = crypto.generateEd25519KeyPair()
        val original = "Hello, Phantm!".encodeToByteArray()
        val sig = crypto.sign(original, kp.privateKey)
        val tampered = "Hello, world!".encodeToByteArray()
        val result = crypto.verify(tampered, sig, kp.publicKey)
        assertTrue(result.isErr(), "Modified message must fail verification")
        assertEquals(CryptoError.InvalidSignature, result.errorOrNull)
    }

    @Test
    fun ed25519RejectsWrongPublicKey() {
        val kp = crypto.generateEd25519KeyPair()
        val other = crypto.generateEd25519KeyPair()
        val sig = crypto.sign("Hello".encodeToByteArray(), kp.privateKey)
        val result = crypto.verify("Hello".encodeToByteArray(), sig, other.publicKey)
        assertTrue(result.isErr(), "Signature verified with wrong public key must fail")
    }

    @Test
    fun ed25519RejectsTamperedSignature() {
        val kp = crypto.generateEd25519KeyPair()
        val message = "Hello".encodeToByteArray()
        val sig = crypto.sign(message, kp.privateKey)
        val tampered = sig.bytes.copyOf().also { it[0] = (it[0].toInt() xor 0x01).toByte() }
        val result = crypto.verify(message, Signature(tampered), kp.publicKey)
        assertTrue(result.isErr(), "Tampered signature must fail verification")
    }

    // ── AC-M01-6: deriveKey determinism and output range ─────────────────────

    @Test
    fun deriveKeyIsDeterministic() {
        val ikm = crypto.randomBytes(32)
        val salt = crypto.randomBytes(32)
        val info = "test-context".encodeToByteArray()
        val key1 = crypto.deriveKey(ikm, salt, info)
        val key2 = crypto.deriveKey(ikm, salt, info)
        assertContentEquals(key1.bytes, key2.bytes, "deriveKey must be deterministic")
    }

    @Test
    fun deriveKeyProducesDifferentOutputForDifferentInfo() {
        val ikm = crypto.randomBytes(32)
        val salt = crypto.randomBytes(32)
        val key1 = crypto.deriveKey(ikm, salt, "context-a".encodeToByteArray())
        val key2 = crypto.deriveKey(ikm, salt, "context-b".encodeToByteArray())
        assertFalse(key1.bytes.contentEquals(key2.bytes), "Different info must produce different keys")
    }

    @Test
    fun deriveKeyProducesDifferentOutputForDifferentSalt() {
        val ikm = crypto.randomBytes(32)
        val info = "context".encodeToByteArray()
        val key1 = crypto.deriveKey(ikm, crypto.randomBytes(32), info)
        val key2 = crypto.deriveKey(ikm, crypto.randomBytes(32), info)
        assertFalse(key1.bytes.contentEquals(key2.bytes), "Different salts must produce different keys")
    }

    @Test
    fun deriveKeyOutputLengthIsRespected() {
        val ikm = crypto.randomBytes(32)
        val salt = crypto.randomBytes(32)
        val info = "ctx".encodeToByteArray()
        for (len in listOf(16, 24, 32, 48, 64)) {
            val key = crypto.deriveKey(ikm, salt, info, len)
            assertEquals(len, key.bytes.size, "deriveKey must produce exactly $len bytes")
        }
    }

    // ── AC-M01-7: No key material in toString ────────────────────────────────

    @Test
    fun privateKeyToStringIsRedacted() {
        val kp = crypto.generateX25519KeyPair()
        assertFalse(
            kp.privateKey.toString().contains(kp.privateKey.bytes.take(4).joinToString("")),
            "PrivateKey.toString() must not contain raw key bytes",
        )
        assertTrue(kp.privateKey.toString().contains("redacted"), "PrivateKey.toString() must say 'redacted'")
    }

    @Test
    fun sharedSecretToStringIsRedacted() {
        val x = crypto.generateX25519KeyPair()
        val m = crypto.generateMlKem768KeyPair()
        val secret = crypto.hybridEncapsulate(x.publicKey, m.publicKey).sharedSecret
        assertTrue(secret.toString().contains("redacted"), "SharedSecret.toString() must say 'redacted'")
    }

    @Test
    fun keyPairToStringDoesNotContainPrivateKey() {
        val kp = crypto.generateEd25519KeyPair()
        assertFalse(
            kp.toString().contains(kp.privateKey.bytes.take(8).map { it.toInt() }.toString()),
            "KeyPair.toString() must not expose private key bytes",
        )
    }

    // ── AC-M01-8: PhantmResult correctness ───────────────────────────────────

    @Test
    fun cryptoResultOkCarriesValue() {
        val key = SecretKey(crypto.randomBytes(32))
        val plaintext = "data".encodeToByteArray()
        val encrypted = crypto.secretBox(plaintext, key)
        val result = crypto.secretBoxOpen(encrypted, key)
        assertNotNull(result.valueOrNull)
        assertTrue(result is PhantmResult.Ok)
    }

    @Test
    fun cryptoResultErrCarriesError() {
        val key1 = SecretKey(crypto.randomBytes(32))
        val key2 = SecretKey(crypto.randomBytes(32))
        val encrypted = crypto.secretBox("data".encodeToByteArray(), key1)
        val result = crypto.secretBoxOpen(encrypted, key2)
        assertNotNull(result.errorOrNull)
        assertTrue(result is PhantmResult.Err)
    }

    // ── Randomness ────────────────────────────────────────────────────────────

    @Test
    fun randomBytesAreDistinct() {
        val samples = (1..50).map { crypto.randomBytes(32).toList() }.toSet()
        assertEquals(50, samples.size, "randomBytes must produce distinct values each call")
    }
}

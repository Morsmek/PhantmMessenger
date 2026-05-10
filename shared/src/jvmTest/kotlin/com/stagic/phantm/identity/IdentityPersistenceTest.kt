package com.stagic.phantm.identity

import com.stagic.phantm.crypto.createCryptoCore
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AC-M02-3: loadIdentity() returns the same public key after simulated app restart.
 * Uses JvmIdentityManager with a shared temp directory to simulate two app sessions.
 */
class IdentityPersistenceTest {

    private val crypto = createCryptoCore()

    @Test
    fun publicKeysAreStableAcrossRestarts() {
        val tmpDir = Files.createTempDirectory("phantm_m02_persist").toFile()
        try {
            val session1 = JvmIdentityManager(crypto, tmpDir)
            val created = runBlocking { session1.createIdentity() }.valueOrNull!!

            // Simulate app restart: new manager instance, same storage dir
            val session2 = JvmIdentityManager(crypto, tmpDir)
            val loaded = runBlocking { session2.loadIdentity() }.valueOrNull!!

            assertEquals(created.id, loaded.id, "Identity id must be stable across restarts")
            assertTrue(
                created.x25519PublicKey.bytes.contentEquals(loaded.x25519PublicKey.bytes),
                "X25519 public key must be identical after reload",
            )
            assertTrue(
                created.ed25519PublicKey.bytes.contentEquals(loaded.ed25519PublicKey.bytes),
                "Ed25519 public key must be identical after reload",
            )
            assertTrue(
                created.mlKem768PublicKey.bytes.contentEquals(loaded.mlKem768PublicKey.bytes),
                "ML-KEM-768 public key must be identical after reload",
            )
            assertEquals(created.fingerprint, loaded.fingerprint, "Fingerprint must be stable across restarts")
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun privateKeysAreStableAcrossRestarts() {
        val tmpDir = Files.createTempDirectory("phantm_m02_privkey_persist").toFile()
        try {
            val session1 = JvmIdentityManager(crypto, tmpDir)
            runBlocking { session1.createIdentity() }
            val keys1 = runBlocking { session1.loadPrivateKeys() }.valueOrNull!!

            val session2 = JvmIdentityManager(crypto, tmpDir)
            val keys2 = runBlocking { session2.loadPrivateKeys() }.valueOrNull!!

            assertTrue(
                keys1.x25519PrivKey.bytes.contentEquals(keys2.x25519PrivKey.bytes),
                "X25519 private key must decrypt to same bytes across restarts",
            )
            assertTrue(
                keys1.ed25519PrivKey.bytes.contentEquals(keys2.ed25519PrivKey.bytes),
                "Ed25519 private key must decrypt to same bytes across restarts",
            )

            keys1.zero()
            keys2.zero()
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun privateKeyRoundTripWithCryptoOps() {
        val tmpDir = Files.createTempDirectory("phantm_m02_ops").toFile()
        try {
            val session1 = JvmIdentityManager(crypto, tmpDir)
            val identity = runBlocking { session1.createIdentity() }.valueOrNull!!
            val keys1 = runBlocking { session1.loadPrivateKeys() }.valueOrNull!!

            // Verify Ed25519 sign → verify round-trip survives storage/reload
            val message = "Phantm M02 persistence test".encodeToByteArray()
            val sig = crypto.sign(message, keys1.ed25519PrivKey)
            val verifyResult = crypto.verify(message, sig, identity.ed25519PublicKey)
            assertTrue(verifyResult.isOk(), "Signature from stored private key must verify with stored public key")

            keys1.zero()
        } finally {
            tmpDir.deleteRecursively()
        }
    }
}

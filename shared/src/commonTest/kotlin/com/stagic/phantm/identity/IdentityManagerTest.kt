package com.stagic.phantm.identity

import com.stagic.phantm.crypto.createCryptoCore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Covers AC-M02-1, AC-M02-2, AC-M02-4, AC-M02-5 (single-session cases).
 * Persistence across "restarts" (AC-M02-3) lives in jvmTest where JvmIdentityManager
 * can be directly instantiated with a shared temp directory.
 */
class IdentityManagerTest {

    private val crypto = createCryptoCore()
    private val context = PlatformContext()

    private fun manager() = createIdentityManager(crypto, context)

    // ── AC-M02-1: Identity creation generates all three keypair types ─────────

    @Test
    fun createIdentityGeneratesAllThreeKeyTypes() = runTest {
        val result = manager().createIdentity()
        assertTrue(result.isOk(), "createIdentity must succeed: ${result.errorOrNull}")
        val id = result.valueOrNull!!
        assertEquals(32, id.x25519PublicKey.bytes.size, "X25519 pub key must be 32 bytes")
        assertEquals(1184, id.mlKem768PublicKey.bytes.size, "ML-KEM-768 pub key must be 1184 bytes")
        assertEquals(32, id.ed25519PublicKey.bytes.size, "Ed25519 pub key must be 32 bytes")
        assertTrue(id.id.isNotBlank())
    }

    @Test
    fun createIdentityFingerprintMatchesBlake2bOfEd25519Pub() = runTest {
        val id = manager().createIdentity().valueOrNull!!
        val expected = computeIdentityFingerprint(crypto, id.ed25519PublicKey)
        assertEquals(expected, id.fingerprint)
    }

    @Test
    fun fingerprintIs64CharHexString() = runTest {
        val id = manager().createIdentity().valueOrNull!!
        assertEquals(64, id.fingerprint.length)
        assertTrue(id.fingerprint.all { it.isDigit() || it in 'a'..'f' })
    }

    // ── AC-M02-2: Private keys not exposed in Identity ────────────────────────

    @Test
    fun identityDataClassHasNoPrivateKeyFields() = runTest {
        val id = manager().createIdentity().valueOrNull!!
        val fieldNames = id::class.members.map { it.name }
        assertFalse(fieldNames.any { "priv" in it.lowercase() },
            "Identity must not expose any private key field")
    }

    @Test
    fun privateKeysAccessibleOnlyViaLoadPrivateKeys() = runTest {
        val m = manager()
        m.createIdentity()
        val privResult = m.loadPrivateKeys()
        assertTrue(privResult.isOk())
        val keys = privResult.valueOrNull!!
        assertTrue(keys.x25519PrivKey.bytes.isNotEmpty())
        assertTrue(keys.ed25519PrivKey.bytes.isNotEmpty())
        assertTrue(keys.mlKemPrivKey.bytes.isNotEmpty())
        keys.zero()
    }

    @Test
    fun identityPrivateKeysToStringIsRedacted() = runTest {
        val m = manager()
        m.createIdentity()
        val keys = m.loadPrivateKeys().valueOrNull!!
        assertTrue(keys.toString().contains("redacted"))
        keys.zero()
    }

    @Test
    fun zeroWipesPrivateKeyBytes() = runTest {
        val m = manager()
        m.createIdentity()
        val keys = m.loadPrivateKeys().valueOrNull!!
        keys.zero()
        assertTrue(keys.x25519PrivKey.bytes.all { it == 0.toByte() }, "x25519 must be zeroed")
        assertTrue(keys.ed25519PrivKey.bytes.all { it == 0.toByte() }, "ed25519 must be zeroed")
        assertTrue(keys.mlKemPrivKey.bytes.all { it == 0.toByte() }, "mlKem must be zeroed")
    }

    // ── AC-M02-4: No raw export via loadIdentity ──────────────────────────────

    @Test
    fun loadIdentityFailsWhenNoneExists() = runTest {
        val result = manager().loadIdentity()
        assertTrue(result.isErr())
        assertEquals(IdentityError.NoIdentityFound, result.errorOrNull)
    }

    @Test
    fun loadPrivateKeysFailsWhenNoneExists() = runTest {
        val result = manager().loadPrivateKeys()
        assertTrue(result.isErr())
        assertEquals(IdentityError.NoIdentityFound, result.errorOrNull)
    }

    // ── AC-M02-5: Fingerprint differs across identities ───────────────────────

    @Test
    fun fingerprintDiffersForDifferentIdentities() = runTest {
        val fp1 = manager().createIdentity().valueOrNull!!.fingerprint
        val fp2 = manager().createIdentity().valueOrNull!!.fingerprint
        assertNotEquals(fp1, fp2)
    }

    // ── hasIdentity / destroyIdentity ─────────────────────────────────────────

    @Test
    fun hasIdentityReturnsFalseInitially() = runTest {
        assertFalse(manager().hasIdentity())
    }

    @Test
    fun hasIdentityReturnsTrueAfterCreate() = runTest {
        val m = manager()
        m.createIdentity()
        assertTrue(m.hasIdentity())
    }

    @Test
    fun destroyIdentityClearsState() = runTest {
        val m = manager()
        m.createIdentity()
        assertTrue(m.hasIdentity())
        val result = m.destroyIdentity()
        assertTrue(result.isOk())
        assertFalse(m.hasIdentity())
    }
}

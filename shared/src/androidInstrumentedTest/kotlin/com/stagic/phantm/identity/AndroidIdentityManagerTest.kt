package com.stagic.phantm.identity

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.createCryptoCore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/**
 * AC-M02-6: Android Keystore identity instrumented tests.
 *
 * Verifies that [AndroidIdentityManager] correctly stores and retrieves identity keys
 * using the Android Keystore on a real device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class AndroidIdentityManagerTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val crypto = createCryptoCore()
    private lateinit var manager: IdentityManager

    @Before
    fun setUp() {
        manager = createIdentityManager(crypto, context)
        context.getSharedPreferences("phantm_identity", Context.MODE_PRIVATE)
            .edit().clear().apply()
        deleteKeystoreAlias("phantm_identity_key")
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("phantm_identity", Context.MODE_PRIVATE)
            .edit().clear().apply()
        deleteKeystoreAlias("phantm_identity_key")
    }

    @Test
    fun createIdentity_succeeds_and_keySizesCorrect() = runBlocking {
        val result = manager.createIdentity()
        assertTrue("createIdentity must succeed", result is PhantmResult.Ok)
        val identity = (result as PhantmResult.Ok).value

        assertEquals("X25519 public key must be 32 bytes", 32, identity.x25519PublicKey.bytes.size)
        assertEquals("ML-KEM-768 public key must be 1184 bytes", 1184, identity.mlKem768PublicKey.bytes.size)
        assertEquals("Ed25519 public key must be 32 bytes", 32, identity.ed25519PublicKey.bytes.size)
        assertEquals("Fingerprint must be 64 hex chars", 64, identity.fingerprint.length)
        assertTrue(
            "Fingerprint must be lowercase hex",
            identity.fingerprint.all { it.isDigit() || it in 'a'..'f' },
        )
    }

    @Test
    fun createIdentity_createsKeystoreEntry() = runBlocking {
        manager.createIdentity()

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertTrue(
            "AndroidKeyStore must contain alias phantm_identity_key after createIdentity",
            keyStore.containsAlias("phantm_identity_key"),
        )
    }

    @Test
    fun loadIdentity_returnsSamePublicKeys() = runBlocking {
        val created = (manager.createIdentity() as PhantmResult.Ok).value
        val loaded = (manager.loadIdentity() as PhantmResult.Ok).value

        assertArrayEquals(
            "X25519 public key must survive round-trip",
            created.x25519PublicKey.bytes,
            loaded.x25519PublicKey.bytes,
        )
        assertArrayEquals(
            "ML-KEM public key must survive round-trip",
            created.mlKem768PublicKey.bytes,
            loaded.mlKem768PublicKey.bytes,
        )
        assertArrayEquals(
            "Ed25519 public key must survive round-trip",
            created.ed25519PublicKey.bytes,
            loaded.ed25519PublicKey.bytes,
        )
        assertEquals("Fingerprint must survive round-trip", created.fingerprint, loaded.fingerprint)
    }

    @Test
    fun loadPrivateKeys_returnsNonEmptyKeys() = runBlocking {
        manager.createIdentity()
        val privKeys = (manager.loadPrivateKeys() as PhantmResult.Ok).value

        assertTrue("X25519 private key must be non-empty", privKeys.x25519PrivKey.bytes.isNotEmpty())
        assertTrue("Ed25519 private key must be non-empty", privKeys.ed25519PrivKey.bytes.isNotEmpty())
        assertTrue("ML-KEM private key must be non-empty", privKeys.mlKemPrivKey.bytes.isNotEmpty())
        privKeys.zero()
    }

    @Test
    fun destroyIdentity_deletesKeystoreEntry() = runBlocking {
        manager.createIdentity()
        manager.destroyIdentity()

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertFalse(
            "Keystore alias must be removed after destroyIdentity",
            keyStore.containsAlias("phantm_identity_key"),
        )
        assertFalse("hasIdentity must return false after destroyIdentity", manager.hasIdentity())
    }

    private fun deleteKeystoreAlias(alias: String) {
        runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (ks.containsAlias(alias)) ks.deleteEntry(alias)
        }
    }
}

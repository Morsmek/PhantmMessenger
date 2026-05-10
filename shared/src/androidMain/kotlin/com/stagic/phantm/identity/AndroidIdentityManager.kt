package com.stagic.phantm.identity

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.crypto.PrivateKey
import com.stagic.phantm.crypto.PublicKey
import com.stagic.phantm.err
import com.stagic.phantm.ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android implementation of [IdentityManager].
 *
 * Private keys are encrypted with an AES-256-GCM key that lives in the Android Keystore
 * (hardware-backed where available). Encrypted ciphertext is stored in SharedPreferences.
 * Private key bytes are never written to disk in plaintext.
 */
internal class AndroidIdentityManager(
    private val crypto: CryptoCore,
    private val context: Context,
) : IdentityManager {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun createIdentity(): IdentityResult<Identity> = withContext(Dispatchers.IO) {
        runCatching {
            ensureKeystoreKey()

            val x25519Kp = crypto.generateX25519KeyPair()
            val ed25519Kp = crypto.generateEd25519KeyPair()
            val mlKemKp = crypto.generateMlKem768KeyPair()

            val id = UUID.randomUUID().toString()
            val fingerprint = computeIdentityFingerprint(crypto, ed25519Kp.publicKey)

            prefs.edit()
                .putString(KEY_ID, id)
                .putString(KEY_X25519_PUB, x25519Kp.publicKey.bytes.encodeBase64())
                .putString(KEY_ED25519_PUB, ed25519Kp.publicKey.bytes.encodeBase64())
                .putString(KEY_MLKEM_PUB, mlKemKp.publicKey.bytes.encodeBase64())
                .putString(KEY_FINGERPRINT, fingerprint)
                .putString(KEY_X25519_PRIV_ENC, encryptWithKeystore(x25519Kp.privateKey.bytes).encodeBase64())
                .putString(KEY_ED25519_PRIV_ENC, encryptWithKeystore(ed25519Kp.privateKey.bytes).encodeBase64())
                .putString(KEY_MLKEM_PRIV_ENC, encryptWithKeystore(mlKemKp.privateKey.bytes).encodeBase64())
                .apply()

            // Zero in-memory private key bytes after persisting
            x25519Kp.privateKey.bytes.fill(0)
            ed25519Kp.privateKey.bytes.fill(0)
            mlKemKp.privateKey.bytes.fill(0)

            Identity(
                id = id,
                x25519PublicKey = x25519Kp.publicKey,
                mlKem768PublicKey = mlKemKp.publicKey,
                ed25519PublicKey = ed25519Kp.publicKey,
                fingerprint = fingerprint,
            )
        }.fold(onSuccess = { it.ok() }, onFailure = { mapException(it).err() })
    }

    override suspend fun loadIdentity(): IdentityResult<Identity> = withContext(Dispatchers.IO) {
        val id = prefs.getString(KEY_ID, null)
            ?: return@withContext IdentityError.NoIdentityFound.err()
        runCatching {
            Identity(
                id = id,
                x25519PublicKey = PublicKey(prefs.getStringOrThrow(KEY_X25519_PUB).decodeBase64()),
                mlKem768PublicKey = PublicKey(prefs.getStringOrThrow(KEY_MLKEM_PUB).decodeBase64()),
                ed25519PublicKey = PublicKey(prefs.getStringOrThrow(KEY_ED25519_PUB).decodeBase64()),
                fingerprint = prefs.getStringOrThrow(KEY_FINGERPRINT),
            )
        }.fold(onSuccess = { it.ok() }, onFailure = { mapException(it).err() })
    }

    override suspend fun hasIdentity(): Boolean = withContext(Dispatchers.IO) {
        prefs.contains(KEY_ID)
    }

    override suspend fun destroyIdentity(): IdentityResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Delete the Keystore key — makes all encrypted private key blobs permanently unreadable
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_ALIAS)
            }
            prefs.edit().clear().apply()
        }.fold(onSuccess = { Unit.ok() }, onFailure = { mapException(it).err() })
    }

    override suspend fun loadPrivateKeys(): IdentityResult<IdentityPrivateKeys> = withContext(Dispatchers.IO) {
        if (!hasIdentity()) return@withContext IdentityError.NoIdentityFound.err()
        runCatching {
            IdentityPrivateKeys(
                x25519PrivKey = PrivateKey(decryptWithKeystore(prefs.getStringOrThrow(KEY_X25519_PRIV_ENC).decodeBase64())),
                ed25519PrivKey = PrivateKey(decryptWithKeystore(prefs.getStringOrThrow(KEY_ED25519_PRIV_ENC).decodeBase64())),
                mlKemPrivKey = PrivateKey(decryptWithKeystore(prefs.getStringOrThrow(KEY_MLKEM_PRIV_ENC).decodeBase64())),
            )
        }.fold(onSuccess = { it.ok() }, onFailure = { mapException(it).err() })
    }

    // ── Android Keystore helpers ──────────────────────────────────────────────

    private fun ensureKeystoreKey() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) return
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        keyGen.generateKey()
    }

    /** Returns [iv (12 bytes)] + [GCM ciphertext + 16-byte auth tag]. */
    private fun encryptWithKeystore(plaintext: ByteArray): ByteArray {
        val key = loadKeystoreKey()
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    private fun decryptWithKeystore(data: ByteArray): ByteArray {
        val iv = data.copyOf(GCM_IV_BYTES)
        val ciphertext = data.copyOfRange(GCM_IV_BYTES, data.size)
        val key = loadKeystoreKey()
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun loadKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)
            ?: throw IllegalStateException("Identity Keystore key missing — was destroyIdentity() called?")
    }

    private fun mapException(t: Throwable): IdentityError = when {
        t.message?.contains("Keystore", ignoreCase = true) == true -> IdentityError.SecureElementUnavailable
        else -> IdentityError.Unknown(t)
    }

    private fun android.content.SharedPreferences.getStringOrThrow(key: String): String =
        getString(key, null) ?: error("Missing identity field: $key")

    private companion object {
        const val PREFS_NAME = "phantm_identity"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEYSTORE_ALIAS = "phantm_identity_key"
        const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128

        const val KEY_ID = "identity_id"
        const val KEY_X25519_PUB = "identity_x25519_pub"
        const val KEY_ED25519_PUB = "identity_ed25519_pub"
        const val KEY_MLKEM_PUB = "identity_mlkem_pub"
        const val KEY_FINGERPRINT = "identity_fingerprint"
        const val KEY_X25519_PRIV_ENC = "identity_x25519_priv_enc"
        const val KEY_ED25519_PRIV_ENC = "identity_ed25519_priv_enc"
        const val KEY_MLKEM_PRIV_ENC = "identity_mlkem_priv_enc"
    }
}

private fun ByteArray.encodeBase64(): String =
    java.util.Base64.getEncoder().encodeToString(this)

private fun String.decodeBase64(): ByteArray =
    java.util.Base64.getDecoder().decode(this)

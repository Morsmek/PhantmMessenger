package com.stagic.phantm.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.identity.PlatformContext
import net.zetetic.database.sqlcipher.SupportFactory
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Derives a 32-byte database passphrase from the Android Keystore:
 *  1. Generates a hardware-backed AES-256-GCM key on first run.
 *  2. Generates and encrypts a random 32-byte passphrase on first run.
 *  3. Persists the encrypted passphrase blob in SharedPreferences.
 *  4. Decrypts on subsequent runs and passes to SQLCipher's [SupportFactory].
 *
 * SQLCipher uses the passphrase via PBKDF2 to derive the actual AES-256 database key.
 */
actual fun createSqlDriver(context: PlatformContext, crypto: CryptoCore): SqlDriver {
    System.loadLibrary("sqlcipher")
    val passphrase = loadOrCreatePassphrase(context, crypto)
    val factory = SupportFactory(passphrase)
    return AndroidSqliteDriver(
        schema = PhantmDatabase.Schema,
        context = context,
        name = DB_NAME,
        factory = factory,
    )
}

// ── Passphrase management ─────────────────────────────────────────────────────

private fun loadOrCreatePassphrase(context: Context, crypto: CryptoCore): ByteArray {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val stored = prefs.getString(KEY_PASSPHRASE_ENC, null)
    if (stored != null) {
        return decryptWithKeystore(stored.decodeBase64())
    }
    ensureKeystoreKey()
    val passphrase = crypto.randomBytes(32)
    val encrypted = encryptWithKeystore(passphrase)
    prefs.edit().putString(KEY_PASSPHRASE_ENC, encrypted.encodeBase64()).apply()
    return passphrase
}

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
    val key = keystoreKey()
    val cipher = Cipher.getInstance(AES_GCM)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    return cipher.iv + cipher.doFinal(plaintext)
}

private fun decryptWithKeystore(data: ByteArray): ByteArray {
    val iv = data.copyOf(GCM_IV_BYTES)
    val ciphertext = data.copyOfRange(GCM_IV_BYTES, data.size)
    val key = keystoreKey()
    val cipher = Cipher.getInstance(AES_GCM)
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    return cipher.doFinal(ciphertext)
}

private fun keystoreKey(): SecretKey {
    val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
}

private fun ByteArray.encodeBase64(): String = java.util.Base64.getEncoder().encodeToString(this)
private fun String.decodeBase64(): ByteArray = java.util.Base64.getDecoder().decode(this)

private const val DB_NAME = "phantm.db"
private const val PREFS_NAME = "phantm_db_config"
private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEYSTORE_ALIAS = "phantm_db_passphrase_key"
private const val AES_GCM = "AES/GCM/NoPadding"
private const val GCM_IV_BYTES = 12
private const val GCM_TAG_BITS = 128
private const val KEY_PASSPHRASE_ENC = "db_passphrase_enc"

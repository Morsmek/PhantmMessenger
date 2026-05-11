package com.stagic.phantm.identity

import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.crypto.PrivateKey
import com.stagic.phantm.crypto.PublicKey
import com.stagic.phantm.err
import com.stagic.phantm.ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * JVM test implementation of [IdentityManager].
 *
 * Uses a deterministic software AES-256-GCM key (derived from a fixed constant via M01)
 * for private key encryption and a [Properties] file for persistence.
 * This implementation exists solely for unit testing — never ship to production.
 */
internal class JvmIdentityManager(
    private val crypto: CryptoCore,
    storageDir: File = java.nio.file.Files.createTempDirectory("phantm_identity_").toFile(),
) : IdentityManager {

    private val propsFile = storageDir.also { it.mkdirs() }.resolve("identity.properties")

    // Deterministic AES-256 key derived from a fixed constant — stable across JVM restarts.
    private val aesKey: SecretKey by lazy {
        val keyBytes = crypto.deriveKey(
            ikm = "phantm-jvm-identity-wrap-key-not-for-production".encodeToByteArray(),
            salt = ByteArray(32),
            info = "jvm-identity-aes-v1".encodeToByteArray(),
            outputLen = 32,
        ).bytes
        SecretKeySpec(keyBytes, "AES")
    }

    override suspend fun createIdentity(): IdentityResult<Identity> = withContext(Dispatchers.IO) {
        runCatching {
            val x25519Kp = crypto.generateX25519KeyPair()
            val ed25519Kp = crypto.generateEd25519KeyPair()
            val mlKemKp = crypto.generateMlKem768KeyPair()

            val id = UUID.randomUUID().toString()
            val fingerprint = computeIdentityFingerprint(crypto, ed25519Kp.publicKey)

            val props = loadProps()
            props[KEY_ID] = id
            props[KEY_X25519_PUB] = x25519Kp.publicKey.bytes.encodeBase64()
            props[KEY_ED25519_PUB] = ed25519Kp.publicKey.bytes.encodeBase64()
            props[KEY_MLKEM_PUB] = mlKemKp.publicKey.bytes.encodeBase64()
            props[KEY_FINGERPRINT] = fingerprint
            props[KEY_X25519_PRIV_ENC] = encryptBytes(x25519Kp.privateKey.bytes).encodeBase64()
            props[KEY_ED25519_PRIV_ENC] = encryptBytes(ed25519Kp.privateKey.bytes).encodeBase64()
            props[KEY_MLKEM_PRIV_ENC] = encryptBytes(mlKemKp.privateKey.bytes).encodeBase64()
            saveProps(props)

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
        }.fold(onSuccess = { it.ok() }, onFailure = { IdentityError.Unknown(it).err() })
    }

    override suspend fun loadIdentity(): IdentityResult<Identity> = withContext(Dispatchers.IO) {
        val props = loadProps()
        val id = props[KEY_ID] as? String
            ?: return@withContext IdentityError.NoIdentityFound.err()
        runCatching {
            Identity(
                id = id,
                x25519PublicKey = PublicKey(props.getStringOrThrow(KEY_X25519_PUB).decodeBase64()),
                mlKem768PublicKey = PublicKey(props.getStringOrThrow(KEY_MLKEM_PUB).decodeBase64()),
                ed25519PublicKey = PublicKey(props.getStringOrThrow(KEY_ED25519_PUB).decodeBase64()),
                fingerprint = props.getStringOrThrow(KEY_FINGERPRINT),
            )
        }.fold(onSuccess = { it.ok() }, onFailure = { IdentityError.Unknown(it).err() })
    }

    override suspend fun hasIdentity(): Boolean = withContext(Dispatchers.IO) {
        loadProps().containsKey(KEY_ID)
    }

    override suspend fun destroyIdentity(): IdentityResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            propsFile.delete()
        }.fold(onSuccess = { Unit.ok() }, onFailure = { IdentityError.Unknown(it).err() })
    }

    override suspend fun loadPrivateKeys(): IdentityResult<IdentityPrivateKeys> = withContext(Dispatchers.IO) {
        if (!hasIdentity()) return@withContext IdentityError.NoIdentityFound.err()
        val props = loadProps()
        runCatching {
            IdentityPrivateKeys(
                x25519PrivKey = PrivateKey(decryptBytes(props.getStringOrThrow(KEY_X25519_PRIV_ENC).decodeBase64())),
                ed25519PrivKey = PrivateKey(decryptBytes(props.getStringOrThrow(KEY_ED25519_PRIV_ENC).decodeBase64())),
                mlKemPrivKey = PrivateKey(decryptBytes(props.getStringOrThrow(KEY_MLKEM_PRIV_ENC).decodeBase64())),
            )
        }.fold(onSuccess = { it.ok() }, onFailure = { IdentityError.Unknown(it).err() })
    }

    // ── AES-256-GCM helpers ───────────────────────────────────────────────────

    /** Returns [iv (12 bytes)] + [GCM ciphertext + 16-byte auth tag]. */
    private fun encryptBytes(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    private fun decryptBytes(data: ByteArray): ByteArray {
        val iv = data.copyOf(GCM_IV_BYTES)
        val ciphertext = data.copyOfRange(GCM_IV_BYTES, data.size)
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    // ── Persistence helpers ───────────────────────────────────────────────────

    private fun loadProps(): Properties = Properties().also {
        if (propsFile.exists()) propsFile.inputStream().use(it::load)
    }

    private fun saveProps(props: Properties) {
        propsFile.outputStream().use { props.store(it, null) }
    }

    private fun Properties.getStringOrThrow(key: String): String =
        getProperty(key) ?: error("Missing identity field: $key")

    private companion object {
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

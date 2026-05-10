package com.stagic.phantm.panic

import android.content.Context
import com.stagic.phantm.err
import com.stagic.phantm.identity.IdentityManager
import com.stagic.phantm.ok
import java.io.File
import java.security.KeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidPanicManager(
    private val identityManager: IdentityManager,
    private val context: Context,
) : PanicManager {

    private val panicPrefs by lazy {
        context.getSharedPreferences(PANIC_PREFS_NAME, Context.MODE_PRIVATE)
    }

    override val isDecoyModeActive: Boolean
        get() = panicPrefs.getBoolean(KEY_DECOY_MODE, false)

    /**
     * Wipe sequence (AC-M12-1, AC-M12-2, AC-M12-5):
     * 1. Delete identity Keystore key → identity private key blobs permanently unreadable
     * 2. Delete DB passphrase Keystore key → SQLCipher DB permanently inaccessible
     * 3. Clear both SharedPreferences stores
     * 4. Delete SQLCipher DB files (phantm.db / -wal / -shm)
     */
    override suspend fun triggerPanicWipe(): PanicResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            identityManager.destroyIdentity()

            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(DB_KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(DB_KEYSTORE_ALIAS)
            }

            context.getSharedPreferences(DB_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()

            val dbFile = context.getDatabasePath(DB_NAME)
            listOf(dbFile, File("${dbFile.path}-wal"), File("${dbFile.path}-shm"))
                .forEach { it.delete() }

            panicPrefs.edit().clear().apply()
        }.fold(
            onSuccess = { Unit.ok() },
            onFailure = { PanicError.WipeFailed(it.message ?: "Unknown error").err() },
        )
    }

    override suspend fun activateDecoyMode(): PanicResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            panicPrefs.edit().putBoolean(KEY_DECOY_MODE, true).apply()
        }.fold(
            onSuccess = { Unit.ok() },
            onFailure = { PanicError.WipeFailed(it.message ?: "Failed to activate decoy mode").err() },
        )
    }

    override suspend fun configurePanicTrigger(config: PanicTriggerConfig): PanicResult<Unit> {
        if (config.wrongPinCountThreshold < 1) {
            return PanicError.ConfigError("wrongPinCountThreshold must be >= 1").err()
        }
        return runCatching {
            panicPrefs.edit()
                .putString(KEY_TRIGGER_TYPE, config.type.name)
                .putInt(KEY_PIN_THRESHOLD, config.wrongPinCountThreshold)
                .also { editor ->
                    if (config.pin != null) editor.putString(KEY_PIN, config.pin)
                    else editor.remove(KEY_PIN)
                }
                .apply()
        }.fold(
            onSuccess = { Unit.ok() },
            onFailure = { PanicError.ConfigError(it.message ?: "Failed to save config").err() },
        )
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val DB_KEYSTORE_ALIAS = "phantm_db_passphrase_key"
        const val DB_PREFS_NAME = "phantm_db_config"
        const val DB_NAME = "phantm.db"
        const val PANIC_PREFS_NAME = "phantm_panic"
        const val KEY_DECOY_MODE = "decoy_mode"
        const val KEY_TRIGGER_TYPE = "trigger_type"
        const val KEY_PIN_THRESHOLD = "pin_threshold"
        const val KEY_PIN = "panic_pin"
    }
}

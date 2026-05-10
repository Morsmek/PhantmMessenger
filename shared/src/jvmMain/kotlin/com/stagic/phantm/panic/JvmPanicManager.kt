package com.stagic.phantm.panic

import com.stagic.phantm.err
import com.stagic.phantm.identity.IdentityManager
import com.stagic.phantm.ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JVM test implementation of [PanicManager].
 *
 * Delegates identity destruction to [IdentityManager.destroyIdentity].
 * Decoy mode and trigger config are held in-memory only — no file I/O.
 * This implementation exists solely for unit testing.
 */
internal class JvmPanicManager(
    private val identityManager: IdentityManager,
) : PanicManager {

    @Volatile
    override var isDecoyModeActive: Boolean = false
        private set

    private var triggerConfig: PanicTriggerConfig = PanicTriggerConfig()

    override suspend fun triggerPanicWipe(): PanicResult<Unit> = withContext(Dispatchers.IO) {
        val result = identityManager.destroyIdentity()
        if (result.isOk()) Unit.ok()
        else PanicError.WipeFailed("Identity destroy failed").err()
    }

    override suspend fun activateDecoyMode(): PanicResult<Unit> {
        isDecoyModeActive = true
        return Unit.ok()
    }

    override suspend fun configurePanicTrigger(config: PanicTriggerConfig): PanicResult<Unit> {
        if (config.wrongPinCountThreshold < 1) {
            return PanicError.ConfigError("wrongPinCountThreshold must be >= 1").err()
        }
        triggerConfig = config
        return Unit.ok()
    }
}

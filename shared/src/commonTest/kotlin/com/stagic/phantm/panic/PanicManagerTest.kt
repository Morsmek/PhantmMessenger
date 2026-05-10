package com.stagic.phantm.panic

import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.createCryptoCore
import com.stagic.phantm.identity.Identity
import com.stagic.phantm.identity.IdentityError
import com.stagic.phantm.identity.IdentityManager
import com.stagic.phantm.identity.IdentityPrivateKeys
import com.stagic.phantm.identity.IdentityResult
import com.stagic.phantm.ok
import com.stagic.phantm.err
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.measureTime

/**
 * Covers AC-M12-1 through AC-M12-5 using the JVM PanicManager implementation.
 */
class PanicManagerTest {

    private val fakeIdentity = FakeIdentityManager()
    private val manager = JvmPanicManager(fakeIdentity)

    // ── AC-M12-1: wipe within 3 s ────────────────────────────────────────────

    @Test
    fun triggerPanicWipe_completesWithinThreeSeconds() = runTest {
        val elapsed = measureTime { manager.triggerPanicWipe() }
        assertTrue(elapsed.inWholeMilliseconds < 3_000, "Wipe took ${elapsed.inWholeMilliseconds} ms — must be < 3000 ms")
    }

    // ── AC-M12-2: identity key destruction ───────────────────────────────────

    @Test
    fun triggerPanicWipe_destroysIdentity() = runTest {
        fakeIdentity.hasData = true
        manager.triggerPanicWipe()
        assertFalse(fakeIdentity.hasData, "destroyIdentity() must have been called")
        assertTrue(fakeIdentity.destroyCalled)
    }

    @Test
    fun triggerPanicWipe_returnsOk() = runTest {
        val result = manager.triggerPanicWipe()
        assertIs<PhantmResult.Ok<Unit>>(result)
    }

    @Test
    fun triggerPanicWipe_returnsWipeFailedWhenIdentityDestroyFails() = runTest {
        fakeIdentity.failDestroy = true
        val result = manager.triggerPanicWipe()
        assertIs<PhantmResult.Err<PanicError>>(result)
        assertIs<PanicError.WipeFailed>(result.error)
    }

    // ── AC-M12-3: decoy mode ─────────────────────────────────────────────────

    @Test
    fun activateDecoyMode_setsIsDecoyModeActive() = runTest {
        assertFalse(manager.isDecoyModeActive)
        val result = manager.activateDecoyMode()
        assertIs<PhantmResult.Ok<Unit>>(result)
        assertTrue(manager.isDecoyModeActive)
    }

    @Test
    fun activateDecoyMode_doesNotDestroyIdentity() = runTest {
        fakeIdentity.hasData = true
        manager.activateDecoyMode()
        assertFalse(fakeIdentity.destroyCalled, "activateDecoyMode must not call destroyIdentity")
        assertTrue(fakeIdentity.hasData)
    }

    // ── AC-M12-4: configurable trigger ───────────────────────────────────────

    @Test
    fun configurePanicTrigger_acceptsValidConfig() = runTest {
        val result = manager.configurePanicTrigger(
            PanicTriggerConfig(
                type = TriggerType.WRONG_PIN_COUNT,
                wrongPinCountThreshold = 3,
                pin = "1234",
            ),
        )
        assertIs<PhantmResult.Ok<Unit>>(result)
    }

    @Test
    fun configurePanicTrigger_acceptsNoneTriggerType() = runTest {
        val result = manager.configurePanicTrigger(PanicTriggerConfig(type = TriggerType.NONE))
        assertIs<PhantmResult.Ok<Unit>>(result)
    }

    @Test
    fun configurePanicTrigger_rejectsZeroThreshold() = runTest {
        val result = manager.configurePanicTrigger(
            PanicTriggerConfig(wrongPinCountThreshold = 0),
        )
        assertIs<PhantmResult.Err<PanicError>>(result)
        assertIs<PanicError.ConfigError>(result.error)
    }

    @Test
    fun configurePanicTrigger_rejectsNegativeThreshold() = runTest {
        val result = manager.configurePanicTrigger(
            PanicTriggerConfig(wrongPinCountThreshold = -1),
        )
        assertIs<PhantmResult.Err<PanicError>>(result)
        assertIs<PanicError.ConfigError>(result.error)
    }

    // ── AC-M12-5: post-wipe no plaintext recoverable ─────────────────────────

    @Test
    fun afterWipe_identityNoLongerExists() = runTest {
        fakeIdentity.hasData = true
        manager.triggerPanicWipe()
        assertFalse(fakeIdentity.hasIdentity())
    }

    @Test
    fun afterWipe_loadIdentityReturnsNoIdentityFound() = runTest {
        fakeIdentity.hasData = true
        manager.triggerPanicWipe()
        val result = fakeIdentity.loadIdentity()
        assertIs<PhantmResult.Err<IdentityError>>(result)
        assertIs<IdentityError.NoIdentityFound>(result.error)
    }

    @Test
    fun consecutiveWipesAreSafe() = runTest {
        manager.triggerPanicWipe()
        val result = manager.triggerPanicWipe()
        assertIs<PhantmResult.Ok<Unit>>(result)
    }
}

// ── Test double ───────────────────────────────────────────────────────────────

private class FakeIdentityManager : IdentityManager {
    var hasData: Boolean = false
    var destroyCalled: Boolean = false
    var failDestroy: Boolean = false

    override suspend fun createIdentity(): IdentityResult<Identity> =
        IdentityError.Unknown(UnsupportedOperationException()).err()

    override suspend fun loadIdentity(): IdentityResult<Identity> =
        if (hasData) IdentityError.Unknown(UnsupportedOperationException()).err()
        else IdentityError.NoIdentityFound.err()

    override suspend fun hasIdentity(): Boolean = hasData

    override suspend fun destroyIdentity(): IdentityResult<Unit> {
        if (failDestroy) return IdentityError.Unknown(RuntimeException("destroy failed")).err()
        destroyCalled = true
        hasData = false
        return Unit.ok()
    }

    override suspend fun loadPrivateKeys(): IdentityResult<IdentityPrivateKeys> =
        IdentityError.Unknown(UnsupportedOperationException()).err()
}

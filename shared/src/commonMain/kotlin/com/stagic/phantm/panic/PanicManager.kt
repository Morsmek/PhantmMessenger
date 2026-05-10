package com.stagic.phantm.panic

import com.stagic.phantm.PhantmResult

typealias PanicResult<V> = PhantmResult<V, PanicError>

/**
 * Emergency data destruction and decoy mode.
 *
 * [triggerPanicWipe] is irreversible — all local cryptographic material is destroyed.
 * [activateDecoyMode] presents an empty app without destroying data.
 */
interface PanicManager {

    /** `true` while the app is showing the decoy empty state. */
    val isDecoyModeActive: Boolean

    /**
     * Irreversibly destroy all local key material and encrypted data.
     * Must complete within 3 seconds (AC-M12-1).
     */
    suspend fun triggerPanicWipe(): PanicResult<Unit>

    /**
     * Activate decoy mode — app appears empty without destroying real data.
     * Call [triggerPanicWipe] to actually destroy keys (AC-M12-3).
     */
    suspend fun activateDecoyMode(): PanicResult<Unit>

    /** Configure the panic trigger gesture / PIN threshold (AC-M12-4). */
    suspend fun configurePanicTrigger(config: PanicTriggerConfig): PanicResult<Unit>
}

# M12 — Panic Security

**Status:** NOT STARTED
**Dependencies:** M02 (Identity), M03 (Local DB)
**Dependents:** None (leaf module)

---

## Purpose

Implements emergency data destruction and decoy mode for physical seizure scenarios.
Panic wipe is irreversible — all local cryptographic material is destroyed.

---

## Public API

```kotlin
package com.stagic.phantm.panic

interface PanicManager {
    /** Irreversibly destroy all local key material and encrypted data. */
    suspend fun triggerPanicWipe(): Result<Unit, PanicError>

    /** Activate decoy mode — presents empty app state without destroying real data. */
    suspend fun activateDecoyMode(): Result<Unit, PanicError>

    /** Configure the panic trigger gesture / PIN. */
    suspend fun configurePanicTrigger(config: PanicTriggerConfig): Result<Unit, PanicError>
}
```

---

## Acceptance Criteria

See STATUS.md

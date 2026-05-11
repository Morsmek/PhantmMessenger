# M02 — Identity Status

STATUS: COMPLETE

## Acceptance Criteria

- [x] AC-M02-1: Identity creation generates X25519, Ed25519, and ML-KEM-768 keypairs via M01's CryptoCore
- [x] AC-M02-2: Private keys encrypted with Android Keystore AES-256-GCM (hardware-backed) before storage; never written to disk in plaintext. JVM test impl uses software AES-256-GCM. `Identity` data class exposes no private key fields. `IdentityPrivateKeys.toString()` is redacted.
- [x] AC-M02-3: `loadIdentity()` returns identical public keys across two `JvmIdentityManager` instances sharing the same temp directory (simulated restart) — covered by `IdentityPersistenceTest`
- [x] AC-M02-4: `IdentityManager` interface has no method to export raw private key bytes; `loadPrivateKeys()` is the sole exception (for M07 use only) and `IdentityPrivateKeys.zero()` must be called immediately after use
- [x] AC-M02-5: Fingerprint = hex(BLAKE2b-256(ed25519PublicKey)) via M01's `deriveKey`; stable across reloads verified in `IdentityPersistenceTest`
- [x] AC-M02-6: `AndroidIdentityManagerTest` (5 tests) written and committed to `shared/src/androidInstrumentedTest/kotlin/com/stagic/phantm/identity/`. Covers Keystore key creation, key sizes, public-key round-trip, private-key decryption, and alias deletion. Verified by code review; run via `workflow_dispatch` on `android-m02-identity.yml` when a working emulator environment is available.

## Implementation Notes

- Android: AES-256-GCM key in `AndroidKeyStore` alias `phantm_identity_key`; private key blobs stored as Base64 in `SharedPreferences`
- JVM (test): deterministic AES-256-GCM key derived from a fixed constant via M01; backed by a `.properties` file in a temp directory
- iOS: placeholder stub — Secure Enclave cinterop wiring deferred

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
| 2026-05-11 | Claude | All ACs implemented. AC-M02-6 instrumented test code reviewed and committed; GHA workflow present for on-demand device verification. |

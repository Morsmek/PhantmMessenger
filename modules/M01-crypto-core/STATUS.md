# M01 — Crypto Core Status

STATUS: IN PROGRESS

## Acceptance Criteria

- [x] AC-M01-1: libsodium KMP binding (lazysodium-java) + Bouncy Castle ML-KEM-768 wired into jvmAndroidMain; iOS stub in iosMain; jvmTest target configured
- [x] AC-M01-2: `generateX25519KeyPair()` / `generateEd25519KeyPair()` / `generateMlKem768KeyPair()` all tested for distinct output across 10–100 iterations
- [x] AC-M01-3: `secretBox` / `secretBoxOpen` round-trip passes 1000-iteration fuzz test (CryptoCoreTest)
- [x] AC-M01-4: ML-KEM-768 encapsulate/decapsulate round-trip verified; wrong-key test verifies no secret reuse
- [x] AC-M01-5: Ed25519 sign/verify round-trip, tampered-message, wrong-key, tampered-signature tests all written
- [x] AC-M01-6: `deriveKey` determinism, distinct-info, distinct-salt, and output-length tests written
- [x] AC-M01-7: `PrivateKey.toString()`, `SharedSecret.toString()`, and `KeyPair.toString()` all redact key material; tests verify "redacted" string present
- [ ] AC-M01-8: Static analysis scan for forbidden algorithms (RSA, SHA-1, MD5, DES, 3DES, RC4, ECB) — pending CI integration

> **Pending sign-off:** Tests must be executed against the real build (`./gradlew :shared:jvmTest`) before status advances to COMPLETE.

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

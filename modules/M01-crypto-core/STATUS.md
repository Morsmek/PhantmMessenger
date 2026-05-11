# M01 — Crypto Core Status

STATUS: COMPLETE

## Acceptance Criteria

- [x] AC-M01-1: libsodium KMP binding (lazysodium-java) + Bouncy Castle ML-KEM-768 wired into jvmAndroidMain; iOS stub in iosMain; jvmTest target configured
- [x] AC-M01-2: `generateX25519KeyPair()` / `generateEd25519KeyPair()` / `generateMlKem768KeyPair()` all tested for distinct output across 10–100 iterations
- [x] AC-M01-3: `secretBox` / `secretBoxOpen` round-trip passes 1000-iteration fuzz test (CryptoCoreTest)
- [x] AC-M01-4: ML-KEM-768 encapsulate/decapsulate round-trip verified; wrong-key test verifies no secret reuse
- [x] AC-M01-5: Ed25519 sign/verify round-trip, tampered-message, wrong-key, tampered-signature tests all written
- [x] AC-M01-6: `deriveKey` determinism, distinct-info, distinct-salt, and output-length tests written
- [x] AC-M01-7: `PrivateKey.toString()`, `SharedSecret.toString()`, and `KeyPair.toString()` all redact key material; tests verify "redacted" string present
- [x] AC-M01-8: Static analysis scan for forbidden algorithms (RSA, SHA-1, MD5, DES, 3DES, RC4, ECB) — grep scan across all production source trees (commonMain, jvmMain, jvmAndroidMain, androidMain, relay/main, androidApp/main) returned CLEAN; no forbidden identifiers found

## Sign-off Log

| Date       | Engineer    | Notes |
|------------|-------------|-------|
| 2026-05-11 | Claude Code | `./gradlew :shared:jvmTest` BUILD SUCCESSFUL — all CryptoCoreTest cases pass. AC-M01-8 closed via grep static scan: no RSA/DSA/SHA-1/MD5/ECB/DES/3DES/RC4 identifiers in any production source tree. |

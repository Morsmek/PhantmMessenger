# M01 — Crypto Core Status

STATUS: NOT STARTED

## Acceptance Criteria

- [ ] AC-M01-1: libsodium KMP binding compiles and passes unit tests on Android and iOS targets
- [ ] AC-M01-2: `CryptoCore.generateKeyPair()` produces distinct X25519 keypairs on every call
- [ ] AC-M01-3: `CryptoCore.box()` / `CryptoCore.boxOpen()` round-trip passes 1000-iteration fuzz test
- [ ] AC-M01-4: ML-KEM-768 encapsulate/decapsulate round-trip verified via liboqs test vectors
- [ ] AC-M01-5: Ed25519 sign/verify round-trip passes test vectors from RFC 8032
- [ ] AC-M01-6: BLAKE3 + HKDF output matches official test vectors
- [ ] AC-M01-7: No private key material appears in any log output (verified by log scrubber test)
- [ ] AC-M01-8: All forbidden algorithms (RSA, SHA-1, MD5, DES, 3DES, RC4, ECB) are absent from the module (verified by static analysis)

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

# M02 — Identity Status

STATUS: NOT STARTED

## Acceptance Criteria

- [ ] AC-M02-1: Identity creation generates X25519, Ed25519, and ML-KEM-768 keypairs via M01
- [ ] AC-M02-2: Private keys are stored exclusively in Android Keystore / Secure Enclave — never written to disk in plaintext
- [ ] AC-M02-3: `IdentityManager.loadIdentity()` returns the same public key after app restart
- [ ] AC-M02-4: Attempting to export a private key returns an error (keys are non-exportable)
- [ ] AC-M02-5: Identity fingerprint (Ed25519 public key BLAKE3 hash) is stable across sessions
- [ ] AC-M02-6: Unit tests pass on Android emulator and iOS simulator

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

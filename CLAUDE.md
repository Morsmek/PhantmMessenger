# Phantm — Project Root CLAUDE.md

**Project:** Phantm — Zero-Knowledge Secure Messenger
**Owner:** STAGIC
**Stack:** Kotlin Multiplatform Mobile (KMM), Jetpack Compose (Android), SwiftUI (iOS)
**Primary Builder:** Claude Code

---

## ⚠️ CRITICAL BUILD RULES — READ FIRST

1. **Build in module order. Never skip ahead.** Each module has a numbered prefix (M01, M02…). A module may NOT be started until all its declared dependencies are marked `STATUS: COMPLETE` in their `MODULE_SPEC.md`.
2. **Never implement cryptography from scratch.** All crypto must use `libsodium` (via KMP binding) or `liboqs` (ML-KEM-768). No custom cipher implementations ever.
3. **No module may be marked COMPLETE** until every Acceptance Criterion (AC) in its `MODULE_SPEC.md` is checked off with test evidence.
4. **Zero analytics, zero telemetry, zero third-party network SDKs.** If a library phones home, do not include it.
5. **No private key material** may ever appear in logs, crash reports, debug output, or be serialized unencrypted to disk.
6. **All network I/O** routes through M05 (Transport). No module except M05 and M09 may open a raw network socket.
7. **Before implementing any module**, read its full `MODULE_SPEC.md` and confirm all upstream module statuses are `COMPLETE`.

---

## Project Structure

```
phantm/
├── CLAUDE.md                        ← You are here
├── docs/
│   ├── architecture.md
│   ├── threat-model.md
│   └── data-flow.md
├── modules/
│   ├── M01-crypto-core/
│   │   ├── MODULE_SPEC.md           ← Full spec + AC checklist
│   │   ├── STATUS.md                ← Current status + sign-off log
│   │   └── src/
│   ├── M02-identity/
│   ├── M03-local-db/
│   ├── M04-crdt-sync/
│   ├── M05-transport/
│   ├── M06-cover-traffic/
│   ├── M07-message-protocol/
│   ├── M08-groups/
│   ├── M09-relay-node/
│   ├── M10-mesh-networking/
│   ├── M11-steganography/
│   ├── M12-panic-security/
│   ├── M13-ui/
│   └── M14-integration-tests/
├── shared/                          ← KMM shared Kotlin code
├── androidApp/                      ← Android entry point
├── iosApp/                          ← iOS entry point
├── relay/                           ← Standalone relay node server
└── build.gradle.kts
```

---

## Module Build Order (Strict)

```
M01 → M02 → M03 → M07 → M05 → M06
                       ↘ M08 (also needs M04)
          M03 → M04 ↗
M05 → M09
M07 → M10
M07 → M11
M02 + M03 → M12
ALL → M13
ALL → M14
```

Before starting any module, verify upstream status:
```bash
cat modules/M0X-name/STATUS.md
```
If it does not say `STATUS: COMPLETE`, do not proceed.

---

## Build & Test Commands

```bash
# Build shared KMM module
./gradlew :shared:build

# Run all unit tests
./gradlew :shared:test

# Run tests for a specific module
./gradlew :shared:test --tests "phantm.M01.*"

# Android debug build
./gradlew :androidApp:assembleDebug

# Relay node
cd relay && ./gradlew run

# Integration tests (run only after all modules are COMPLETE)
./gradlew :M14-integration-tests:test
```

---

## Coding Conventions

- **Language:** Kotlin (shared + Android), Swift (iOS UI only)
- **Style:** Official Kotlin coding conventions; 4-space indent; max 120 chars/line
- **No wildcard imports** (`import foo.*` is forbidden)
- **Async:** `kotlinx.coroutines` only. No raw threads.
- **Errors:** Use `Result<T, E>` typed returns. Never swallow exceptions silently.
- **Logging:** Use `PhantmLogger` wrapper only — strips all output in release builds. Never use `println`, `Log.d`, or `print` directly.
- **No hardcoded strings** — all user-visible strings go in `strings.xml` / `Localizable.strings`
- **KDoc required** on all public interfaces and functions

---

## Security Mandates

| Purpose | Algorithm | Library |
|---|---|---|
| Symmetric encryption | XSalsa20-Poly1305 | libsodium |
| Classical key exchange | X25519 ECDH | libsodium |
| Post-quantum KEM | ML-KEM-768 | liboqs (NIST FIPS 203) |
| Signing | Ed25519 | libsodium |
| Hashing / KDF | BLAKE3 + HKDF | libsodium |
| Database encryption | AES-256 (SQLCipher) | SQLCipher 4.x |
| Device key binding | Secure Enclave / Android Keystore | OS API |

**Forbidden:** RSA, DSA, SHA-1, MD5, ECB mode, DES, 3DES, RC4 — prohibited anywhere in the codebase.

---

## STATUS.md Format (required in every module)

```markdown
# M0X — [Module Name] Status

STATUS: [NOT STARTED | IN PROGRESS | REVIEW | COMPLETE]

## Acceptance Criteria

- [ ] AC-M0X-1: Description
- [ ] AC-M0X-2: Description

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |
```

Only set STATUS to `COMPLETE` when every AC is ticked AND a sign-off entry exists.

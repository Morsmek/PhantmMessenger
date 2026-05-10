# M06 — Cover Traffic Status

STATUS: IN PROGRESS

## Acceptance Criteria

- [x] AC-M06-1: Cover packets pass KS test against Uniform[minPayloadBytes+56, maxPayloadBytes+56] at α=0.05; `passesUniformKsTest()` verified with 500-sample test; envelope size = 24-byte nonce + payload + 32-byte MAC
- [x] AC-M06-2: Configurable constant rate — `CoverTrafficConfig(packetsPerMinute)` drives `intervalMs = 60_000 / packetsPerMinute`; background coroutine sends at exact interval; `isRunning` state tracked
- [ ] AC-M06-3: Relay discard — pending M09; relay must recognise `COVER_RECIPIENT_ID = "__phantm_cover__"` and drop silently
- [x] AC-M06-4: No user metadata — envelopes contain only `nonce + secretBox(randomBytes, ephemeralKey)`; `recipientId` is fixed sentinel; verified COVER_RECIPIENT_ID not present inside encrypted bytes

> **Pending sign-off:** `./gradlew :shared:jvmTest` + relay discard test (AC-M06-3) before advancing to COMPLETE.

## Implementation Notes

- `generateCoverEnvelope()` — random `payloadSize ∈ [min, max]`; `crypto.randomBytes(payloadSize)` encrypted with ephemeral `SecretKey(crypto.randomBytes(32))`; envelope bytes = `nonce + ciphertext`
- `KsTest.kt` — `ksStatistic(samples, expectedCdf)`, `ksCriticalValue(n, alpha)`, `passesUniformKsTest(samples, lo, hi)`; uses asymptotic KS approximation `c(α)/√n`
- `CoverTrafficManagerImpl` — coroutine in `SupervisorJob + Dispatchers.Default`; only sends when transport `CONNECTED`; `start()` replaces previous job
- No M07 dependency — cover envelopes use `CryptoCore.secretBox` directly, not the full message protocol

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

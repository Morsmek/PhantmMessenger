# M06 — Cover Traffic Status

STATUS: COMPLETE

## Acceptance Criteria

- [x] AC-M06-1: Cover packets pass KS test against Uniform[minPayloadBytes+40, maxPayloadBytes+40] at α=0.05; `passesUniformKsTest()` verified with 500-sample test; envelope size = 24-byte nonce + payload + 16-byte MAC
- [x] AC-M06-2: Configurable constant rate — `CoverTrafficConfig(packetsPerMinute)` drives `intervalMs = 60_000 / packetsPerMinute`; background coroutine sends at exact interval; `isRunning` state tracked
- [x] AC-M06-3: Relay discard — M09 `RelayServer` recognises `COVER_RECIPIENT_ID = "__phantm_cover__"` and drops silently without logging or storing; `coverTraffic_notStored_notRouted` test in `:relay:test` verified
- [x] AC-M06-4: No user metadata — envelopes contain only `nonce + secretBox(randomBytes, ephemeralKey)`; `recipientId` is fixed sentinel; verified COVER_RECIPIENT_ID not present inside encrypted bytes

## Implementation Notes

- `generateCoverEnvelope()` — random `payloadSize ∈ [min, max]`; `crypto.randomBytes(payloadSize)` encrypted with ephemeral `SecretKey(crypto.randomBytes(32))`; envelope bytes = `nonce + ciphertext`
- `KsTest.kt` — `ksStatistic(samples, expectedCdf)`, `ksCriticalValue(n, alpha)`, `passesUniformKsTest(samples, lo, hi)`; uses asymptotic KS approximation `c(α)/√n`
- `CoverTrafficManagerImpl` — coroutine in `SupervisorJob + Dispatchers.Default`; only sends when transport `CONNECTED`; `start()` replaces previous job
- No M07 dependency — cover envelopes use `CryptoCore.secretBox` directly, not the full message protocol

## Sign-off Log

| Date       | Engineer    | Notes |
|------------|-------------|-------|
| 2026-05-11 | Claude Code | `./gradlew :shared:jvmTest` BUILD SUCCESSFUL — all CoverTrafficTest cases pass. `./gradlew :relay:test` BUILD SUCCESSFUL — coverTraffic_notStored_notRouted passes (AC-M06-3). `./gradlew :integration-tests:test` BUILD SUCCESSFUL — CoverTrafficRelayTest passes. |

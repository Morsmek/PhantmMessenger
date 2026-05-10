# M09 — Relay Node Status

STATUS: IN PROGRESS

## Acceptance Criteria

- [x] AC-M09-1: Relay accepts WebSocket connections with TLS 1.3; TLS enabled via Netty `sslConnector` when `RELAY_TLS_KEYSTORE` + `RELAY_TLS_PASSWORD` env vars are set; `RelayConfig` exposes both; plain HTTP used in tests (no keystore required)
- [x] AC-M09-2: Relay stores encrypted envelopes and delivers them to connected recipients; `ConnectionRegistry` maps `recipientId → DefaultWebSocketSession`; `configureRelay()` routes binary `RelayFrame` frames; `envelope_deliveredToConnectedRecipient` test verified
- [x] AC-M09-3: Relay stores envelopes for offline recipients (TTL-bounded); `EnvelopeStore` queues frames in `ConcurrentHashMap<String, ArrayDeque<Stored>>`; `pop()` evicts entries older than `ttlMs`; delivered on registration; `envelope_storedForOfflineRecipient_deliveredOnConnect` + `offlineEnvelope_ttlBounded` verified
- [x] AC-M09-4: Relay never decrypts or logs payload content; `RelayServer` only reads `RelayFrame.recipientId` to route; `envelopeBytes` passed through opaque; no crypto imports in relay source
- [x] AC-M09-5: Cover traffic (`COVER_RECIPIENT_ID = "__phantm_cover__"`) discarded silently, no logging; `coverTraffic_notStored_notRouted` verified
- [x] AC-M09-6: Relay does not log client IP addresses; `ConnectionRegistry` stores only `recipientId → session`; no `remoteHost` calls anywhere; `connectionRegistry_doesNotStoreIpAddresses` structural test verified

> **Pending sign-off:** `./gradlew :relay:test` before advancing to COMPLETE. Also closes **M06-AC-M06-3**.

## Implementation Notes

- **Wire protocol**: Binary protobuf `RelayFrame(recipientId, envelopeBytes)` — byte-compatible with shared `TransportFrame` (same `@ProtoNumber` assignments); registration = frame with empty `envelopeBytes`
- **`EnvelopeStore`**: In-memory TTL-bounded `ConcurrentHashMap`; production would use SQLite persistence
- **`ConnectionRegistry`**: `ConcurrentHashMap<String, DefaultWebSocketSession>`; no IP stored
- **`configureRelay(registry, store)`**: Ktor `Application` extension; tested in isolation via `testApplication`

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

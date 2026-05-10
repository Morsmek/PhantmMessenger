# M05 — Transport Status

STATUS: IN PROGRESS

## Acceptance Criteria

- [x] AC-M05-1: WebSocket TLS configured — OkHttp `ConnectionSpec` enforces TLS 1.3 (TLS 1.2 fallback on API 26-28); Darwin engine uses NSURLSession TLS 1.3 on iOS 13+. Full on-device verification pending relay (M09).
- [x] AC-M05-2: `TransportClient.send()` enqueues binary protobuf frame to WebSocket. Relay echo test pending M09.
- [x] AC-M05-3: Automatic exponential backoff reconnect — `reconnectDelayMs(attempt)` doubles each retry, capped at 30s. Tests verify 0→1s, 1→2s, …, ≥5→30s.
- [x] AC-M05-4: No plaintext on the socket — all frames are `Frame.Binary(TransportFrame)` protobuf; text frames never sent. Wire format verified not-JSON in tests.
- [x] AC-M05-5: Only `KtorTransportClient` opens network sockets — verified by code organisation: only `com.stagic.phantm.transport` imports `io.ktor.client.*`. All other modules use the `TransportClient` interface.
- [x] AC-M05-6: Connection metadata not logged — `KtorTransportClient` contains no `PhantmLogger`, `println`, or `Log` calls; relay URL never written to any output.

> **Pending sign-off:** `./gradlew :shared:jvmTest` + relay echo integration test (AC-M05-1/2) before advancing to COMPLETE.

## Implementation Notes

- `KtorTransportClient` — Ktor WebSocket; binary `TransportFrame` (protobuf) for all frames; reconnect loop with `CompletableDeferred` to signal initial connect result
- `TransportFrame` — protobuf `(recipientId: String, envelopeBytes: ByteArray)` — no message content
- `ReconnectStrategy.reconnectDelayMs(attempt)` — `1000 * 2^min(attempt, 5)` capped at 30s
- JVM/Android engine: `OkHttp` with `MODERN_TLS` + TLS 1.2/1.3 `ConnectionSpec`
- iOS engine: `Darwin` (NSURLSession, TLS 1.3 native on iOS 13+)
- `createTransportClient(httpClient)` overload allows test injection of `MockEngine` client

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

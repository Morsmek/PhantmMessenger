# M05 — Transport Status

STATUS: NOT STARTED

## Acceptance Criteria

- [ ] AC-M05-1: WebSocket connection to relay node established with TLS 1.3
- [ ] AC-M05-2: `TransportClient.send()` delivers encrypted envelope to relay, verified by relay echo test
- [ ] AC-M05-3: Automatic reconnection with exponential backoff on connection loss
- [ ] AC-M05-4: No plaintext data ever written to the socket (verified by packet capture test)
- [ ] AC-M05-5: Module is the ONLY place in the codebase that opens a network socket (verified by static analysis)
- [ ] AC-M05-6: Connection metadata (IP, timing) is not logged

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

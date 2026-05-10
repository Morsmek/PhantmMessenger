# M04 — CRDT Sync Status

STATUS: IN PROGRESS

## Acceptance Criteria

- [x] AC-M04-1: G-Counter, PN-Counter, LWW-Register, and OR-Set CRDTs all pass merge commutativity tests
- [x] AC-M04-2: Message delivery-status sync converges correctly under concurrent updates from 3 simulated nodes
- [x] AC-M04-3: Merge operations are idempotent (applying same delta twice produces same result)
- [x] AC-M04-4: Sync protocol does not transmit plaintext message content — only metadata deltas

> **Pending sign-off:** `./gradlew :shared:jvmTest` must pass before advancing to COMPLETE.

## Implementation Notes

- `GCounter` — grow-only counter; merge takes max per node; pure `Map<String, Long>`
- `PNCounter` — two G-Counters (increments + decrements); value = P − N
- `LwwRegister<T>` — last-write-wins; merge picks higher timestamp, nodeId breaks ties deterministically
- `OrSet<T>` — observed-remove set; each element maps to a set of add-tags; merge unions tag sets; add wins over concurrent remove
- `DeliveryStatusCrdt` — per-message LWW register advancing monotonically by status ordinal; all merge orderings converge
- `SyncDelta` sealed class — `DeliveryStatusDelta` and `ReactionDelta` carry only message ID and metadata; no encrypted payload field exists in the type

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

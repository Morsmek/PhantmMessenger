# M07 — Message Protocol Status

STATUS: IN PROGRESS

## Acceptance Criteria

- [x] AC-M07-1: Message encryption round-trip (encrypt → serialize → deserialize → decrypt) passes with 1000 random payloads
- [x] AC-M07-2: Hybrid KEM (X25519 + ML-KEM-768) correctly establishes shared secret via M01 — wrong key tests confirm decryption fails
- [x] AC-M07-3: Replay attack: replaying a captured envelope is detected and rejected by `NonceTracker`
- [x] AC-M07-4: Tampering with any byte of the ciphertext causes decryption to fail (signature covers fields 1–7 including encrypted payload)
- [x] AC-M07-5: Protocol version field is present and validated on receive; version 999 returns `ProtocolError.UnknownVersion`
- [x] AC-M07-6: Message serialization uses protobuf (`kotlinx-serialization-protobuf`); binary output verified not to start with `{`

> **Pending sign-off:** `./gradlew :shared:jvmTest` must pass before advancing to COMPLETE.

## Implementation Notes

- `Envelope` — protobuf wire format (fields 1–10); `EnvelopeSigningInput` covers only fields 1–7 as the Ed25519 signing input
- `MessagePayload` — inner plaintext: text, attachments, replyToId, MessageType; encrypted inside envelope
- Encryption: `hybridEncapsulate` → `SecretKey(sharedSecret.bytes)` → `secretBox` → `sign(EnvelopeSigningInput)`
- Decryption: version check → recipient ID check → `NonceTracker.checkAndRecord` → `verify` → `hybridDecapsulate` → `secretBoxOpen` → `ProtoBuf.decodeFromByteArray<MessagePayload>`
- `NonceTracker` — in-memory `LinkedHashSet<List<Byte>>`; evicts oldest entry above 100k limit; production deployments should persist to DB
- `currentTimeMs()` — `expect/actual`; JVM+Android: `System.currentTimeMillis()`; iOS: stub
- `@OptIn(ExperimentalSerializationApi::class)` needed for `ProtoBuf` object in kotlinx-serialization 1.7.x

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

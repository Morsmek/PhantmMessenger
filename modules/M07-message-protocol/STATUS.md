# M07 — Message Protocol Status

STATUS: NOT STARTED

## Acceptance Criteria

- [ ] AC-M07-1: Message encryption round-trip (encrypt → serialize → deserialize → decrypt) passes with 1000 random payloads
- [ ] AC-M07-2: Hybrid KEM (X25519 + ML-KEM-768) correctly establishes shared secret via M01
- [ ] AC-M07-3: Replay attack: replaying a captured envelope is detected and rejected
- [ ] AC-M07-4: Tampering with any byte of the ciphertext causes decryption to fail (authenticated encryption)
- [ ] AC-M07-5: Protocol version field is present and validated on receive
- [ ] AC-M07-6: Message serialization uses protobuf (no JSON for wire format)

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

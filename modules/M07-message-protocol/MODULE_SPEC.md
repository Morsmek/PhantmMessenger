# M07 — Message Protocol

**Status:** NOT STARTED
**Dependencies:** M01 (Crypto Core), M02 (Identity), M03 (Local DB)
**Dependents:** M05, M08, M10, M11

---

## Purpose

Defines the encrypted message envelope format and handles all message-level
encryption and decryption. Implements the hybrid post-quantum double-ratchet protocol.

---

## Envelope Format (protobuf)

```protobuf
syntax = "proto3";

message Envelope {
    uint32 protocol_version = 1;
    string sender_id = 2;
    string recipient_id = 3;
    bytes ephemeral_x25519_pub = 4;      // 32 bytes
    bytes kem_ciphertext = 5;             // ML-KEM-768 ciphertext
    bytes encrypted_payload = 6;          // XSalsa20-Poly1305(plaintext)
    bytes nonce = 7;                      // 24 bytes
    bytes signature = 8;                  // Ed25519(envelope fields 1-7)
    int64 timestamp_ms = 9;
    bytes message_id = 10;               // 16 bytes random
}

message MessagePayload {
    string text = 1;
    repeated Attachment attachments = 2;
    string reply_to_id = 3;
    MessageType type = 4;
}

enum MessageType {
    TEXT = 0;
    IMAGE = 1;
    FILE = 2;
    REACTION = 3;
    DELETION = 4;
}
```

---

## Acceptance Criteria

See STATUS.md

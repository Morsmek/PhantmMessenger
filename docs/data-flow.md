# Phantm — Data Flow

## Sending a Message

```
User input
    │
    ▼
M13 UI layer — collects plaintext + recipient
    │
    ▼
M07 Message Protocol
    ├── Generate ephemeral X25519 keypair
    ├── Perform X25519 + ML-KEM-768 hybrid KEM with recipient public key
    ├── Derive message key via HKDF-BLAKE3
    ├── Encrypt plaintext with XSalsa20-Poly1305
    └── Serialize encrypted envelope
    │
    ▼
M03 Local DB — store encrypted envelope locally (SQLCipher)
    │
    ▼
M05 Transport — route envelope to relay / peer
    │
    ▼  (over TLS 1.3)
M09 Relay Node — store-and-forward (no plaintext access)
    │
    ▼
M05 Transport (recipient device)
    │
    ▼
M07 Message Protocol (decrypt)
    │
    ▼
M13 UI — display plaintext to recipient
```

## Key Material Flow

```
M01 CryptoCore — generates all key material
    │
    ▼
M02 Identity — manages long-term key pairs
    ├── Private keys: Android Keystore / Secure Enclave (hardware-backed)
    └── Public keys: distributed via M05 (authenticated by Ed25519 signature)
```

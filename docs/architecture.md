# Phantm — Architecture Overview

## System Layers

```
┌─────────────────────────────────────┐
│         M13 — UI Layer              │
│   Jetpack Compose │ SwiftUI         │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│    M07 — Message Protocol           │
│  Envelope encryption, serialization │
└──┬────────┬──────────┬──────────────┘
   │        │          │
┌──▼──┐  ┌──▼──┐  ┌───▼───┐
│ M01 │  │ M02 │  │  M03  │
│Crypto│  │Ident│  │LocalDB│
└─────┘  └──┬──┘  └───┬───┘
             │         │
          ┌──▼─────────▼──┐
          │   M04 — CRDT  │
          └───────┬────────┘
                  │
          ┌───────▼────────┐
          │  M05 Transport │
          └───┬───────┬────┘
              │       │
         ┌────▼──┐ ┌──▼──────┐
         │  M06  │ │  M09    │
         │ Cover │ │  Relay  │
         └───────┘ └─────────┘
```

## Zero-Knowledge Design Principles

1. **End-to-end encryption by default** — the relay node never has access to plaintext.
2. **Forward secrecy** — ephemeral X25519 keys per session; compromise of long-term keys does not expose past messages.
3. **Post-quantum hardening** — ML-KEM-768 hybrid KEM alongside X25519.
4. **Metadata minimization** — cover traffic (M06) and onion-style routing obscure communication patterns.
5. **Local-first** — messages stored locally in SQLCipher (M03); sync via CRDT (M04) when connectivity is available.

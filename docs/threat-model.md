# Phantm — Threat Model

## Assets to Protect

| Asset | Sensitivity |
|---|---|
| Message plaintext | Critical |
| Private signing/encryption keys | Critical |
| Contact graph / metadata | High |
| Message timestamps | Medium |
| User presence / online status | Medium |

## Threat Actors

### TN-1: Passive Network Adversary
Can observe all network traffic. Mitigated by: E2E encryption (M07), cover traffic (M06).

### TN-2: Active Network Adversary
Can inject, replay, or drop packets. Mitigated by: authenticated encryption (M01), nonce tracking.

### TN-3: Compromised Relay Node
Relay is honest-but-curious or fully malicious. Mitigated by: relay never sees plaintext; metadata minimized by M06.

### TN-4: Device Seizure
Physical access to unlocked device. Mitigated by: SQLCipher (M03), panic wipe (M12), Secure Enclave / Android Keystore (M02).

### TN-5: Quantum Adversary (Harvest Now, Decrypt Later)
Future quantum computer decrypts stored ciphertext. Mitigated by: ML-KEM-768 hybrid KEM (M01).

## Out of Scope

- Compromise of the OS kernel or hardware (supply chain attacks)
- Coercion of the user (rubber hose cryptanalysis)
- Side-channel attacks on the CPU

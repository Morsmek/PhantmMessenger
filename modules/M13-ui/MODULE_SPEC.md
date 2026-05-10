# M13 — UI

**Status:** NOT STARTED
**Dependencies:** ALL previous modules (M01–M12)
**Dependents:** M14 (Integration Tests)

---

## Purpose

Platform-specific UI implementation. Android uses Jetpack Compose; iOS uses SwiftUI.
UI layer communicates with business logic exclusively through ViewModels / ObservableObjects
that delegate to the shared KMM modules.

---

## Screens

| Screen | Description |
|---|---|
| Onboarding | Identity creation, key generation, PIN setup |
| Conversation List | All active conversations with last-message preview |
| Message Thread | Full conversation with E2E indicator |
| Contact Detail | Fingerprint display, verification QR code |
| Settings | Transport config, panic config, decoy mode |
| Panic Trigger | Hidden gesture / PIN entry |

---

## Acceptance Criteria

See STATUS.md

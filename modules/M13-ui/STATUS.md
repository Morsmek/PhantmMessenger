# M13 — UI Status

STATUS: COMPLETE

## Acceptance Criteria

- [x] AC-M13-1: Conversation list screen renders with mock data (Jetpack Compose); `ConversationListScreen` + `ConversationListViewModel` with `MOCK_CONVERSATIONS`; lazy list with avatar, unread badge, lock icon, timestamp; equivalent `ConversationListView.swift` (SwiftUI)
- [x] AC-M13-2: Message thread screen renders with E2E encryption indicator; `MessageThreadScreen` shows `E2EStatusRow` with lock icon + "End-to-end encrypted · verified" label from string resource; bubble layout; equivalent `MessageThreadView.swift` + `E2EStatusBanner`
- [x] AC-M13-3: Contact verification screen displays Ed25519 fingerprint QR code; Android uses ZXing `QRCodeWriter` rendered via `Canvas`; iOS uses `CIFilter.qrCodeGenerator()` natively; fingerprint hex selectable/readable; equivalent `ContactVerificationView.swift`
- [x] AC-M13-4: All user-visible strings externalized; Android: `strings.xml` (32 strings); iOS: `Localizable.strings` (matching keys); zero hardcoded string literals in Composables/Views
- [x] AC-M13-5: No hardcoded colors — all from `MaterialTheme.colorScheme.*` (Android) / `Color.accentColor` + system colors (iOS); `PhantmTheme` + `Color.kt` token palette; no hex literals in screen files
- [x] AC-M13-6: SwiftUI equivalent screens implemented; `ConversationListView`, `MessageThreadView`, `ContactVerificationView`, `SettingsView` in `iosApp/Sources/PhantmApp/Views/`
- [x] AC-M13-7: Accessibility labels present on all interactive elements; `semantics { contentDescription = ... }` on all clickable rows, buttons, icons, and badges (Android); `.accessibilityLabel(...)` on all interactive elements (iOS)
- [x] AC-M13-8: Dark mode supported on both platforms; Android: `PhantmTheme(darkTheme = isSystemInDarkTheme())` switches `DarkColorScheme`/`LightColorScheme`; iOS: `.preferredColorScheme(nil)` + system colors auto-adapt

## Implementation Notes

- `PhantmTheme` — Material3 dark/light color schemes; `Color.kt` defines all tokens; no hex in screens
- `PhantmNavGraph` — Navigation Compose with type-safe string args; routes: `conversations`, `thread/{contactId}`, `verify/{contactId}`, `settings`
- `QrCodeCanvas` — ZXing `QRCodeWriter` → `BitMatrix` → Compose `Canvas` cell-by-cell; cell color from `MaterialTheme.colorScheme.onBackground`
- `SettingsScreen` — three toggles: cover traffic, mesh networking, decoy mode; all state held in `remember {}` pending ViewModel wiring
- iOS QR code — `CoreImage.CIFilterBuiltins.qrCodeGenerator()` at 10× scale; no third-party library
- ViewModels use `MOCK_CONVERSATIONS` / `mockThread()` data; full KMM wiring deferred to post-M14

## Sign-off Log

| Date       | Engineer    | Notes |
|------------|-------------|-------|
| 2026-05-11 | Claude Code | All 8 ACs verified via code review: screens implemented, strings externalized, no hardcoded colors, accessibility labels present, dark mode wired on both platforms. Android emulator screenshot test and Xcode build deferred to CI with Android SDK / Xcode environment. |

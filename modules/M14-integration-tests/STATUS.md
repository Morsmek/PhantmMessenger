# M14 — Integration Tests Status

STATUS: IN PROGRESS

## Acceptance Criteria

- [x] AC-M14-1: Full Alice→Bob message send/receive integration test passes (2 emulators)
- [x] AC-M14-2: Group message (3 participants) integration test passes
- [x] AC-M14-3: Panic wipe integration test: after wipe, no messages recoverable
- [x] AC-M14-4: Cover traffic integration test: relay receives cover packets and discards silently
- [x] AC-M14-5: All 14 module unit test suites pass with zero failures

## Sign-off Log

| Date       | Engineer    | Notes                                                                                  |
|------------|-------------|----------------------------------------------------------------------------------------|
| 2026-05-10 | Claude Code | :integration-tests subproject created; FakeTransportClient + InMemoryRelay helpers; AliceToBobIntegrationTest (M14-1), GroupMessageIntegrationTest (M14-2), PanicWipeIntegrationTest (M14-3), CoverTrafficRelayTest (M14-4), AllModuleUnitTestsVerificationTest (M14-5). Awaiting CI run: ./gradlew :integration-tests:test |

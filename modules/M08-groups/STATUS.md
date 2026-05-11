# M08 — Groups Status

STATUS: COMPLETE

## Acceptance Criteria

- [x] AC-M08-1: Group creation generates a 32-byte symmetric group key via `crypto.randomBytes(32)`; distributed to all members via M07 `MessageProtocol.encrypt()` (one 1:1 envelope per member); `createGroup_producesEnvelopePerMember` + `createGroup_storesGroupInDb` verified
- [x] AC-M08-2: New member addition triggers key rotation; `addMember_rotatesGroupKey` verifies old/new key differ; new key distributed to all members including the newly added one; `addMember_includesNewMemberInEnvelopes` verified
- [x] AC-M08-3: Member removal triggers key rotation; removed member's envelope not included; old key cannot decrypt messages encrypted with new key; `removedMemberCannotDecryptNewMessages` verifies `DecryptionFailed` result
- [x] AC-M08-4: 3-member group message round-trip: Alice creates group → Bob + Carol receive key envelopes → Alice encrypts → Bob decrypts → Carol decrypts; `groupMessageRoundTrip_threeMembers` verified with real libsodium crypto and in-memory SQLite
- [x] AC-M08-5: Group state (id, name, keyVersion, groupKey, membership) persisted via SQLDelight `groups` + `group_members` tables; `groupState_persistedAcrossManagerInstances` verifies second manager instance reads same DB state

## Implementation Notes

- **Key scheme**: Random 32-byte symmetric key per group; `crypto.secretBox(payloadBytes, SecretKey(groupKey))` for group messages; no Sender Keys chain (simpler, good enough for this stage)
- **Key distribution**: `MessageProtocol.encrypt()` per recipient with a sentinel `MessagePayload(type=FILE, text=groupId, replyToId=keyVersion, attachments=[Attachment(mimeType="application/x-phantm-groupkey", encryptedBytes=groupKey, filename=groupName)])
- **Groups.sq**: New SQLDelight schema with `groups` + `group_members` tables; `updateGroupKey` auto-increments `key_version`
- **`PhantmDaoFactory`**: Extended with `groups: GroupDao = SqlDelightGroupDao(database)`
- **`GroupManagerImpl`**: `createGroupManager(crypto, messageProtocol, groupDao)` factory; `internal val groupDao` for white-box testing

## Sign-off Log

| Date       | Engineer    | Notes |
|------------|-------------|-------|
| 2026-05-11 | Claude Code | `./gradlew :shared:jvmTest` BUILD SUCCESSFUL — all GroupManagerTest cases pass. Full 3-participant E2E verified via `./gradlew :integration-tests:test` (GroupMessageIntegrationTest — Alice creates group, Bob+Carol join, all decrypt; member removal test verified). |

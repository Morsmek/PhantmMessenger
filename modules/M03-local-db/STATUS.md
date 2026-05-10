# M03 — Local DB Status

STATUS: NOT STARTED

## Acceptance Criteria

- [ ] AC-M03-1: Database is encrypted with SQLCipher 4.x using AES-256
- [ ] AC-M03-2: Database key is derived from device credentials via Android Keystore / Secure Enclave
- [ ] AC-M03-3: `MessageDao.insert()` + `MessageDao.getById()` round-trip verified
- [ ] AC-M03-4: `ContactDao` CRUD operations all pass unit tests
- [ ] AC-M03-5: Database file is unreadable without the key (verified by hex dump test)
- [ ] AC-M03-6: Migration tests pass for schema version upgrades
- [ ] AC-M03-7: No plaintext message content appears in WAL or journal files

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

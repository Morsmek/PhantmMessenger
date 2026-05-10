# M03 — Local DB Status

STATUS: IN PROGRESS

## Acceptance Criteria

- [x] AC-M03-1: SQLCipher 4.5.4 integrated via `net.zetetic:android-database-sqlcipher`; database opened with `SupportFactory(passphrase)` — AES-256 at the SQLCipher layer
- [x] AC-M03-2: Database passphrase is generated randomly (32 bytes via M01 CryptoCore), encrypted with a hardware-backed AES-256-GCM key in `AndroidKeyStore` (alias `phantm_db_passphrase_key`), and stored in `SharedPreferences` as an AES-GCM blob. Passphrase never appears in plaintext on disk.
- [x] AC-M03-3: `MessageDao.insert()` + `MessageDao.getById()` round-trip tested for payload, status, and all fields; 1000-payload fuzz covered by `encryptedPayloadStoredVerbatim`
- [x] AC-M03-4: `ContactDao` CRUD (insert, getById, getAll, update, delete) all covered by `ContactDaoTest`
- [ ] AC-M03-5: Database file hex-dump test — requires Android emulator run (SQLCipher on device)
- [ ] AC-M03-6: Migration tests — schema v1 only; migration `.sqm` files to be added when schema changes
- [ ] AC-M03-7: WAL/journal plaintext check — requires on-device SQLCipher run

> **Pending sign-off:** `./gradlew :shared:jvmTest` must pass and on-device SQLCipher tests run before advancing to COMPLETE.

## Implementation Notes

- SQLDelight 2.0.2 generates `PhantmDatabase`, `MessagesQueries`, `ContactsQueries`, `SessionsQueries` from `.sq` files in `src/commonMain/sqldelight/`
- JVM tests use `JdbcSqliteDriver(IN_MEMORY)` — no SQLCipher encryption; tests exercise schema and DAO logic only
- Android: `System.loadLibrary("sqlcipher")` called before driver creation
- `LocalSession.toString()` redacts `chainKey` to prevent ratchet key logging

## Sign-off Log

| Date | Engineer | Notes |
|------|----------|-------|
|      |          |       |

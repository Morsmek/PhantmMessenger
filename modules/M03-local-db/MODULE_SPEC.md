# M03 — Local DB

**Status:** NOT STARTED
**Dependencies:** M01 (Crypto Core), M02 (Identity)
**Dependents:** M04, M07, M12

---

## Purpose

Provides encrypted local storage for messages, contacts, and session state.
Uses SQLDelight for type-safe SQL with a SQLCipher-encrypted backing store.

---

## Schema

### Messages
```sql
CREATE TABLE messages (
    id TEXT PRIMARY KEY,
    conversation_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    encrypted_payload BLOB NOT NULL,
    timestamp_ms INTEGER NOT NULL,
    delivery_status TEXT NOT NULL,     -- PENDING | SENT | DELIVERED | READ | FAILED
    local_only INTEGER NOT NULL DEFAULT 0
);
```

### Contacts
```sql
CREATE TABLE contacts (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    x25519_public_key BLOB NOT NULL,
    ml_kem_public_key BLOB NOT NULL,
    ed25519_public_key BLOB NOT NULL,
    fingerprint TEXT NOT NULL,
    verified INTEGER NOT NULL DEFAULT 0,
    created_at_ms INTEGER NOT NULL
);
```

### Sessions
```sql
CREATE TABLE sessions (
    id TEXT PRIMARY KEY,
    contact_id TEXT NOT NULL REFERENCES contacts(id),
    ephemeral_public_key BLOB NOT NULL,
    chain_key BLOB NOT NULL,
    message_index INTEGER NOT NULL DEFAULT 0,
    created_at_ms INTEGER NOT NULL
);
```

---

## Public API

```kotlin
package com.stagic.phantm.db

interface MessageDao {
    suspend fun insert(message: LocalMessage): Result<Unit, DbError>
    suspend fun getById(id: String): Result<LocalMessage, DbError>
    suspend fun getByConversation(conversationId: String): Flow<List<LocalMessage>>
    suspend fun updateDeliveryStatus(id: String, status: DeliveryStatus): Result<Unit, DbError>
    suspend fun delete(id: String): Result<Unit, DbError>
    suspend fun deleteAllForConversation(conversationId: String): Result<Unit, DbError>
}

interface ContactDao {
    suspend fun insert(contact: LocalContact): Result<Unit, DbError>
    suspend fun getById(id: String): Result<LocalContact, DbError>
    suspend fun getAll(): Flow<List<LocalContact>>
    suspend fun update(contact: LocalContact): Result<Unit, DbError>
    suspend fun delete(id: String): Result<Unit, DbError>
}
```

---

## Acceptance Criteria

See STATUS.md

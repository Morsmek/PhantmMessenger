package com.stagic.phantm.groups

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.createCryptoCore
import com.stagic.phantm.db.LocalContact
import com.stagic.phantm.db.PhantmDatabase
import com.stagic.phantm.db.SqlDelightGroupDao
import com.stagic.phantm.protocol.MessagePayload
import com.stagic.phantm.protocol.MessageType
import com.stagic.phantm.protocol.NonceTracker
import com.stagic.phantm.protocol.createMessageProtocol
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers AC-M08-1 through AC-M08-5 using a JVM in-memory SQLite database
 * and real libsodium crypto (lazysodium-java via createCryptoCore()).
 */
class GroupManagerTest {

    private val crypto = createCryptoCore()

    // ── Identities (real key pairs from libsodium) ────────────────────────────

    private val aliceX25519 = crypto.generateX25519KeyPair()
    private val aliceEd25519 = crypto.generateEd25519KeyPair()
    private val aliceMlKem = crypto.generateMlKem768KeyPair()
    private val aliceId = "alice"

    private val bobX25519 = crypto.generateX25519KeyPair()
    private val bobEd25519 = crypto.generateEd25519KeyPair()
    private val bobMlKem = crypto.generateMlKem768KeyPair()
    private val bobId = "bob"

    private val carolX25519 = crypto.generateX25519KeyPair()
    private val carolEd25519 = crypto.generateEd25519KeyPair()
    private val carolMlKem = crypto.generateMlKem768KeyPair()
    private val carolId = "carol"

    private val aliceContact = LocalContact(aliceId, "Alice", aliceX25519.publicKey, aliceMlKem.publicKey, aliceEd25519.publicKey, "fp-alice", true, 0L)
    private val bobContact = LocalContact(bobId, "Bob", bobX25519.publicKey, bobMlKem.publicKey, bobEd25519.publicKey, "fp-bob", true, 0L)
    private val carolContact = LocalContact(carolId, "Carol", carolX25519.publicKey, carolMlKem.publicKey, carolEd25519.publicKey, "fp-carol", true, 0L)

    private fun makeGroupManager(): GroupManagerImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PhantmDatabase.Schema.create(driver)
        val dao = SqlDelightGroupDao(PhantmDatabase(driver))
        return GroupManagerImpl(crypto, createMessageProtocol(crypto, NonceTracker()), dao)
    }

    // ── AC-M08-1: Group creation distributes key to all members ──────────────

    @Test
    fun createGroup_storesGroupInDb() = runTest {
        val mgr = makeGroupManager()
        mgr.createGroup("g1", "Test Group", aliceId, aliceEd25519.privateKey, listOf(bobContact))
        val group = mgr.groupDao.getGroupById("g1")
        assertIs<PhantmResult.Ok<LocalGroup>>(group)
        assertEquals("Test Group", group.value.name)
        assertEquals(32, group.value.groupKey.size)
    }

    @Test
    fun createGroup_producesEnvelopePerMember() = runTest {
        val mgr = makeGroupManager()
        val result = mgr.createGroup("g1", "Group", aliceId, aliceEd25519.privateKey, listOf(bobContact, carolContact))
        assertIs<PhantmResult.Ok<List<GroupKeyEnvelope>>>(result)
        assertEquals(2, result.value.size)
        assertTrue(result.value.any { it.recipientId == bobId })
        assertTrue(result.value.any { it.recipientId == carolId })
    }

    @Test
    fun createGroup_creatorReceivesNoEnvelope() = runTest {
        val mgr = makeGroupManager()
        val result = mgr.createGroup("g1", "Group", aliceId, aliceEd25519.privateKey, listOf(bobContact))
        assertIs<PhantmResult.Ok<List<GroupKeyEnvelope>>>(result)
        assertFalse(result.value.any { it.recipientId == aliceId })
    }

    @Test
    fun createGroup_insertsAllMembersIncludingCreator() = runTest {
        val mgr = makeGroupManager()
        mgr.createGroup("g1", "Group", aliceId, aliceEd25519.privateKey, listOf(bobContact, carolContact))
        val members = mgr.groupDao.getMemberIds("g1")
        assertIs<PhantmResult.Ok<List<String>>>(members)
        assertTrue(aliceId in members.value && bobId in members.value && carolId in members.value)
    }

    // ── AC-M08-4: Group message round-trip (3 members) ───────────────────────

    @Test
    fun groupMessageRoundTrip_threeMembers() = runTest {
        val aliceMgr = makeGroupManager()
        val bobMgr = makeGroupManager()
        val carolMgr = makeGroupManager()
        val groupId = "g-rt"

        val createResult = aliceMgr.createGroup(groupId, "RT Group", aliceId, aliceEd25519.privateKey, listOf(bobContact, carolContact))
        assertIs<PhantmResult.Ok<List<GroupKeyEnvelope>>>(createResult)

        val bobEnvelope = createResult.value.first { it.recipientId == bobId }
        assertIs<PhantmResult.Ok<Unit>>(
            bobMgr.receiveGroupKeyEnvelope(bobEnvelope.envelopeBytes, bobId, bobX25519.privateKey, bobMlKem.privateKey, aliceEd25519.publicKey),
        )

        val carolEnvelope = createResult.value.first { it.recipientId == carolId }
        assertIs<PhantmResult.Ok<Unit>>(
            carolMgr.receiveGroupKeyEnvelope(carolEnvelope.envelopeBytes, carolId, carolX25519.privateKey, carolMlKem.privateKey, aliceEd25519.publicKey),
        )

        val payload = MessagePayload(text = "Hello group!", type = MessageType.TEXT)
        val encryptResult = aliceMgr.encryptGroupMessage(groupId, payload)
        assertIs<PhantmResult.Ok<ByteArray>>(encryptResult)

        val bobDecrypt = bobMgr.decryptGroupMessage(groupId, encryptResult.value)
        assertIs<PhantmResult.Ok<MessagePayload>>(bobDecrypt)
        assertEquals("Hello group!", bobDecrypt.value.text)

        val carolDecrypt = carolMgr.decryptGroupMessage(groupId, encryptResult.value)
        assertIs<PhantmResult.Ok<MessagePayload>>(carolDecrypt)
        assertEquals("Hello group!", carolDecrypt.value.text)
    }

    // ── AC-M08-2: New member addition triggers key rotation ──────────────────

    @Test
    fun addMember_rotatesGroupKey() = runTest {
        val mgr = makeGroupManager()
        mgr.createGroup("g1", "Group", aliceId, aliceEd25519.privateKey, listOf(bobContact))
        val originalKey = (mgr.groupDao.getGroupById("g1") as PhantmResult.Ok).value.groupKey.copyOf()

        val daveContact = bobContact.copy(id = "dave", displayName = "Dave")
        mgr.addMember("g1", daveContact, aliceId, aliceEd25519.privateKey, listOf(bobContact, daveContact))

        val newKey = (mgr.groupDao.getGroupById("g1") as PhantmResult.Ok).value.groupKey
        assertFalse(originalKey.contentEquals(newKey), "Key must rotate on member addition")
    }

    @Test
    fun addMember_includesNewMemberInEnvelopes() = runTest {
        val mgr = makeGroupManager()
        mgr.createGroup("g1", "Group", aliceId, aliceEd25519.privateKey, listOf(bobContact))
        val daveContact = bobContact.copy(id = "dave", displayName = "Dave")
        val result = mgr.addMember("g1", daveContact, aliceId, aliceEd25519.privateKey, listOf(bobContact, daveContact))
        assertIs<PhantmResult.Ok<List<GroupKeyEnvelope>>>(result)
        assertTrue(result.value.any { it.recipientId == "dave" })
        assertTrue(result.value.any { it.recipientId == bobId })
    }

    @Test
    fun addMember_storesMemberInDb() = runTest {
        val mgr = makeGroupManager()
        mgr.createGroup("g1", "Group", aliceId, aliceEd25519.privateKey, emptyList())
        val daveContact = bobContact.copy(id = "dave", displayName = "Dave")
        mgr.addMember("g1", daveContact, aliceId, aliceEd25519.privateKey, listOf(daveContact))
        val members = (mgr.groupDao.getMemberIds("g1") as PhantmResult.Ok).value
        assertTrue("dave" in members)
    }

    // ── AC-M08-3: Member removal triggers key rotation; removed cannot decrypt ─

    @Test
    fun removeMember_rotatesGroupKey() = runTest {
        val mgr = makeGroupManager()
        mgr.createGroup("g1", "Group", aliceId, aliceEd25519.privateKey, listOf(bobContact, carolContact))
        val keyBefore = (mgr.groupDao.getGroupById("g1") as PhantmResult.Ok).value.groupKey.copyOf()

        mgr.removeMember("g1", carolId, aliceId, aliceEd25519.privateKey, listOf(bobContact))

        val keyAfter = (mgr.groupDao.getGroupById("g1") as PhantmResult.Ok).value.groupKey
        assertFalse(keyBefore.contentEquals(keyAfter), "Key must rotate on member removal")
    }

    @Test
    fun removeMember_excludesRemovedMemberFromEnvelopes() = runTest {
        val mgr = makeGroupManager()
        mgr.createGroup("g1", "Group", aliceId, aliceEd25519.privateKey, listOf(bobContact, carolContact))
        val result = mgr.removeMember("g1", carolId, aliceId, aliceEd25519.privateKey, listOf(bobContact))
        assertIs<PhantmResult.Ok<List<GroupKeyEnvelope>>>(result)
        assertFalse(result.value.any { it.recipientId == carolId })
        assertTrue(result.value.any { it.recipientId == bobId })
    }

    @Test
    fun removedMemberCannotDecryptNewMessages() = runTest {
        val aliceMgr = makeGroupManager()
        val carolMgr = makeGroupManager()
        val groupId = "g-rm"

        val createResult = aliceMgr.createGroup(groupId, "Group", aliceId, aliceEd25519.privateKey, listOf(carolContact))
        assertIs<PhantmResult.Ok<List<GroupKeyEnvelope>>>(createResult)

        // Carol receives initial key
        carolMgr.receiveGroupKeyEnvelope(
            createResult.value.first { it.recipientId == carolId }.envelopeBytes,
            carolId, carolX25519.privateKey, carolMlKem.privateKey, aliceEd25519.publicKey,
        )

        // Alice removes Carol — key rotates in Alice's DB but Carol's DB keeps the old key
        aliceMgr.removeMember(groupId, carolId, aliceId, aliceEd25519.privateKey, emptyList())

        // Alice encrypts with the NEW rotated key
        val ciphertext = (aliceMgr.encryptGroupMessage(groupId, MessagePayload(text = "Secret after Carol left")) as PhantmResult.Ok).value

        // Carol still has the OLD key — decryption must fail
        val carolResult = carolMgr.decryptGroupMessage(groupId, ciphertext)
        assertIs<PhantmResult.Err<GroupError>>(carolResult)
        assertIs<GroupError.DecryptionFailed>(carolResult.error)
    }

    // ── AC-M08-5: Group state persisted in M03 ────────────────────────────────

    @Test
    fun groupState_persistedAcrossManagerInstances() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PhantmDatabase.Schema.create(driver)
        val db = PhantmDatabase(driver)
        val dao = SqlDelightGroupDao(db)
        val protocol = createMessageProtocol(crypto, NonceTracker())

        GroupManagerImpl(crypto, protocol, dao)
            .createGroup("g-persist", "Persisted Group", aliceId, aliceEd25519.privateKey, listOf(bobContact))

        // New manager instance on the same DAO (same DB)
        val mgr2 = GroupManagerImpl(crypto, protocol, dao)
        val group = mgr2.groupDao.getGroupById("g-persist")
        assertIs<PhantmResult.Ok<LocalGroup>>(group)
        assertEquals("Persisted Group", group.value.name)

        val members = mgr2.groupDao.getMemberIds("g-persist")
        assertIs<PhantmResult.Ok<List<String>>>(members)
        assertTrue(aliceId in members.value && bobId in members.value)
    }

    @Test
    fun encryptGroupMessage_producesNonDeterministicCiphertext() = runTest {
        val mgr = makeGroupManager()
        mgr.createGroup("g1", "Group", aliceId, aliceEd25519.privateKey, emptyList())
        val payload = MessagePayload(text = "same text")
        val c1 = (mgr.encryptGroupMessage("g1", payload) as PhantmResult.Ok).value
        val c2 = (mgr.encryptGroupMessage("g1", payload) as PhantmResult.Ok).value
        assertFalse(c1.contentEquals(c2), "secretBox must produce distinct ciphertexts (random nonce)")
    }
}

package com.stagic.phantm.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.createCryptoCore
import com.stagic.phantm.db.LocalContact
import com.stagic.phantm.db.PhantmDatabase
import com.stagic.phantm.db.SqlDelightGroupDao
import com.stagic.phantm.groups.createGroupManager
import com.stagic.phantm.identity.PlatformContext
import com.stagic.phantm.identity.createIdentityManager
import com.stagic.phantm.protocol.MessagePayload
import com.stagic.phantm.protocol.MessageType
import com.stagic.phantm.protocol.createMessageProtocol
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * AC-M14-2: Group message integration test with 3 participants.
 *
 * Exercises M08 (Groups) wired with M07 (MessageProtocol) and M02 (Identity):
 *   Alice creates group with Bob and Carol → distributes key envelopes →
 *   Bob and Carol join → Alice encrypts group message → Bob and Carol both decrypt.
 */
class GroupMessageIntegrationTest {

    private fun makeGroupDao(): SqlDelightGroupDao {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PhantmDatabase.Schema.create(driver)
        return SqlDelightGroupDao(PhantmDatabase(driver))
    }

    @Test
    fun groupMessage_threeParticipants_allDecrypt() = runTest {
        // Isolated crypto core per participant (simulates separate devices)
        val aliceCrypto = createCryptoCore()
        val bobCrypto = createCryptoCore()
        val carolCrypto = createCryptoCore()

        // Generate long-term keypairs for each participant
        val aliceX25519 = aliceCrypto.generateX25519KeyPair()
        val aliceEd25519 = aliceCrypto.generateEd25519KeyPair()
        val aliceMlKem = aliceCrypto.generateMlKem768KeyPair()

        val bobX25519 = bobCrypto.generateX25519KeyPair()
        val bobEd25519 = bobCrypto.generateEd25519KeyPair()
        val bobMlKem = bobCrypto.generateMlKem768KeyPair()

        val carolX25519 = carolCrypto.generateX25519KeyPair()
        val carolEd25519 = carolCrypto.generateEd25519KeyPair()
        val carolMlKem = carolCrypto.generateMlKem768KeyPair()

        // Build LocalContact records (the public-key view each participant has of others)
        val ts = 1_700_000_000_000L
        val bobContact = LocalContact(
            id = "bob",
            displayName = "Bob",
            x25519PublicKey = bobX25519.publicKey,
            mlKemPublicKey = bobMlKem.publicKey,
            ed25519PublicKey = bobEd25519.publicKey,
            fingerprint = "bob-fp",
            verified = true,
            createdAtMs = ts,
        )
        val carolContact = LocalContact(
            id = "carol",
            displayName = "Carol",
            x25519PublicKey = carolX25519.publicKey,
            mlKemPublicKey = carolMlKem.publicKey,
            ed25519PublicKey = carolEd25519.publicKey,
            fingerprint = "carol-fp",
            verified = true,
            createdAtMs = ts,
        )

        // Create group managers (each backed by its own in-memory DB)
        val aliceProtocol = createMessageProtocol(aliceCrypto)
        val bobProtocol = createMessageProtocol(bobCrypto)
        val carolProtocol = createMessageProtocol(carolCrypto)

        val aliceMgr = createGroupManager(aliceCrypto, aliceProtocol, makeGroupDao())
        val bobMgr = createGroupManager(bobCrypto, bobProtocol, makeGroupDao())
        val carolMgr = createGroupManager(carolCrypto, carolProtocol, makeGroupDao())

        // Alice creates the group — caller provides the groupId
        val groupId = "integration-test-group-01"
        val createResult = aliceMgr.createGroup(
            groupId = groupId,
            name = "Phantm Test Group",
            creatorId = "alice",
            creatorEd25519PrivKey = aliceEd25519.privateKey,
            members = listOf(bobContact, carolContact),
        )
        assertIs<PhantmResult.Ok<*>>(createResult)
        val envelopes = (createResult as PhantmResult.Ok).value
        assertEquals(2, envelopes.size, "Must produce one envelope per non-creator member")

        // Deliver envelopes to Bob and Carol (in a real app these arrive via transport/relay)
        val bobEnvelope = envelopes.first { it.recipientId == "bob" }
        val carolEnvelope = envelopes.first { it.recipientId == "carol" }

        val bobJoin = bobMgr.receiveGroupKeyEnvelope(
            envelopeBytes = bobEnvelope.envelopeBytes,
            recipientId = "bob",
            recipientX25519PrivKey = bobX25519.privateKey,
            recipientMlKemPrivKey = bobMlKem.privateKey,
            senderEd25519Pub = aliceEd25519.publicKey,
        )
        assertIs<PhantmResult.Ok<Unit>>(bobJoin)

        val carolJoin = carolMgr.receiveGroupKeyEnvelope(
            envelopeBytes = carolEnvelope.envelopeBytes,
            recipientId = "carol",
            recipientX25519PrivKey = carolX25519.privateKey,
            recipientMlKemPrivKey = carolMlKem.privateKey,
            senderEd25519Pub = aliceEd25519.publicKey,
        )
        assertIs<PhantmResult.Ok<Unit>>(carolJoin)

        // Alice encrypts a group message
        val plaintext = "Secret group message — E2E encrypted for all members!"
        val payload = MessagePayload(type = MessageType.TEXT, text = plaintext, timestampMs = 1L)
        val cipherResult = aliceMgr.encryptGroupMessage(groupId, payload)
        assertIs<PhantmResult.Ok<ByteArray>>(cipherResult)
        val cipherBytes = cipherResult.value

        // Bob decrypts — must recover original plaintext
        val bobDecrypted = bobMgr.decryptGroupMessage(groupId, cipherBytes)
        assertIs<PhantmResult.Ok<MessagePayload>>(bobDecrypted)
        assertEquals(plaintext, bobDecrypted.value.text, "Bob must decrypt the group message")

        // Carol decrypts — must recover original plaintext
        val carolDecrypted = carolMgr.decryptGroupMessage(groupId, cipherBytes)
        assertIs<PhantmResult.Ok<MessagePayload>>(carolDecrypted)
        assertEquals(plaintext, carolDecrypted.value.text, "Carol must decrypt the group message")
    }

    @Test
    fun groupMessage_memberRemoved_cannotDecryptNewMessages() = runTest {
        val crypto = createCryptoCore()
        val aliceEd25519 = crypto.generateEd25519KeyPair()
        val carolX25519 = crypto.generateX25519KeyPair()
        val carolEd25519 = crypto.generateEd25519KeyPair()
        val carolMlKem = crypto.generateMlKem768KeyPair()

        val carolContact = LocalContact(
            id = "carol",
            displayName = "Carol",
            x25519PublicKey = carolX25519.publicKey,
            mlKemPublicKey = carolMlKem.publicKey,
            ed25519PublicKey = carolEd25519.publicKey,
            fingerprint = "carol-fp",
            verified = true,
            createdAtMs = 1L,
        )

        val aliceProtocol = createMessageProtocol(crypto)
        val carolProtocol = createMessageProtocol(createCryptoCore())
        val aliceMgr = createGroupManager(crypto, aliceProtocol, makeGroupDao())
        val carolMgr = createGroupManager(createCryptoCore(), carolProtocol, makeGroupDao())

        val groupId = "integration-removal-group"
        val envelopes = (aliceMgr.createGroup(
            groupId, "Removal Group", "alice", aliceEd25519.privateKey, listOf(carolContact)
        ) as PhantmResult.Ok).value

        carolMgr.receiveGroupKeyEnvelope(
            envelopeBytes = envelopes.first { it.recipientId == "carol" }.envelopeBytes,
            recipientId = "carol",
            recipientX25519PrivKey = carolX25519.privateKey,
            recipientMlKemPrivKey = carolMlKem.privateKey,
            senderEd25519Pub = aliceEd25519.publicKey,
        )

        // Remove Carol — key rotation
        aliceMgr.removeMember(groupId, "carol", "alice", aliceEd25519.privateKey, emptyList())

        // Alice encrypts a new message with the rotated key
        val newCipher = (aliceMgr.encryptGroupMessage(
            groupId,
            MessagePayload(type = MessageType.TEXT, text = "Post-rotation secret"),
        ) as PhantmResult.Ok).value

        // Carol (with old key) must fail to decrypt
        val carolResult = carolMgr.decryptGroupMessage(groupId, newCipher)
        assertIs<PhantmResult.Err<*>>(carolResult, "Removed member must not decrypt post-rotation messages")
    }
}

package com.stagic.phantm.db

import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.createCryptoCore
import com.stagic.phantm.identity.PlatformContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Covers AC-M03-3: MessageDao insert + getById round-trip and all CRUD ops. */
class MessageDaoTest {

    private val crypto = createCryptoCore()
    private val dao: MessageDao get() {
        val driver = createSqlDriver(PlatformContext(), crypto)
        return PhantmDaoFactory(driver).messages
    }

    // ── AC-M03-3: insert + getById round-trip ────────────────────────────────

    @Test
    fun insertAndGetByIdRoundTrip() = runTest {
        val d = dao
        val msg = sampleMessage("msg-001")
        val insertResult = d.insert(msg)
        assertTrue(insertResult.isOk(), "insert must succeed")

        val getResult = d.getById("msg-001")
        assertTrue(getResult.isOk(), "getById must succeed")
        val loaded = getResult.valueOrNull!!
        assertEquals(msg.id, loaded.id)
        assertEquals(msg.conversationId, loaded.conversationId)
        assertEquals(msg.senderId, loaded.senderId)
        assertContentEquals(msg.encryptedPayload, loaded.encryptedPayload)
        assertEquals(msg.timestampMs, loaded.timestampMs)
        assertEquals(msg.deliveryStatus, loaded.deliveryStatus)
        assertEquals(msg.localOnly, loaded.localOnly)
    }

    @Test
    fun getByIdReturnsNotFoundForMissingId() = runTest {
        val result = dao.getById("nonexistent")
        assertTrue(result.isErr())
        assertEquals(DbError.NotFound, result.errorOrNull)
    }

    // ── updateDeliveryStatus ──────────────────────────────────────────────────

    @Test
    fun updateDeliveryStatusChangesStatus() = runTest {
        val d = dao
        d.insert(sampleMessage("msg-002", status = DeliveryStatus.PENDING))

        val updateResult = d.updateDeliveryStatus("msg-002", DeliveryStatus.DELIVERED)
        assertTrue(updateResult.isOk())

        val loaded = d.getById("msg-002").valueOrNull!!
        assertEquals(DeliveryStatus.DELIVERED, loaded.deliveryStatus)
    }

    @Test
    fun allDeliveryStatusValuesRoundTrip() = runTest {
        val d = dao
        DeliveryStatus.entries.forEachIndexed { i, status ->
            val id = "msg-status-$i"
            d.insert(sampleMessage(id, status = status))
            val loaded = d.getById(id).valueOrNull!!
            assertEquals(status, loaded.deliveryStatus, "Status $status must survive round-trip")
        }
    }

    // ── getByConversation ─────────────────────────────────────────────────────

    @Test
    fun getByConversationReturnsMessagesInAscTimestampOrder() = runTest {
        val d = dao
        val convId = "conv-alpha"
        d.insert(sampleMessage("m3", convId, timestampMs = 3000L))
        d.insert(sampleMessage("m1", convId, timestampMs = 1000L))
        d.insert(sampleMessage("m2", convId, timestampMs = 2000L))
        // Insert decoy for another conversation
        d.insert(sampleMessage("other", "conv-beta", timestampMs = 500L))

        val messages = d.getByConversation(convId).first()
        assertEquals(3, messages.size)
        assertEquals(listOf("m1", "m2", "m3"), messages.map { it.id })
    }

    @Test
    fun getByConversationEmitsUpdateOnInsert() = runTest {
        val d = dao
        val convId = "conv-reactive"
        val flow = d.getByConversation(convId)

        val initial = flow.first()
        assertTrue(initial.isEmpty())

        d.insert(sampleMessage("reactive-msg", convId))
        val updated = flow.first()
        assertEquals(1, updated.size)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    fun deleteRemovesMessage() = runTest {
        val d = dao
        d.insert(sampleMessage("del-msg"))
        assertTrue(d.getById("del-msg").isOk())

        val result = d.delete("del-msg")
        assertTrue(result.isOk())
        assertTrue(d.getById("del-msg").isErr())
    }

    @Test
    fun deleteAllForConversationRemovesOnlyThatConversation() = runTest {
        val d = dao
        d.insert(sampleMessage("c1-m1", "conv-1"))
        d.insert(sampleMessage("c1-m2", "conv-1"))
        d.insert(sampleMessage("c2-m1", "conv-2"))

        d.deleteAllForConversation("conv-1")

        assertTrue(d.getByConversation("conv-1").first().isEmpty())
        assertEquals(1, d.getByConversation("conv-2").first().size)
    }

    // ── encryptedPayload is stored/loaded as-is ───────────────────────────────

    @Test
    fun encryptedPayloadStoredVerbatim() = runTest {
        val d = dao
        val payload = crypto.randomBytes(512)
        val msg = sampleMessage("payload-test", encryptedPayload = payload)
        d.insert(msg)
        val loaded = d.getById("payload-test").valueOrNull!!
        assertContentEquals(payload, loaded.encryptedPayload)
    }

    // ── localOnly flag ────────────────────────────────────────────────────────

    @Test
    fun localOnlyFlagRoundTrips() = runTest {
        val d = dao
        d.insert(sampleMessage("local-true", localOnly = true))
        d.insert(sampleMessage("local-false", localOnly = false))
        assertTrue(d.getById("local-true").valueOrNull!!.localOnly)
        assertFalse(d.getById("local-false").valueOrNull!!.localOnly)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun sampleMessage(
        id: String,
        conversationId: String = "conv-default",
        senderId: String = "sender-alice",
        encryptedPayload: ByteArray = "ciphertext".encodeToByteArray(),
        timestampMs: Long = 1_000_000L,
        status: DeliveryStatus = DeliveryStatus.PENDING,
        localOnly: Boolean = false,
    ) = LocalMessage(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        encryptedPayload = encryptedPayload,
        timestampMs = timestampMs,
        deliveryStatus = status,
        localOnly = localOnly,
    )
}

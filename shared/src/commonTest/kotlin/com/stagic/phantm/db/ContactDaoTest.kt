package com.stagic.phantm.db

import com.stagic.phantm.crypto.PublicKey
import com.stagic.phantm.crypto.createCryptoCore
import com.stagic.phantm.identity.PlatformContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Covers AC-M03-4: ContactDao full CRUD suite. */
class ContactDaoTest {

    private val crypto = createCryptoCore()
    private val dao: ContactDao get() {
        val driver = createSqlDriver(PlatformContext(), crypto)
        return PhantmDaoFactory(driver).contacts
    }

    // ── insert + getById ──────────────────────────────────────────────────────

    @Test
    fun insertAndGetByIdRoundTrip() = runTest {
        val d = dao
        val contact = sampleContact("alice")
        assertTrue(d.insert(contact).isOk())

        val loaded = d.getById("alice").valueOrNull!!
        assertEquals(contact.id, loaded.id)
        assertEquals(contact.displayName, loaded.displayName)
        assertContentEquals(contact.x25519PublicKey.bytes, loaded.x25519PublicKey.bytes)
        assertContentEquals(contact.mlKemPublicKey.bytes, loaded.mlKemPublicKey.bytes)
        assertContentEquals(contact.ed25519PublicKey.bytes, loaded.ed25519PublicKey.bytes)
        assertEquals(contact.fingerprint, loaded.fingerprint)
        assertEquals(contact.verified, loaded.verified)
        assertEquals(contact.createdAtMs, loaded.createdAtMs)
    }

    @Test
    fun getByIdReturnsNotFoundForMissingId() = runTest {
        val result = dao.getById("nobody")
        assertTrue(result.isErr())
        assertEquals(DbError.NotFound, result.errorOrNull)
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    fun getAllReturnsContactsAlphabetically() = runTest {
        val d = dao
        d.insert(sampleContact("z-zara", name = "Zara"))
        d.insert(sampleContact("a-alice", name = "Alice"))
        d.insert(sampleContact("m-mallory", name = "Mallory"))

        val contacts = d.getAll().first()
        assertEquals(listOf("Alice", "Mallory", "Zara"), contacts.map { it.displayName })
    }

    @Test
    fun getAllEmitsEmptyListInitially() = runTest {
        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun getAllEmitsUpdatesOnInsert() = runTest {
        val d = dao
        val flow = d.getAll()
        assertEquals(0, flow.first().size)

        d.insert(sampleContact("bob"))
        assertEquals(1, flow.first().size)
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    fun updateChangesDisplayName() = runTest {
        val d = dao
        d.insert(sampleContact("charlie", name = "Charlie"))
        val updated = sampleContact("charlie", name = "Charles")
        assertTrue(d.update(updated).isOk())
        assertEquals("Charles", d.getById("charlie").valueOrNull!!.displayName)
    }

    @Test
    fun updateChangesVerifiedFlag() = runTest {
        val d = dao
        d.insert(sampleContact("dave", verified = false))
        d.update(sampleContact("dave", verified = true))
        assertTrue(d.getById("dave").valueOrNull!!.verified)
    }

    @Test
    fun verifiedFlagRoundTrips() = runTest {
        val d = dao
        d.insert(sampleContact("verified-true", verified = true))
        d.insert(sampleContact("verified-false", verified = false))
        assertTrue(d.getById("verified-true").valueOrNull!!.verified)
        assertFalse(d.getById("verified-false").valueOrNull!!.verified)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    fun deleteRemovesContact() = runTest {
        val d = dao
        d.insert(sampleContact("eve"))
        assertTrue(d.getById("eve").isOk())

        assertTrue(d.delete("eve").isOk())
        assertTrue(d.getById("eve").isErr())
    }

    @Test
    fun deleteReducesGetAllCount() = runTest {
        val d = dao
        d.insert(sampleContact("frank"))
        d.insert(sampleContact("grace"))
        d.delete("frank")
        assertEquals(1, d.getAll().first().size)
    }

    // ── public key bytes are stored verbatim ──────────────────────────────────

    @Test
    fun publicKeyBytesStoredVerbatim() = runTest {
        val d = dao
        val x25519Pub = PublicKey(crypto.randomBytes(32))
        val mlKemPub = PublicKey(crypto.randomBytes(1184))
        val ed25519Pub = PublicKey(crypto.randomBytes(32))
        val contact = sampleContact("keyholder").copy(
            x25519PublicKey = x25519Pub,
            mlKemPublicKey = mlKemPub,
            ed25519PublicKey = ed25519Pub,
        )
        d.insert(contact)
        val loaded = d.getById("keyholder").valueOrNull!!
        assertContentEquals(x25519Pub.bytes, loaded.x25519PublicKey.bytes)
        assertContentEquals(mlKemPub.bytes, loaded.mlKemPublicKey.bytes)
        assertContentEquals(ed25519Pub.bytes, loaded.ed25519PublicKey.bytes)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun sampleContact(
        id: String,
        name: String = "Contact $id",
        verified: Boolean = false,
    ) = LocalContact(
        id = id,
        displayName = name,
        x25519PublicKey = PublicKey(crypto.randomBytes(32)),
        mlKemPublicKey = PublicKey(crypto.randomBytes(1184)),
        ed25519PublicKey = PublicKey(crypto.randomBytes(32)),
        fingerprint = "fp-$id",
        verified = verified,
        createdAtMs = 1_000_000L,
    )
}

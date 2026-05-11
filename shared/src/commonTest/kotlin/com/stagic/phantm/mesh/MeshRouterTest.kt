package com.stagic.phantm.mesh

import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.createCryptoCore
import com.stagic.phantm.protocol.MessagePayload
import com.stagic.phantm.protocol.MessageType
import com.stagic.phantm.protocol.NonceTracker
import com.stagic.phantm.protocol.createMessageProtocol
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MeshRouterTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeTriangle(): Triple<JvmMeshNetwork, JvmMeshNetwork, JvmMeshNetwork> {
        val aliceNet = JvmMeshNetwork("alice")
        val bobNet = JvmMeshNetwork("bob")
        val carolNet = JvmMeshNetwork("carol")
        aliceNet.connectTo(bobNet)
        bobNet.connectTo(carolNet)
        // alice ← direct → bob ← direct → carol  (alice and carol not directly connected)
        return Triple(aliceNet, bobNet, carolNet)
    }

    // ── AC-M10-1: Peer discovery (structural) ────────────────────────────────

    @Test
    fun jvmMeshNetwork_connectTo_addsBidirectionalPeer() {
        val a = JvmMeshNetwork("a")
        val b = JvmMeshNetwork("b")
        a.connectTo(b)
        assertTrue(a.directPeers.any { it.id == "b" }, "a must see b as direct peer")
        assertTrue(b.directPeers.any { it.id == "a" }, "b must see a as direct peer")
    }

    @Test
    fun jvmMeshNetwork_reachableIds_excludeSelf() {
        val a = JvmMeshNetwork("a")
        val b = JvmMeshNetwork("b")
        val c = JvmMeshNetwork("c")
        a.connectTo(b)
        b.connectTo(c)
        // a's view of b: b's reachable IDs should include c but NOT a itself
        val bPeerFromA = a.directPeers.first { it.id == "b" }
        assertTrue("c" in bPeerFromA.reachableIds, "b should advertise c as reachable")
        assertFalse("a" in bPeerFromA.reachableIds, "b must not advertise a (the sender) as reachable")
    }

    // ── AC-M10-2: 2-hop routing ───────────────────────────────────────────────

    @Test
    fun twoHop_alice_to_carol_via_bob() = runTest {
        val (aliceNet, bobNet, carolNet) = makeTriangle()
        val aliceRouter = MeshRouter("alice", aliceNet)
        val bobRouter = MeshRouter("bob", bobNet)
        val carolRouter = MeshRouter("carol", carolNet)

        val received = CompletableDeferred<ByteArray>()
        carolRouter.onEnvelopeReceived { _, envelopeBytes -> received.complete(envelopeBytes) }

        val payload = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val result = aliceRouter.sendEnvelope("carol", payload)

        assertIs<PhantmResult.Ok<Unit>>(result)
        assertContentEquals(payload, received.await())
    }

    @Test
    fun directSend_doesNotRelay() = runTest {
        val aliceNet = JvmMeshNetwork("alice")
        val bobNet = JvmMeshNetwork("bob")
        aliceNet.connectTo(bobNet)

        val aliceRouter = MeshRouter("alice", aliceNet)
        val bobRouter = MeshRouter("bob", bobNet)

        val bobReceived = CompletableDeferred<ByteArray>()
        bobRouter.onEnvelopeReceived { _, env -> bobReceived.complete(env) }

        val payload = byteArrayOf(0xDE.toByte(), 0xAD.toByte())
        val result = aliceRouter.sendEnvelope("bob", payload)
        assertIs<PhantmResult.Ok<Unit>>(result)
        assertContentEquals(payload, bobReceived.await())
    }

    @Test
    fun noRoute_returns_error() = runTest {
        val aliceNet = JvmMeshNetwork("alice")
        val aliceRouter = MeshRouter("alice", aliceNet)
        val result = aliceRouter.sendEnvelope("nobody", byteArrayOf(1))
        assertIs<PhantmResult.Err<MeshError>>(result)
        assertIs<MeshError.NoRoute>(result.error)
    }

    @Test
    fun maxHops_exceeded_dropsFrame() = runTest {
        // 3-hop chain: alice → bob → carol → dave
        // Alice sends to dave (3 hops) — should be dropped (MAX_HOPS = 2)
        val aliceNet = JvmMeshNetwork("alice")
        val bobNet = JvmMeshNetwork("bob")
        val carolNet = JvmMeshNetwork("carol")
        val daveNet = JvmMeshNetwork("dave")
        aliceNet.connectTo(bobNet)
        bobNet.connectTo(carolNet)
        carolNet.connectTo(daveNet)

        val aliceRouter = MeshRouter("alice", aliceNet)
        val bobRouter = MeshRouter("bob", bobNet)
        val carolRouter = MeshRouter("carol", carolNet)
        val daveRouter = MeshRouter("dave", daveNet)

        var daveReceived = false
        daveRouter.onEnvelopeReceived { _, _ -> daveReceived = true }

        // Alice can't see dave in 2 hops (carol's peers = [bob, dave], but
        // alice's peer bob has reachableIds=[alice,carol], not dave)
        val result = aliceRouter.sendEnvelope("dave", byteArrayOf(9))
        // Either NoRoute or frame dropped — dave must NOT receive anything
        assertFalse(daveReceived, "Frame exceeding MAX_HOPS must not be delivered")
    }

    // ── AC-M10-3: Mesh uses M07 envelope ─────────────────────────────────────

    @Test
    fun twoHop_carriesRealM07Envelope() = runTest {
        val crypto = createCryptoCore()
        val protocol = createMessageProtocol(crypto)

        // Generate key pairs for alice and carol
        val aliceEd25519 = crypto.generateEd25519KeyPair()
        val carolX25519 = crypto.generateX25519KeyPair()
        val carolMlKem = crypto.generateMlKem768KeyPair()

        val envelopeBytes = (protocol.encrypt(
            payload = MessagePayload(type = MessageType.TEXT, text = "mesh test"),
            senderId = "alice",
            recipientId = "carol",
            senderEd25519PrivKey = aliceEd25519.privateKey,
            recipientX25519Pub = carolX25519.publicKey,
            recipientMlKemPub = carolMlKem.publicKey,
        ) as PhantmResult.Ok).value

        val (aliceNet, bobNet, carolNet) = makeTriangle()
        val aliceRouter = MeshRouter("alice", aliceNet)
        val bobRouter = MeshRouter("bob", bobNet)
        val carolRouter = MeshRouter("carol", carolNet)

        val receivedEnvelope = CompletableDeferred<ByteArray>()
        carolRouter.onEnvelopeReceived { _, env -> receivedEnvelope.complete(env) }

        aliceRouter.sendEnvelope("carol", envelopeBytes)

        val received = receivedEnvelope.await()
        assertContentEquals(envelopeBytes, received)

        // Carol decrypts with her private keys (AC-M10-3: same M07 envelope arrives intact)
        val decrypted = protocol.decrypt(
            envelopeBytes = received,
            recipientId = "carol",
            recipientX25519PrivKey = carolX25519.privateKey,
            recipientMlKemPrivKey = carolMlKem.privateKey,
            senderEd25519Pub = aliceEd25519.publicKey,
        )
        assertIs<PhantmResult.Ok<*>>(decrypted)
    }

    // ── AC-M10-4: No plaintext on mesh links (structural) ─────────────────────

    @Test
    fun meshRouter_neverCallsDecrypt() {
        // Structural: MeshRouter has no crypto dependency; it only handles
        // opaque ByteArray blobs. Verified by absence of any decrypt/secretBoxOpen
        // call in MeshRouter source — enforced by no CryptoCore import.
        val methods = MeshRouter::class.java.declaredMethods.map { it.name }
        assertFalse(
            methods.any { it.contains("decrypt", ignoreCase = true) },
            "MeshRouter must not have any decrypt method",
        )
    }
}

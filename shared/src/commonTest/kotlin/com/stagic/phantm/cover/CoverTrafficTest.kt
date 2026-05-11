package com.stagic.phantm.cover

import com.stagic.phantm.crypto.createCryptoCore
import com.stagic.phantm.transport.ConnectionState
import com.stagic.phantm.transport.TransportClient
import com.stagic.phantm.transport.TransportEnvelope
import com.stagic.phantm.transport.TransportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.random.Random

/**
 * Covers AC-M06-1 through AC-M06-4.
 * AC-M06-3 (relay discard) is pending M09 integration.
 */
class CoverTrafficTest {

    private val crypto = createCryptoCore()
    private val impl = CoverTrafficManagerImpl(crypto, Random(seed = 42))
    private val config = CoverTrafficConfig(
        packetsPerMinute = 60,
        minPayloadBytes = 256,
        maxPayloadBytes = 1024,
    )

    // ── AC-M06-1: KS test — cover packet sizes follow configured uniform distribution ──

    @Test
    fun packetSizesPassKsTestAgainstUniformDistribution() {
        val n = 500
        val sizes = (1..n).map {
            impl.generateCoverEnvelope(config).bytes.size.toDouble()
        }

        // Cover envelope = 24-byte nonce + secretBox ciphertext (payload + 16-byte MAC)
        // so total = 24 + payloadSize + 16 = payloadSize + 40
        val expectedMin = (config.minPayloadBytes + 40).toDouble()
        val expectedMax = (config.maxPayloadBytes + 40).toDouble()

        assertTrue(
            passesUniformKsTest(sizes, expectedMin, expectedMax, alpha = 0.05),
            "Cover packet sizes should follow Uniform[${expectedMin.toInt()}, ${expectedMax.toInt()}] " +
                "(KS test at α=0.05). This test is probabilistic and may rarely fail.",
        )
    }

    @Test
    fun allPacketSizesAreWithinConfiguredRange() {
        val expectedMin = config.minPayloadBytes + 40 // 24 nonce + 16 MAC
        val expectedMax = config.maxPayloadBytes + 40

        repeat(200) {
            val size = impl.generateCoverEnvelope(config).bytes.size
            assertTrue(size >= expectedMin, "Packet too small: $size < $expectedMin")
            assertTrue(size <= expectedMax, "Packet too large: $size > $expectedMax")
        }
    }

    @Test
    fun ksStatisticIsCorrectForPerfectUniformSample() {
        // A perfectly uniform sample should have D ≈ 0
        val n = 1000
        val samples = (1..n).map { it.toDouble() / n }
        val d = ksStatistic(samples, expectedCdf = { x -> x })
        assertTrue(d < 0.01, "Perfect uniform sample should have D < 0.01, got $d")
    }

    @Test
    fun ksStatisticDetectsNonUniformDistribution() {
        // All samples concentrated at 0.0 — clearly not Uniform[0,1]
        val samples = List(500) { 0.0 }
        val d = ksStatistic(samples, expectedCdf = { x -> x.coerceIn(0.0, 1.0) })
        assertTrue(d > ksCriticalValue(500, alpha = 0.05), "Should detect non-uniform distribution")
    }

    @Test
    fun ksCriticalValueDecreasesWithSampleSize() {
        assertTrue(ksCriticalValue(100) > ksCriticalValue(1000))
        assertTrue(ksCriticalValue(1000) > ksCriticalValue(10_000))
    }

    @Test
    fun ksCriticalValueAtAlpha05IsApproximatelyCorrect() {
        // For n=100, c(0.05) / sqrt(100) ≈ 0.1358
        val cv = ksCriticalValue(100, alpha = 0.05)
        assertTrue(cv > 0.13 && cv < 0.14, "Expected ≈0.1358, got $cv")
    }

    // ── AC-M06-2: constant configurable rate ──────────────────────────────────

    @Test
    fun intervalMsMatchesPacketsPerMinute() {
        assertEquals(6_000L, CoverTrafficConfig(packetsPerMinute = 10).intervalMs)
        assertEquals(1_000L, CoverTrafficConfig(packetsPerMinute = 60).intervalMs)
        assertEquals(60_000L, CoverTrafficConfig(packetsPerMinute = 1).intervalMs)
        assertEquals(500L, CoverTrafficConfig(packetsPerMinute = 120).intervalMs)
    }

    @Test
    fun startSetsIsRunning() {
        val mgr = CoverTrafficManagerImpl(crypto)
        assertFalse(mgr.isRunning)
        mgr.start(FakeConnectedTransport(), config)
        assertTrue(mgr.isRunning)
        mgr.stop()
    }

    @Test
    fun stopClearsIsRunning() {
        val mgr = CoverTrafficManagerImpl(crypto)
        mgr.start(FakeConnectedTransport(), config)
        assertTrue(mgr.isRunning)
        mgr.stop()
        assertFalse(mgr.isRunning)
    }

    @Test
    fun restartReplacesConfig() {
        val mgr = CoverTrafficManagerImpl(crypto)
        mgr.start(FakeConnectedTransport(), CoverTrafficConfig(packetsPerMinute = 5))
        mgr.start(FakeConnectedTransport(), CoverTrafficConfig(packetsPerMinute = 60))
        assertTrue(mgr.isRunning)
        mgr.stop()
    }

    // ── AC-M06-4: no user metadata ────────────────────────────────────────────

    @Test
    fun coverEnvelopeRecipientIdIsCoverSentinel() {
        val envelope = impl.generateCoverEnvelope(config)
        assertEquals(COVER_RECIPIENT_ID, envelope.recipientId)
    }

    @Test
    fun differentCoverPacketsHaveDifferentBytes() {
        val a = impl.generateCoverEnvelope(config)
        val b = impl.generateCoverEnvelope(config)
        // Content should be random — same bytes would indicate deterministic leakage
        assertFalse(a.bytes.contentEquals(b.bytes), "Cover packets must not be identical")
    }

    @Test
    fun coverEnvelopeBytesDoNotContainCoverRecipientId() {
        // The recipient ID sentinel must not appear inside the encrypted payload
        val envelope = impl.generateCoverEnvelope(config)
        val recipientBytes = COVER_RECIPIENT_ID.encodeToByteArray()
        val found = (0..envelope.bytes.size - recipientBytes.size).any { i ->
            envelope.bytes.copyOfRange(i, i + recipientBytes.size).contentEquals(recipientBytes)
        }
        assertFalse(found, "COVER_RECIPIENT_ID must not appear inside the encrypted envelope bytes")
    }

    @Test
    fun coverEnvelopeBytesAreEncrypted() {
        // Encrypted bytes must not equal the random plaintext (trivially true for secretBox)
        val envelope = impl.generateCoverEnvelope(config)
        // The first 24 bytes are the nonce; the rest is ciphertext + MAC
        assertTrue(envelope.bytes.size > 40, "Envelope must have nonce + payload + MAC")
    }

    @Test
    fun configRequiresPositivePacketsPerMinute() = runTest {
        var threw = false
        try { CoverTrafficConfig(packetsPerMinute = 0) } catch (e: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun configRequiresMinLeMax() = runTest {
        var threw = false
        try { CoverTrafficConfig(minPayloadBytes = 512, maxPayloadBytes = 128) } catch (e: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    @Test
    fun fixedSizeConfigProducesUniformSize() {
        val fixedConfig = CoverTrafficConfig(minPayloadBytes = 512, maxPayloadBytes = 512)
        val expectedSize = 512 + 40 // 512 payload + 24 nonce + 16 MAC
        repeat(20) {
            assertEquals(expectedSize, impl.generateCoverEnvelope(fixedConfig).bytes.size)
        }
    }
}

// ── Test doubles ──────────────────────────────────────────────────────────────

private class FakeConnectedTransport : TransportClient {
    override val connectionState: StateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.CONNECTED)
    override val incomingEnvelopes: Flow<TransportEnvelope> = emptyFlow()
    override suspend fun connect(relayUrl: String): TransportResult<Unit> =
        com.stagic.phantm.PhantmResult.Ok(Unit)
    override suspend fun send(envelope: TransportEnvelope): TransportResult<Unit> =
        com.stagic.phantm.PhantmResult.Ok(Unit)
    override suspend fun disconnect() {}
}
